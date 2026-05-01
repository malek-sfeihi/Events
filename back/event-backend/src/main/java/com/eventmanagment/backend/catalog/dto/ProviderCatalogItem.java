package com.eventmanagment.backend.catalog.dto;

import java.math.BigDecimal;
import java.util.Set;

public record ProviderCatalogItem(
        Long providerUserId,
        String businessName,
        Integer minCapacity,
        Integer maxCapacity,
        Set<String> acceptedEventTypes,
        BigDecimal minimumPrice,
        String availabilityNotes
) {}
