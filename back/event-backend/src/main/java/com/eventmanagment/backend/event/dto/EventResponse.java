package com.eventmanagment.backend.event.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record EventResponse(
        Long id,
        String eventType,
        LocalDate eventDate,
        Integer participantCount,
        BigDecimal budget,
        String preferences
) {}
