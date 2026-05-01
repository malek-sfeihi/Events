package com.eventmanagment.backend.catalog;

import com.eventmanagment.backend.catalog.dto.ProviderCatalogItem;
import com.eventmanagment.backend.common.ResourceNotFoundException;
import com.eventmanagment.backend.provider.ProviderProfile;
import com.eventmanagment.backend.provider.ProviderProfileRepository;
import com.eventmanagment.backend.user.Role;
import com.eventmanagment.backend.user.User;
import com.eventmanagment.backend.user.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProviderCatalogService {

    private final ProviderProfileRepository providerProfileRepository;
    private final UserRepository userRepository;

    public List<ProviderCatalogItem> listForOrganizer(String organizerEmail, String eventTypeFilter) {
        User organizer = userRepository.findByEmail(organizerEmail.toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (organizer.getRole() != Role.ORGANISATEUR) {
            throw new AccessDeniedException("Only ORGANISATEUR can browse provider catalog");
        }

        return providerProfileRepository.findByApproved(true).stream()
                .filter(this::isListedProvider)
                .filter(p -> eventTypeFilter == null
                        || eventTypeFilter.isBlank()
                        || p.getAcceptedEventTypes().contains(eventTypeFilter))
                .map(this::toItem)
                .toList();
    }

    private boolean isListedProvider(ProviderProfile profile) {
        return userRepository.findById(profile.getProviderUserId())
                .filter(User::isEnabled)
                .filter(u -> u.getRole() == Role.PRESTATAIRE)
                .isPresent();
    }

    private ProviderCatalogItem toItem(ProviderProfile p) {
        return new ProviderCatalogItem(
                p.getProviderUserId(),
                p.getBusinessName(),
                p.getMinCapacity(),
                p.getMaxCapacity(),
                p.getAcceptedEventTypes(),
                p.getMinimumPrice(),
                p.getAvailabilityNotes());
    }
}
