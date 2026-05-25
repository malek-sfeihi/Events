package com.eventmanagment.backend.admin.dto;

import com.eventmanagment.backend.user.Role;

public record AdminUserSummaryResponse(Long id, String email, String fullName, Role role, boolean enabled) {}
