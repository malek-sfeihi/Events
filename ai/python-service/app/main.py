"""
API FastAPI — moteur de recommandation EventSpace.
Le backend Spring Boot agrège les données métier et appelle POST /api/v1/recommendations/score.

Endpoints :
  GET  /health                        — santé du service
  POST /api/v1/recommendations/score  — classement des prestataires
  GET  /api/v1/model/info             — état et métriques du modèle ML
  POST /api/v1/model/train            — entraîne le modèle sur les données historiques PostgreSQL
"""
from __future__ import annotations

import os
import pickle
import logging
from datetime import datetime
from pathlib import Path
from typing import List, Optional

from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel

from .schemas import ScoreItem, ScoreRequest
from .scoring import rank_providers, get_model_info, reload_model, FEATURE_NAMES

logger = logging.getLogger(__name__)

app = FastAPI(
    title="EventSpace — moteur de recommandation IA",
    version="2.0.0",
    description=(
        "Scoring hybride règles + ML (Logistic Regression) "
        "pour la plateforme événements–prestataires."
    ),
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


# ────────────────────────────────────────────────────────────────────────────
# Schémas réponse
# ────────────────────────────────────────────────────────────────────────────
class ModelInfoResponse(BaseModel):
    ml_model_available:  bool
    scoring_mode:        str
    model_type:          Optional[str] = None
    trained_at:          Optional[str] = None
    n_samples:           Optional[int] = None
    accuracy:            Optional[float] = None
    roc_auc:             Optional[float] = None
    feature_names:       Optional[List[str]] = None
    model_path:          Optional[str] = None


class TrainResponse(BaseModel):
    success:   bool
    message:   str
    n_samples: Optional[int] = None
    accuracy:  Optional[float] = None
    roc_auc:   Optional[float] = None


# ────────────────────────────────────────────────────────────────────────────
# Routes
# ────────────────────────────────────────────────────────────────────────────
@app.get("/health")
def health():
    info = get_model_info()
    return {
        "status":       "up",
        "service":      "recommendation-python",
        "scoring_mode": info["scoring_mode"],
        "ml_ready":     info["ml_model_available"],
    }


@app.post("/api/v1/recommendations/score", response_model=List[ScoreItem])
def score(req: ScoreRequest) -> List[ScoreItem]:
    if not req.providers:
        return []
    return rank_providers(req)


@app.get("/api/v1/model/info", response_model=ModelInfoResponse)
def model_info():
    """Retourne l'état du modèle ML : disponibilité, métriques, date d'entraînement."""
    return ModelInfoResponse(**get_model_info())


@app.post("/api/v1/model/train", response_model=TrainResponse)
def train_model():
    """
    Entraîne un modèle Logistic Regression sur les données historiques de réservations
    récupérées depuis PostgreSQL, puis sauvegarde le modèle dans data/model.pkl.

    Variables d'environnement requises (voir .env.example) :
      DB_HOST, DB_PORT, DB_NAME, DB_USER, DB_PASSWORD
    """
    try:
        import numpy as np
        from sklearn.linear_model import LogisticRegression
        from sklearn.preprocessing import StandardScaler
        from sklearn.pipeline import Pipeline
        from sklearn.model_selection import cross_val_score
        from sklearn.metrics import roc_auc_score, accuracy_score
        import psycopg2
    except ImportError as e:
        raise HTTPException(status_code=500, detail=f"Dépendance manquante : {e}")

    # ── Connexion PostgreSQL ──────────────────────────────────────────────────
    db_params = {
        "host":     os.getenv("DB_HOST",     "localhost"),
        "port":     int(os.getenv("DB_PORT", "5432")),
        "dbname":   os.getenv("DB_NAME",     "eventdb"),
        "user":     os.getenv("DB_USER",     "postgres"),
        "password": os.getenv("DB_PASSWORD", "1234567890"),
    }

    query = """
        SELECT
            r.status,
            e.event_type,
            e.participant_count,
            e.budget,
            p.accepted_event_types,
            p.min_capacity,
            p.max_capacity,
            p.minimum_price,
            COUNT(*) FILTER (WHERE r2.status = 'ACCEPTEE') AS accepted_count,
            COUNT(*) FILTER (WHERE r2.status = 'REFUSEE')  AS refused_count
        FROM reservations r
        JOIN events e       ON r.event_id    = e.id
        JOIN provider_profiles p ON r.provider_id = p.provider_user_id
        LEFT JOIN reservations r2 ON r2.provider_id = r.provider_id
            AND r2.status IN ('ACCEPTEE', 'REFUSEE')
            AND r2.id < r.id
        WHERE r.status IN ('ACCEPTEE', 'REFUSEE')
        GROUP BY r.id, r.status, e.event_type, e.participant_count, e.budget,
                 p.accepted_event_types, p.min_capacity, p.max_capacity, p.minimum_price
        LIMIT 10000
    """

    try:
        conn = psycopg2.connect(**db_params)
        cur  = conn.cursor()
        cur.execute(query)
        rows = cur.fetchall()
        cur.close()
        conn.close()
    except Exception as exc:
        logger.error("Connexion DB échouée : %s", exc)
        raise HTTPException(status_code=503, detail=f"Connexion base de données impossible : {exc}")

    if len(rows) < 10:
        raise HTTPException(
            status_code=422,
            detail=f"Données insuffisantes pour l'entraînement ({len(rows)} ligne(s)). "
                   "Il faut au minimum 10 réservations décidées (ACCEPTEE ou REFUSEE)."
        )

    # ── Construction du dataset ───────────────────────────────────────────────
    X_list, y_list = [], []

    for row in rows:
        (status, event_type, participants, budget,
         accepted_types, min_c, max_c, min_price,
         acc_hist, ref_hist) = row

        label = 1 if status == "ACCEPTEE" else 0

        et = (event_type or "").strip().casefold()
        accepted_list = accepted_types if isinstance(accepted_types, list) else []
        type_match = float(any(t and t.strip().casefold() == et for t in accepted_list))

        participants = max(int(participants or 0), 0)
        min_c   = max(int(min_c or 0), 0)
        max_c   = max(int(max_c or 0), 1)
        capacity_ratio = participants / max_c
        in_range       = float(min_c <= participants <= max_c)

        budget    = max(float(budget or 0), 0)
        min_price = max(float(min_price or 0), 1e-9)
        budget_ratio        = budget / min_price
        budget_comfortable  = float(budget_ratio >= 1.15)

        acc_hist = int(acc_hist or 0)
        ref_hist = int(ref_hist or 0)
        decided   = acc_hist + ref_hist
        hist_rate    = acc_hist / decided if decided > 0 else 0.65
        log_decided  = float(np.log1p(decided))

        X_list.append([type_match, capacity_ratio, in_range,
                        budget_ratio, hist_rate, log_decided, budget_comfortable])
        y_list.append(label)

    X = np.array(X_list, dtype=float)
    y = np.array(y_list, dtype=int)

    # ── Entraînement ─────────────────────────────────────────────────────────
    pipeline = Pipeline([
        ("scaler", StandardScaler()),
        ("clf",    LogisticRegression(
            C=1.0,
            class_weight="balanced",
            max_iter=1000,
            random_state=42,
        )),
    ])
    pipeline.fit(X, y)

    y_pred  = pipeline.predict(X)
    y_proba = pipeline.predict_proba(X)[:, 1]
    acc     = float(accuracy_score(y, y_pred))
    auc     = float(roc_auc_score(y, y_proba)) if len(set(y)) > 1 else None

    # ── Sauvegarde ───────────────────────────────────────────────────────────
    model_path = Path(__file__).parent.parent / "data" / "model.pkl"
    model_path.parent.mkdir(parents=True, exist_ok=True)

    payload = {
        "pipeline":      pipeline,
        "model_type":    "LogisticRegression + StandardScaler",
        "feature_names": FEATURE_NAMES,
        "n_samples":     len(y_list),
        "accuracy":      round(acc, 4),
        "roc_auc":       round(auc, 4) if auc else None,
        "trained_at":    datetime.utcnow().isoformat() + "Z",
    }

    with open(model_path, "wb") as f:
        pickle.dump(payload, f)

    # Rechargement en mémoire
    info = reload_model()
    logger.info("Modèle entraîné et rechargé : accuracy=%.3f roc_auc=%s", acc, auc)

    return TrainResponse(
        success=True,
        message=f"Modèle entraîné sur {len(y_list)} échantillons et rechargé avec succès.",
        n_samples=len(y_list),
        accuracy=round(acc, 4),
        roc_auc=round(auc, 4) if auc else None,
    )