package com.eventmanagment.backend.recommendation;

import com.eventmanagment.backend.recommendation.dto.RecommendationScoreResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
public class RecommendationController {

    private final IARecommendationService recommendationService;

    /** Classement des prestataires pour un événement appartenant à l'organisateur connecté. */
    @GetMapping("/events/{eventId}")
    public ResponseEntity<List<RecommendationScoreResponse>> forEvent(
            @PathVariable Long eventId, Authentication authentication) {
        return ResponseEntity.ok(recommendationService.rankProvidersForEvent(authentication.getName(), eventId));
    }
}
