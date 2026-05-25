"""
API FastAPI du moteur de recommandation.
Le backend Spring Boot agrège les données métier et appelle POST /api/v1/recommendations/score.
"""

from __future__ import annotations

from typing import List

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from .schemas import ScoreItem, ScoreRequest
from .scoring import rank_providers

app = FastAPI(
    title="EventMatcher — moteur de recommandation",
    version="1.0.0",
    description="Scoring / ML pour la plateforme événements–prestataires.",
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


@app.get("/health")
def health():
    return {"status": "up", "service": "recommendation-python"}


@app.post("/api/v1/recommendations/score", response_model=List[ScoreItem])
def score(req: ScoreRequest) -> List[ScoreItem]:
    if not req.providers:
        return []
    return rank_providers(req)
