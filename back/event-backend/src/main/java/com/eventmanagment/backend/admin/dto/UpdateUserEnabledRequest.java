package com.eventmanagment.backend.admin.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateUserEnabledRequest(@NotNull Boolean enabled) {}
