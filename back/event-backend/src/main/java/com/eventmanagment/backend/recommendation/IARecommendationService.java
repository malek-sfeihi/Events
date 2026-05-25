package com.eventmanagment.backend.recommendation;

import com.eventmanagment.backend.recommendation.dto.RecommendationScoreResponse;
import java.util.List;

/** Contrat du moteur de classement / scoring (couche « intelligence » de la plateforme). */
public interface IARecommendationService {

    /**
     * Classe les prestataires éligibles pour l'événement donné (appartenance à l'organisateur vérifiée).
     */
    List<RecommendationScoreResponse> rankProvidersForEvent(String organizerEmail, Long eventId);
}
