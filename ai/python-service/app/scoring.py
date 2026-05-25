"""
Moteur de scoring hybride (règles + historique).
Pondérations et formules alignées sur le client Java (secours local).
Peut être remplacé par un modèle ML (scikit-learn, etc.) en conservant l'API.
"""

from __future__ import annotations

from decimal import Decimal, ROUND_HALF_UP
from typing import List

from .schemas import EventIn, ProviderIn, ScoreItem, ScoreRequest

W_TYPE = 0.30
W_CAPACITY = 0.25
W_BUDGET = 0.25
W_HISTORY = 0.20


def _round2(v: float) -> float:
    return float(Decimal(str(v)).quantize(Decimal("0.01"), rounding=ROUND_HALF_UP))


def _score_event_type(event_type: str, accepted: List[str]) -> float:
    et = (event_type or "").strip()
    if not et:
        return 50.0
    for t in accepted:
        if t and t.strip().casefold() == et.casefold():
            return 100.0
    return 0.0


def _score_capacity(participants: int, min_c: int, max_c: int) -> float:
    if participants >= min_c and participants <= max_c:
        return 100.0
    if participants < min_c:
        if min_c <= 0:
            return 100.0
        return 100.0 * participants / min_c
    if max_c > 0 and participants <= max_c * 1.5:
        return 100.0 * max_c / participants
    return 35.0


def _score_budget(budget: float, minimum_price: float) -> float:
    if minimum_price is None or minimum_price <= 0:
        return 100.0
    if budget is None or budget <= 0:
        return 0.0
    r = budget / minimum_price
    if r >= 1.15:
        return 100.0
    if r >= 1.0:
        return 88.0
    return min(100.0, r * 85.0)


def _historical_acceptance_rate(acc: int, ref: int) -> float:
    decided = acc + ref
    if decided == 0:
        return 0.65
    return acc / decided


def _blend_probability(norm_compat: float, hist_rate: float) -> float:
    p = 0.62 * norm_compat + 0.38 * hist_rate
    return min(0.97, max(0.05, p))


def _build_explanation(
    type_score: float,
    capacity_score: float,
    budget_score: float,
    history_score: float,
    historical_rate: float,
) -> str:
    parts: List[str] = []
    if type_score >= 95:
        parts.append("Le profil couvre le type d'événement.")
    elif type_score <= 5:
        parts.append(
            "Le type d'événement ne figure pas parmi les types acceptés par ce prestataire."
        )
    if capacity_score >= 95:
        parts.append("La capacité annoncée correspond au nombre de participants.")
    elif capacity_score < 70:
        parts.append(
            "Écart notable entre le nombre de participants et la fourchette de capacité du prestataire."
        )
    if budget_score >= 95:
        parts.append("Le budget est confortable par rapport au minimum tarifaire.")
    elif budget_score < 70:
        parts.append("Le budget est serré par rapport au prix minimum demandé.")
    if history_score >= 75:
        parts.append(
            "Historique favorable : taux d'acceptation élevé sur les demandes déjà traitées."
        )
    elif historical_rate < 0.45:
        parts.append("Historique : part importante de refus sur les demandes passées.")
    if not parts:
        parts.append(
            "Score obtenu par combinaison pondérée des critères type, capacité, budget et historique."
        )
    return " ".join(parts)


def score_one(event: EventIn, p: ProviderIn) -> ScoreItem:
    event_type = (event.eventType or "").strip()
    type_score = _score_event_type(event_type, p.acceptedEventTypes)
    capacity_score = _score_capacity(
        event.participantCount, p.minCapacity, p.maxCapacity
    )
    budget_score = _score_budget(event.budget, p.minimumPrice)

    acc = p.acceptedReservationCount
    ref = p.refusedReservationCount
    hist_rate = _historical_acceptance_rate(acc, ref)
    history_score = 100.0 * hist_rate

    weighted = (
        W_TYPE * type_score
        + W_CAPACITY * capacity_score
        + W_BUDGET * budget_score
        + W_HISTORY * history_score
    )
    compatibility = int(round(min(100, max(0, weighted))))

    acceptance = _blend_probability(compatibility / 100.0, hist_rate)
    explanation = _build_explanation(
        type_score, capacity_score, budget_score, history_score, hist_rate
    )

    return ScoreItem(
        providerUserId=p.providerUserId,
        businessName=p.businessName,
        compatibilityScore=compatibility,
        acceptanceProbability=_round2(acceptance),
        explanation=explanation,
    )


def rank_providers(req: ScoreRequest) -> List[ScoreItem]:
    out = [score_one(req.event, p) for p in req.providers]
    out.sort(key=lambda x: x.compatibilityScore, reverse=True)
    return out
