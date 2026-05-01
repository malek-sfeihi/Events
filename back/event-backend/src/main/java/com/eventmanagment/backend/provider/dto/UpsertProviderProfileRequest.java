package com.eventmanagment.backend.provider.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.Set;

public record UpsertProviderProfileRequest(
        @NotBlank String businessName,
        @NotNull @Min(1) Integer minCapacity,
        @NotNull @Min(1) Integer maxCapacity,
        @NotEmpty Set<@NotBlank String> acceptedEventTypes,
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal minimumPrice,
        String availabilityNotes
) {}
