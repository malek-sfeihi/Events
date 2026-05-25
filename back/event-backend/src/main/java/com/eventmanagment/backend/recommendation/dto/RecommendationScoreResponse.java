package com.eventmanagment.backend.recommendation.dto;

/**
 * Résultat d'évaluation pour un prestataire candidat : score de compatibilité,
 * probabilité d'acceptation estimée et texte d'explicabilité.
 */
public record RecommendationScoreResponse(
        long providerUserId,
        String businessName,
        int compatibilityScore,
        double acceptanceProbability,
        String explanation) {}
