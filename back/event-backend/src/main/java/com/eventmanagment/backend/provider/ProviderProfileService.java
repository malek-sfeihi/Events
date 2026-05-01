package com.eventmanagment.backend.provider;

import com.eventmanagment.backend.common.ResourceNotFoundException;
import com.eventmanagment.backend.provider.dto.ProviderProfileResponse;
import com.eventmanagment.backend.provider.dto.UpsertProviderProfileRequest;
import com.eventmanagment.backend.user.Role;
import com.eventmanagment.backend.user.User;
import com.eventmanagment.backend.user.UserRepository;
import java.time.Instant;
import java.util.HashSet;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProviderProfileService {

    private final ProviderProfileRepository providerProfileRepository;
    private final UserRepository userRepository;

    public ProviderProfileResponse upsert(String userEmail, UpsertProviderProfileRequest request) {
        User provider = getProvider(userEmail);

        if (request.minCapacity() > request.maxCapacity()) {
            throw new IllegalArgumentException("minCapacity cannot be greater than maxCapacity");
        }

        ProviderProfile profile = providerProfileRepository.findByProviderUserId(provider.getId())
                .orElseGet(() -> ProviderProfile.builder()
                        .providerUserId(provider.getId())
                        .createdAt(Instant.now())
                        .build());

        profile.setBusinessName(request.businessName());
        profile.setMinCapacity(request.minCapacity());
        profile.setMaxCapacity(request.maxCapacity());
        profile.setAcceptedEventTypes(new HashSet<>(request.acceptedEventTypes()));
        profile.setMinimumPrice(request.minimumPrice());
        profile.setAvailabilityNotes(request.availabilityNotes());
        profile.setUpdatedAt(Instant.now());

        ProviderProfile saved = providerProfileRepository.save(profile);
        return toResponse(saved);
    }

    public ProviderProfileResponse getMine(String userEmail) {
        User provider = getProvider(userEmail);
        ProviderProfile profile = providerProfileRepository.findByProviderUserId(provider.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Provider profile not found"));
        return toResponse(profile);
    }

    public void deleteMine(String userEmail) {
        User provider = getProvider(userEmail);
        ProviderProfile profile = providerProfileRepository.findByProviderUserId(provider.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Provider profile not found"));
        providerProfileRepository.delete(profile);
    }

    private User getProvider(String userEmail) {
        User user = userRepository.findByEmail(userEmail.toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (user.getRole() != Role.PRESTATAIRE) {
            throw new AccessDeniedException("Only PRESTATAIRE can manage provider profile");
        }
        return user;
    }

    private ProviderProfileResponse toResponse(ProviderProfile profile) {
        return new ProviderProfileResponse(
                profile.getId(),
                profile.getBusinessName(),
                profile.getMinCapacity(),
                profile.getMaxCapacity(),
                profile.getAcceptedEventTypes(),
                profile.getMinimumPrice(),
                profile.getAvailabilityNotes()
        );
    }
}
