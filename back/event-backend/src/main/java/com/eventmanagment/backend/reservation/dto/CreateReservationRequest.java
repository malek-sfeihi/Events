package com.eventmanagment.backend.reservation.dto;

import jakarta.validation.constraints.NotNull;

public record CreateReservationRequest(
        @NotNull Long eventId,
        @NotNull Long providerUserId
) {}
