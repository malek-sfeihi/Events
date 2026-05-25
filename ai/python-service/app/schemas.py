"""Contrats JSON alignés sur Jackson (camelCase) côté Spring Boot."""

from __future__ import annotations

from typing import List

from pydantic import BaseModel, Field


class EventIn(BaseModel):
    eventType: str = ""
    participantCount: int
    budget: float


class ProviderIn(BaseModel):
    providerUserId: int
    businessName: str
    minCapacity: int
    maxCapacity: int
    acceptedEventTypes: List[str] = Field(default_factory=list)
    minimumPrice: float
    acceptedReservationCount: int = 0
    refusedReservationCount: int = 0


class ScoreRequest(BaseModel):
    event: EventIn
    providers: List[ProviderIn]


class ScoreItem(BaseModel):
    providerUserId: int
    businessName: str
    compatibilityScore: int
    acceptanceProbability: float
    explanation: str
