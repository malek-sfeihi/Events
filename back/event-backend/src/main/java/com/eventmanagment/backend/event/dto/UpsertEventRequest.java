package com.eventmanagment.backend.event.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public record UpsertEventRequest(
        @NotBlank String eventType,
        @NotNull @FutureOrPresent LocalDate eventDate,
        @NotNull @Min(1) Integer participantCount,
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal budget,
        String preferences
) {}
