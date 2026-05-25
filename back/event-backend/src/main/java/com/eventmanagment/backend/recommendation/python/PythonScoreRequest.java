package com.eventmanagment.backend.recommendation.python;

import java.util.List;

/** Payload envoyé au service FastAPI (champs JSON en camelCase). */
public record PythonScoreRequest(EventPayload event, List<ProviderPayload> providers) {

    public record EventPayload(String eventType, int participantCount, double budget) {}

    public record ProviderPayload(
            long providerUserId,
            String businessName,
            int minCapacity,
            int maxCapacity,
            List<String> acceptedEventTypes,
            double minimumPrice,
            int acceptedReservationCount,
            int refusedReservationCount) {}
}
