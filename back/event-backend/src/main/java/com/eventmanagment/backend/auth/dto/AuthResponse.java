package com.eventmanagment.backend.auth.dto;

import com.eventmanagment.backend.user.Role;

public record AuthResponse(
        String token,
        Long userId,
        String email,
        Role role
) {}
