package com.eventmanagment.backend.provider;

import com.eventmanagment.backend.provider.dto.ProviderProfileResponse;
import com.eventmanagment.backend.provider.dto.UpsertProviderProfileRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/providers/me")
@RequiredArgsConstructor
public class ProviderProfileController {

    private final ProviderProfileService providerProfileService;

    @PutMapping
    public ResponseEntity<ProviderProfileResponse> upsert(
            @Valid @RequestBody UpsertProviderProfileRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(providerProfileService.upsert(authentication.getName(), request));
    }

    @GetMapping
    public ResponseEntity<ProviderProfileResponse> getMine(Authentication authentication) {
        return ResponseEntity.ok(providerProfileService.getMine(authentication.getName()));
    }

    @PostMapping("/logo")
    public ResponseEntity<ProviderProfileResponse> uploadLogo(
            @RequestParam("file") MultipartFile file,
            Authentication authentication
    ) {
        return ResponseEntity.ok(providerProfileService.uploadLogo(authentication.getName(), file));
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteMine(Authentication authentication) {
        providerProfileService.deleteMine(authentication.getName());
        return ResponseEntity.noContent().build();
    }
}
