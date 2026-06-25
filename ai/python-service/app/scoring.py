"""
Moteur de scoring hybride : règles pondérées + modèle ML (Logistic Regression).
- Si un modèle entraîné existe (data/model.pkl), il est utilisé pour la probabilité d'acceptation.
- Sinon, le scoring tombe en mode règles pondérées (logique identique côté Java).
- Les poids et formules sont alignés sur le client Java (secours local).
"""
from __future__ import annotations

import os
import pickle
import logging
from decimal import Decimal, ROUND_HALF_UP
from pathlib import Path
from typing import List, Optional, Dict, Any

import numpy as np

from .schemas import EventIn, ProviderIn, ScoreItem, ScoreRequest

logger = logging.getLogger(__name__)

# ── Pondérations scoring règles (fallback) ──────────────────────────────────
W_TYPE     = 0.30
W_CAPACITY = 0.25
W_BUDGET   = 0.25
W_HISTORY  = 0.20

# ── Chemin du modèle ML persisté ─────────────────────────────────────────────
MODEL_PATH = Path(__file__).parent.parent / "data" / "model.pkl"
_ml_model: Optional[Any] = None
_model_metadata: Dict[str, Any] = {}


def _try_load_model() -> None:
    """Charge le modèle scikit-learn depuis le disque si disponible."""
    global _ml_model, _model_metadata
    if MODEL_PATH.exists():
        try:
            with open(MODEL_PATH, "rb") as f:
                payload = pickle.load(f)
            _ml_model = payload.get("pipeline")
            _model_metadata = {
                "trained_at":    payload.get("trained_at", "inconnue"),
                "n_samples":     payload.get("n_samples", 0),
                "accuracy":      payload.get("accuracy"),
                "roc_auc":       payload.get("roc_auc"),
                "feature_names": payload.get("feature_names", []),
                "model_type":    payload.get("model_type", "LogisticRegression"),
            }
            logger.info("Modèle ML chargé : %s", _model_metadata)
        except Exception as exc:
            logger.warning("Impossible de charger le modèle ML : %s", exc)
            _ml_model = None
            _model_metadata = {}


_try_load_model()


# ── Info publique ─────────────────────────────────────────────────────────────
def get_model_info() -> Dict[str, Any]:
    return {
        "ml_model_available": _ml_model is not None,
        "model_path":         str(MODEL_PATH),
        "scoring_mode":       "hybrid-ml" if _ml_model else "rule-based",
        **_model_metadata,
    }


def reload_model() -> Dict[str, Any]:
    """Force le rechargement depuis le disque (appelé après /model/train)."""
    _try_load_model()
    return get_model_info()


# ── Construction du vecteur de features ──────────────────────────────────────
def _build_feature_vector(event: EventIn, p: ProviderIn) -> List[float]:
    """
    7 features numériques alignées sur l'export d'entraînement :
    [type_match, capacity_ratio, in_range, budget_ratio,
     hist_rate, log_decided, budget_comfortable]
    """
    et = (event.eventType or "").strip().casefold()
    type_match = float(
        any(t and t.strip().casefold() == et for t in (p.acceptedEventTypes or []))
    )

    participants = max(event.participantCount or 0, 0)
    min_c = max(p.minCapacity or 0, 0)
    max_c = max(p.maxCapacity or 0, 1)
    capacity_ratio   = participants / max_c
    in_range         = float(min_c <= participants <= max_c)

    budget    = max(event.budget or 0, 0)
    min_price = max(p.minimumPrice or 0, 1e-9)
    budget_ratio        = budget / min_price
    budget_comfortable  = float(budget_ratio >= 1.15)

    acc = p.acceptedReservationCount or 0
    ref = p.refusedReservationCount  or 0
    decided  = acc + ref
    hist_rate    = acc / decided if decided > 0 else 0.65
    log_decided  = float(np.log1p(decided))

    return [type_match, capacity_ratio, in_range,
            budget_ratio, hist_rate, log_decided, budget_comfortable]


FEATURE_NAMES = [
    "type_match", "capacity_ratio", "in_range",
    "budget_ratio", "hist_rate", "log_decided", "budget_comfortable",
]


# ── Scoring règles (fallback Java-compatible) ─────────────────────────────────
def _round2(v: float) -> float:
    return float(Decimal(str(v)).quantize(Decimal("0.01"), rounding=ROUND_HALF_UP))


def _score_event_type(event_type: str, accepted: List[str]) -> float:
    et = (event_type or "").strip()
    if not et:
        return 50.0
    for t in (accepted or []):
        if t and t.strip().casefold() == et.casefold():
            return 100.0
    return 0.0


