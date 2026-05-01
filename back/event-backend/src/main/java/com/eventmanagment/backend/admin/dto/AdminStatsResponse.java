package com.eventmanagment.backend.admin.dto;

public record AdminStatsResponse(
        long organizerCount,
        long prestataireCount,
        long adminCount,
        long totalUsers,
        long eventCount,
        long reservationCount,
        long reservationPendingCount,
        long reservationAcceptedCount,
        long reservationRejectedCount,
        long pendingProviderProfilesCount
) {}
