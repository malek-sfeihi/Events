package com.eventmanagment.backend.provider.dto;

import java.math.BigDecimal;
import java.util.Set;

public record ProviderProfileResponse(
        Long id,
        String businessName,
        Integer minCapacity,
        Integer maxCapacity,
        Set<String> acceptedEventTypes,
        BigDecimal minimumPrice,
        String availabilityNotes
) {}