def _score_capacity(participants: int, min_c: int, max_c: int) -> float:
    if min_c <= participants <= max_c:
        return 100.0
    if participants < min_c:
        return 100.0 * participants / max(min_c, 1)
    if max_c > 0 and participants <= max_c * 1.5:
        return 100.0 * max_c / participants
    return 35.0


def _score_budget(budget: float, minimum_price: float) -> float:
    if not minimum_price or minimum_price <= 0:
        return 100.0
    if not budget or budget <= 0:
        return 0.0
    r = budget / minimum_price
    if r >= 1.15:
        return 100.0
    if r >= 1.0:
        return 88.0
    return min(100.0, r * 85.0)


def _historical_rate(acc: int, ref: int) -> float:
    decided = acc + ref
    return acc / decided if decided > 0 else 0.65


def _blend_probability(norm_compat: float, hist_rate: float) -> float:
    p = 0.62 * norm_compat + 0.38 * hist_rate
    return min(0.97, max(0.05, p))


def _build_explanation(
    type_score: float, capacity_score: float, budget_score: float,
    history_score: float, historical_rate: float, ml_used: bool
) -> str:
    parts: List[str] = []
    if type_score >= 95:
        parts.append("Le profil couvre le type d'événement.")
    elif type_score <= 5:
        parts.append("Le type d'événement ne figure pas parmi les types acceptés.")
    if capacity_score >= 95:
        parts.append("La capacité annoncée correspond au nombre de participants.")
    elif capacity_score < 70:
        parts.append("Écart notable entre le nombre de participants et la fourchette de capacité.")
    if budget_score >= 95:
        parts.append("Le budget est confortable par rapport au minimum tarifaire.")
    elif budget_score < 70:
        parts.append("Le budget est serré par rapport au prix minimum demandé.")
    if history_score >= 75:
        parts.append("Historique favorable : taux d'acceptation élevé.")
    elif historical_rate < 0.45:
        parts.append("Historique : part importante de refus sur les demandes passées.")
    if not parts:
        parts.append("Score obtenu par combinaison pondérée des critères type, capacité, budget et historique.")
    if ml_used:
        parts.append("(probabilité affinée par modèle ML entraîné)")
    return " ".join(parts)


# ── Scoring d'un seul prestataire ────────────────────────────────────────────
def score_one(event: EventIn, p: ProviderIn) -> ScoreItem:
    type_score     = _score_event_type(event.eventType or "", p.acceptedEventTypes or [])
    capacity_score = _score_capacity(event.participantCount, p.minCapacity, p.maxCapacity)
    budget_score   = _score_budget(event.budget, p.minimumPrice)
    hist_rate      = _historical_rate(p.acceptedReservationCount or 0, p.refusedReservationCount or 0)
    history_score  = 100.0 * hist_rate

    weighted = (
        W_TYPE     * type_score
        + W_CAPACITY * capacity_score
        + W_BUDGET   * budget_score
        + W_HISTORY  * history_score
    )
    compatibility = int(round(min(100, max(0, weighted))))

    # Probabilité : ML si disponible, sinon règles
    ml_used = False
    if _ml_model is not None:
        try:
            features   = _build_feature_vector(event, p)
            X          = np.array(features).reshape(1, -1)
            prob_ml    = float(_ml_model.predict_proba(X)[0, 1])
            # Blend : 50 % ML + 50 % règles pour robustesse
            prob_rules = _blend_probability(compatibility / 100.0, hist_rate)
            acceptance = min(0.97, max(0.05, 0.50 * prob_ml + 0.50 * prob_rules))
            ml_used    = True
        except Exception as exc:
            logger.warning("ML predict failed, fallback rules: %s", exc)
            acceptance = _blend_probability(compatibility / 100.0, hist_rate)
    else:
        acceptance = _blend_probability(compatibility / 100.0, hist_rate)

    explanation = _build_explanation(
        type_score, capacity_score, budget_score, history_score, hist_rate, ml_used
    )

    return ScoreItem(
        providerUserId      = p.providerUserId,
        businessName        = p.businessName,
        compatibilityScore  = compatibility,
        acceptanceProbability = _round2(acceptance),
        explanation         = explanation,
    )


# ── Classement de tous les prestataires ──────────────────────────────────────
def rank_providers(req: ScoreRequest) -> List[ScoreItem]:
    out = [score_one(req.event, p) for p in req.providers]
    out.sort(key=lambda x: (x.compatibilityScore, x.acceptanceProbability), reverse=True)
    return out