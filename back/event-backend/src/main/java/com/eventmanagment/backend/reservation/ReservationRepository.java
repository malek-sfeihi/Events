package com.eventmanagment.backend.reservation;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    List<Reservation> findByOrganizerUserIdOrderByCreatedAtDesc(Long organizerUserId);

    List<Reservation> findByProviderUserIdOrderByCreatedAtDesc(Long providerUserId);

    Optional<Reservation> findByEventIdAndProviderUserIdAndStatus(
            Long eventId, Long providerUserId, ReservationStatus status);

    long countByStatus(ReservationStatus status);
}
