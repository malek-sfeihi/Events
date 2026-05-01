package com.eventmanagment.backend.admin.dto;

import com.eventmanagment.backend.provider.dto.ProviderProfileResponse;

public record AdminPendingProviderResponse(
        Long providerUserId,
        String email,
        String fullName,
        ProviderProfileResponse profile
) {}
