package com.eventmanagment.backend.event;

import com.eventmanagment.backend.event.dto.EventResponse;
import com.eventmanagment.backend.event.dto.UpsertEventRequest;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    @PostMapping
    public ResponseEntity<EventResponse> create(@Valid @RequestBody UpsertEventRequest request, Authentication authentication) {
        return ResponseEntity.ok(eventService.create(authentication.getName(), request));
    }

    @GetMapping
    public ResponseEntity<List<EventResponse>> getMine(Authentication authentication) {
        return ResponseEntity.ok(eventService.getMine(authentication.getName()));
    }

    @GetMapping("/{eventId}")
    public ResponseEntity<EventResponse> getById(@PathVariable Long eventId, Authentication authentication) {
        return ResponseEntity.ok(eventService.getById(authentication.getName(), eventId));
    }

    @PutMapping("/{eventId}")
    public ResponseEntity<EventResponse> update(
            @PathVariable Long eventId,
            @Valid @RequestBody UpsertEventRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(eventService.update(authentication.getName(), eventId, request));
    }

    @DeleteMapping("/{eventId}")
    public ResponseEntity<Void> delete(@PathVariable Long eventId, Authentication authentication) {
        eventService.delete(authentication.getName(), eventId);
        return ResponseEntity.noContent().build();
    }
}
