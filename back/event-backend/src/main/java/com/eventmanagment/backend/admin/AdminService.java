package com.eventmanagment.backend.admin;

import com.eventmanagment.backend.admin.dto.AdminPendingProviderResponse;
import com.eventmanagment.backend.admin.dto.AdminStatsResponse;
import com.eventmanagment.backend.admin.dto.UpdateUserEnabledRequest;
import com.eventmanagment.backend.common.ResourceNotFoundException;
import com.eventmanagment.backend.event.EventRepository;
import com.eventmanagment.backend.provider.ProviderProfile;
import com.eventmanagment.backend.provider.ProviderProfileRepository;
import com.eventmanagment.backend.provider.dto.ProviderProfileResponse;
import com.eventmanagment.backend.reservation.ReservationRepository;
import com.eventmanagment.backend.reservation.ReservationStatus;
import com.eventmanagment.backend.user.Role;
import com.eventmanagment.backend.user.User;
import com.eventmanagment.backend.user.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final ReservationRepository reservationRepository;
    private final ProviderProfileRepository providerProfileRepository;

    public AdminStatsResponse stats() {
        long org = userRepository.countByRole(Role.ORGANISATEUR);
        long presta = userRepository.countByRole(Role.PRESTATAIRE);
        long adm = userRepository.countByRole(Role.ADMIN);
        long pendingProfiles = providerProfileRepository.countByApproved(false);

        return new AdminStatsResponse(
                org,
                presta,
                adm,
                org + presta + adm,
                eventRepository.count(),
                reservationRepository.count(),
                reservationRepository.countByStatus(ReservationStatus.EN_ATTENTE),
                reservationRepository.countByStatus(ReservationStatus.ACCEPTEE),
                reservationRepository.countByStatus(ReservationStatus.REFUSEE),
                pendingProfiles);
    }

    public List<AdminPendingProviderResponse> listPendingProviders() {
        return providerProfileRepository.findByApproved(false).stream()
                .map(this::toPendingRow)
                .toList();
    }

    @Transactional
    public ProviderProfileResponse approveProvider(Long providerUserId) {
        ProviderProfile profile = providerProfileRepository.findByProviderUserId(providerUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Provider profile not found"));

        User user = userRepository.findById(providerUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (user.getRole() != Role.PRESTATAIRE) {
            throw new IllegalArgumentException("User is not a PRESTATAIRE");
        }

        profile.setApproved(true);
        providerProfileRepository.save(profile);
        return toProfileResponse(profile);
    }

    @Transactional
    public void updateUserEnabled(Long userId, UpdateUserEnabledRequest request, String adminEmail) {
        User admin = userRepository.findByEmail(adminEmail.toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("Admin user not found"));

        if (userId.equals(admin.getId())) {
            throw new AccessDeniedException("Cannot change your own account status");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setEnabled(request.enabled());
        userRepository.save(user);
    }

    private AdminPendingProviderResponse toPendingRow(ProviderProfile profile) {
        User user = userRepository.findById(profile.getProviderUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return new AdminPendingProviderResponse(
                profile.getProviderUserId(),
                user.getEmail(),
                user.getFullName(),
                toProfileResponse(profile));
    }

    private ProviderProfileResponse toProfileResponse(ProviderProfile profile) {
        return new ProviderProfileResponse(
                profile.getId(),
                profile.getBusinessName(),
                profile.getMinCapacity(),
                profile.getMaxCapacity(),
                profile.getAcceptedEventTypes(),
                profile.getMinimumPrice(),
                profile.getAvailabilityNotes(),
                profile.isApproved());
    }
}
