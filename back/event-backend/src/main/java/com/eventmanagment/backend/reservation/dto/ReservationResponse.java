package com.eventmanagment.backend.reservation.dto;

import com.eventmanagment.backend.reservation.ReservationStatus;
import java.time.Instant;

public record ReservationResponse(
        Long id,
        Long eventId,
        Long organizerUserId,
        Long providerUserId,
        ReservationStatus status,
        Instant createdAt,
        Instant updatedAt
) {}
