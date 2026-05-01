package com.eventmanagment.backend.reservation;

import com.eventmanagment.backend.common.ResourceNotFoundException;
import com.eventmanagment.backend.event.Event;
import com.eventmanagment.backend.event.EventRepository;
import com.eventmanagment.backend.reservation.dto.CreateReservationRequest;
import com.eventmanagment.backend.reservation.dto.ReservationResponse;
import com.eventmanagment.backend.user.Role;
import com.eventmanagment.backend.user.User;
import com.eventmanagment.backend.user.UserRepository;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;

    @Transactional
    public ReservationResponse create(String organizerEmail, CreateReservationRequest request) {
        User organizer = requireOrganizer(organizerEmail);

        Event event = eventRepository.findById(request.eventId())
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));

        if (!event.getOrganizerId().equals(organizer.getId())) {
            throw new AccessDeniedException("You do not own this event");
        }

        User provider = userRepository.findById(request.providerUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Provider user not found"));

        if (provider.getRole() != Role.PRESTATAIRE) {
            throw new IllegalArgumentException("Target user must be a PRESTATAIRE");
        }

        reservationRepository
                .findByEventIdAndProviderUserIdAndStatus(
                        request.eventId(), request.providerUserId(), ReservationStatus.EN_ATTENTE)
                .ifPresent(r -> {
                    throw new IllegalArgumentException(
                            "A pending reservation already exists for this event and provider");
                });

        Instant now = Instant.now();
        Reservation saved = reservationRepository.save(Reservation.builder()
                .eventId(event.getId())
                .organizerUserId(organizer.getId())
                .providerUserId(provider.getId())
                .status(ReservationStatus.EN_ATTENTE)
                .createdAt(now)
                .updatedAt(now)
                .build());

        return toResponse(saved);
    }

    public List<ReservationResponse> listSent(String organizerEmail) {
        User organizer = requireOrganizer(organizerEmail);
        return reservationRepository.findByOrganizerUserIdOrderByCreatedAtDesc(organizer.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<ReservationResponse> listReceived(String providerEmail) {
        User provider = requireProvider(providerEmail);
        return reservationRepository.findByProviderUserIdOrderByCreatedAtDesc(provider.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public ReservationResponse accept(String providerEmail, Long reservationId) {
        User provider = requireProvider(providerEmail);
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found"));

        if (!reservation.getProviderUserId().equals(provider.getId())) {
            throw new AccessDeniedException("You cannot accept this reservation");
        }

        if (reservation.getStatus() != ReservationStatus.EN_ATTENTE) {
            throw new IllegalArgumentException("Reservation is not pending");
        }

        reservation.setStatus(ReservationStatus.ACCEPTEE);
        reservation.setUpdatedAt(Instant.now());
        return toResponse(reservationRepository.save(reservation));
    }

    @Transactional
    public ReservationResponse reject(String providerEmail, Long reservationId) {
        User provider = requireProvider(providerEmail);
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found"));

        if (!reservation.getProviderUserId().equals(provider.getId())) {
            throw new AccessDeniedException("You cannot reject this reservation");
        }

        if (reservation.getStatus() != ReservationStatus.EN_ATTENTE) {
            throw new IllegalArgumentException("Reservation is not pending");
        }

        reservation.setStatus(ReservationStatus.REFUSEE);
        reservation.setUpdatedAt(Instant.now());
        return toResponse(reservationRepository.save(reservation));
    }

    private User requireOrganizer(String email) {
        User user = userRepository.findByEmail(email.toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (user.getRole() != Role.ORGANISATEUR) {
            throw new AccessDeniedException("Only ORGANISATEUR can manage outgoing reservations");
        }
        return user;
    }

    private User requireProvider(String email) {
        User user = userRepository.findByEmail(email.toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (user.getRole() != Role.PRESTATAIRE) {
            throw new AccessDeniedException("Only PRESTATAIRE can manage incoming reservations");
        }
        return user;
    }

    private ReservationResponse toResponse(Reservation r) {
        return new ReservationResponse(
                r.getId(),
                r.getEventId(),
                r.getOrganizerUserId(),
                r.getProviderUserId(),
                r.getStatus(),
                r.getCreatedAt(),
                r.getUpdatedAt());
    }
}
