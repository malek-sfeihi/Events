package com.eventmanagment.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "recommendation.python")
public record RecommendationPythonProperties(
        /** Si faux : scoring calculé en Java (sans appeler FastAPI). Utile pour CI ou démo hors ligne. */
        boolean enabled,
        /** Ex. http://127.0.0.1:8000 — sans slash final. */
        String baseUrl,
        /** Chemin relatif, ex. /api/v1/recommendations/score */
        String scorePath) {

    public RecommendationPythonProperties {
        if (scorePath == null || scorePath.isBlank()) {
            scorePath = "/api/v1/recommendations/score";
        }
        if (baseUrl != null && baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
    }
}
