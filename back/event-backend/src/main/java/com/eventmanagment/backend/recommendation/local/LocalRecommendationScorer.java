package com.eventmanagment.backend.recommendation.local;

import com.eventmanagment.backend.recommendation.dto.RecommendationScoreResponse;
import com.eventmanagment.backend.recommendation.python.PythonScoreRequest;
import com.eventmanagment.backend.recommendation.python.PythonScoreRequest.ProviderPayload;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;

/**
 * Même logique que le service FastAPI — utilisée lorsque {@code recommendation.python.enabled=false}.
 */
@Component
public class LocalRecommendationScorer {

    private static final double W_TYPE = 0.30;
    private static final double W_CAPACITY = 0.25;
    private static final double W_BUDGET = 0.25;
    private static final double W_HISTORY = 0.20;

    public List<RecommendationScoreResponse> rank(PythonScoreRequest req) {
        String eventType =
                req.event().eventType() == null ? "" : req.event().eventType().trim();
        int participants = req.event().participantCount();
        BigDecimal budget = BigDecimal.valueOf(req.event().budget());

        List<RecommendationScoreResponse> results = new ArrayList<>();

        for (ProviderPayload p : req.providers()) {
            double typeScore = scoreEventType(eventType, p.acceptedEventTypes());
            double capacityScore = scoreCapacity(participants, p.minCapacity(), p.maxCapacity());
            double budgetScore = scoreBudget(budget, BigDecimal.valueOf(p.minimumPrice()));

            long acc = p.acceptedReservationCount();
            long ref = p.refusedReservationCount();
            double histRate = historicalAcceptanceRate(acc, ref);
            double historyScore = 100.0 * histRate;

            double weighted =
                    W_TYPE * typeScore + W_CAPACITY * capacityScore + W_BUDGET * budgetScore + W_HISTORY * historyScore;
            int compatibility = (int) Math.round(Math.min(100, Math.max(0, weighted)));

            double acceptance = blendAcceptanceProbability(compatibility / 100.0, histRate);
            String explanation = buildExplanation(typeScore, capacityScore, budgetScore, historyScore, histRate);

            results.add(new RecommendationScoreResponse(
                    p.providerUserId(),
                    p.businessName(),
                    compatibility,
                    round2(acceptance),
                    explanation));
        }

        results.sort(Comparator.comparingInt(RecommendationScoreResponse::compatibilityScore).reversed());
        return results;
    }

    private static double scoreEventType(String eventType, List<String> accepted) {
        if (eventType.isEmpty()) {
            return 50;
        }
        boolean match = accepted.stream()
                .anyMatch(t -> t != null && t.trim().equalsIgnoreCase(eventType));
        return match ? 100 : 0;
    }

    private static double scoreCapacity(int participants, int minC, int maxC) {
        if (participants >= minC && participants <= maxC) {
            return 100;
        }
        if (participants < minC) {
            if (minC <= 0) {
                return 100;
            }
            return 100.0 * participants / minC;
        }
        if (participants <= maxC * 1.5 && maxC > 0) {
            return 100.0 * maxC / participants;
        }
        return 35;
    }

    private static double scoreBudget(BigDecimal budget, BigDecimal minimumPrice) {
        if (minimumPrice == null || minimumPrice.signum() <= 0) {
            return 100;
        }
        if (budget == null || budget.signum() <= 0) {
            return 0;
        }
        BigDecimal ratio = budget.divide(minimumPrice, 4, RoundingMode.HALF_UP);
        double r = ratio.doubleValue();
        if (r >= 1.15) {
            return 100;
        }
        if (r >= 1.0) {
            return 88;
        }
        return Math.min(100, r * 85);
    }

    private static double historicalAcceptanceRate(long acc, long ref) {
        long decided = acc + ref;
        if (decided == 0) {
            return 0.65;
        }
        return (double) acc / decided;
    }

    private static double blendAcceptanceProbability(double normalizedCompatibility, double historicalRate) {
        double p = 0.62 * normalizedCompatibility + 0.38 * historicalRate;
        return Math.min(0.97, Math.max(0.05, p));
    }

    private static String buildExplanation(
            double typeScore,
            double capacityScore,
            double budgetScore,
            double historyScore,
            double historicalRate) {
        List<String> parts = new ArrayList<>();
        if (typeScore >= 95) {
            parts.add("Le profil couvre le type d'événement.");
        } else if (typeScore <= 5) {
            parts.add("Le type d'événement ne figure pas parmi les types acceptés par ce prestataire.");
        }
        if (capacityScore >= 95) {
            parts.add("La capacité annoncée correspond au nombre de participants.");
        } else if (capacityScore < 70) {
            parts.add("Écart notable entre le nombre de participants et la fourchette de capacité du prestataire.");
        }
        if (budgetScore >= 95) {
            parts.add("Le budget est confortable par rapport au minimum tarifaire.");
        } else if (budgetScore < 70) {
            parts.add("Le budget est serré par rapport au prix minimum demandé.");
        }
        if (historyScore >= 75) {
            parts.add("Historique favorable : taux d'acceptation élevé sur les demandes déjà traitées.");
        } else if (historicalRate < 0.45) {
            parts.add("Historique : part importante de refus sur les demandes passées.");
        }
        if (parts.isEmpty()) {
            parts.add("Score obtenu par combinaison pondérée des critères type, capacité, budget et historique.");
        }
        return String.join(" ", parts);
    }

    private static double round2(double v) {
        return BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
