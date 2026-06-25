package com.eventmanagment.backend.admin;

import com.eventmanagment.backend.admin.dto.AdminPendingProviderResponse;
import com.eventmanagment.backend.admin.dto.AdminStatsResponse;
import com.eventmanagment.backend.admin.dto.AdminUserSummaryResponse;
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
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
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

    public List<AdminUserSummaryResponse> listUsers() {
        return userRepository.findAll().stream()
                .sorted(Comparator.comparing(User::getId))
                .map(u -> new AdminUserSummaryResponse(
                        u.getId(), u.getEmail(), u.getFullName(), u.getRole(), u.isEnabled()))
                .toList();
    }

    public List<AdminPendingProviderResponse> listApprovedProviders() {
        return providerProfileRepository.findByApproved(true).stream()
                .map(this::toPendingRow)
                .toList();
    }

    @Transactional
    public ProviderProfileResponse revokeProvider(Long providerUserId) {
        ProviderProfile profile = providerProfileRepository.findByProviderUserId(providerUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Provider profile not found"));
        profile.setApproved(false);
        providerProfileRepository.save(profile);
        return toProfileResponse(profile);
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
        User admin = requireAdmin(adminEmail);

        if (userId.equals(admin.getId())) {
            throw new AccessDeniedException("Cannot change your own account status");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setEnabled(request.enabled());
        userRepository.save(user);
    }

    /**
     * Suppression définitive d'un organisateur ou prestataire (données liées incluses).
     * Réservé à l'admin ; impossible sur un compte ADMIN ou sur soi-même.
     */
    @Transactional
    public void deleteUser(Long userId, String adminEmail) {
        User admin = requireAdmin(adminEmail);

        if (userId.equals(admin.getId())) {
            throw new AccessDeniedException("Cannot delete your own account");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getRole() == Role.ADMIN) {
            throw new IllegalArgumentException("Cannot delete an ADMIN account");
        }
        if (user.getRole() != Role.ORGANISATEUR && user.getRole() != Role.PRESTATAIRE) {
            throw new IllegalArgumentException("Only ORGANISATEUR or PRESTATAIRE accounts can be deleted");
        }

        if (user.getRole() == Role.ORGANISATEUR) {
            deleteOrganizerData(user.getId());
        } else {
            deleteProviderData(user.getId());
        }

        userRepository.delete(user);
    }

    private User requireAdmin(String adminEmail) {
        User admin = userRepository
                .findByEmail(adminEmail.toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new ResourceNotFoundException("Admin user not found"));
        if (admin.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Only ADMIN can perform this action");
        }
        return admin;
    }

    private void deleteOrganizerData(Long organizerUserId) {
        List<Long> eventIds = eventRepository.findByOrganizerIdOrderByEventDateAsc(organizerUserId).stream()
                .map(e -> e.getId())
                .toList();

        if (!eventIds.isEmpty()) {
            reservationRepository.deleteByEventIdIn(eventIds);
        }
        reservationRepository.deleteByOrganizerUserId(organizerUserId);
        eventRepository.deleteByOrganizerId(organizerUserId);
    }

    private void deleteProviderData(Long providerUserId) {
        reservationRepository.deleteByProviderUserId(providerUserId);
        providerProfileRepository.deleteByProviderUserId(providerUserId);
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
        return
                new ProviderProfileResponse(
                profile.getId(),
                profile.getBusinessName(),
                profile.getMinCapacity(),
                profile.getMaxCapacity(),
                profile.getAcceptedEventTypes(),
                profile.getMinimumPrice(),
                profile.getAvailabilityNotes(),
                profile.isApproved(),
                profile.getLogoUrl());
    }
}
