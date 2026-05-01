package com.eventmanagment.backend.reservation;

import com.eventmanagment.backend.reservation.dto.CreateReservationRequest;
import com.eventmanagment.backend.reservation.dto.ReservationResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;

    @PostMapping
    public ResponseEntity<ReservationResponse> create(
            @Valid @RequestBody CreateReservationRequest request, Authentication authentication) {
        return ResponseEntity.ok(reservationService.create(authentication.getName(), request));
    }

    @GetMapping("/sent")
    public ResponseEntity<List<ReservationResponse>> listSent(Authentication authentication) {
        return ResponseEntity.ok(reservationService.listSent(authentication.getName()));
    }

    @GetMapping("/received")
    public ResponseEntity<List<ReservationResponse>> listReceived(Authentication authentication) {
        return ResponseEntity.ok(reservationService.listReceived(authentication.getName()));
    }

    @PatchMapping("/{id}/accept")
    public ResponseEntity<ReservationResponse> accept(
            @PathVariable("id") Long id, Authentication authentication) {
        return ResponseEntity.ok(reservationService.accept(authentication.getName(), id));
    }

    @PatchMapping("/{id}/reject")
    public ResponseEntity<ReservationResponse> reject(
            @PathVariable("id") Long id, Authentication authentication) {
        return ResponseEntity.ok(reservationService.reject(authentication.getName(), id));
    }
}
