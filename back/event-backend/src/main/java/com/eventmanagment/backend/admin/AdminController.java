package com.eventmanagment.backend.admin;

import com.eventmanagment.backend.admin.dto.AdminPendingProviderResponse;
import com.eventmanagment.backend.admin.dto.AdminStatsResponse;
import com.eventmanagment.backend.admin.dto.AdminUserSummaryResponse;
import com.eventmanagment.backend.admin.dto.UpdateUserEnabledRequest;
import com.eventmanagment.backend.provider.dto.ProviderProfileResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/stats")
    public AdminStatsResponse stats() {
        return adminService.stats();
    }

    @GetMapping("/users")
    public List<AdminUserSummaryResponse> users() {
        return adminService.listUsers();
    }

    @GetMapping("/providers/pending")
    public List<AdminPendingProviderResponse> pendingProviders() {
        return adminService.listPendingProviders();
    }

    @PatchMapping("/providers/{providerUserId}/approve")
    public ProviderProfileResponse approve(@PathVariable Long providerUserId) {
        return adminService.approveProvider(providerUserId);
    }

    @PatchMapping("/users/{userId}/enabled")
    public ResponseEntity<Void> setEnabled(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateUserEnabledRequest request,
            Authentication authentication) {

        adminService.updateUserEnabled(userId, request, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/users/{userId}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long userId, Authentication authentication) {
        adminService.deleteUser(userId, authentication.getName());
        return ResponseEntity.noContent().build();
    }
}
