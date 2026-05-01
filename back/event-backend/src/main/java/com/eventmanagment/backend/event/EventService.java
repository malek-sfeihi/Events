package com.eventmanagment.backend.event;

import com.eventmanagment.backend.common.ResourceNotFoundException;
import com.eventmanagment.backend.event.dto.EventResponse;
import com.eventmanagment.backend.event.dto.UpsertEventRequest;
import com.eventmanagment.backend.user.Role;
import com.eventmanagment.backend.user.User;
import com.eventmanagment.backend.user.UserRepository;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;

    public EventResponse create(String userEmail, UpsertEventRequest request) {
        User organizer = getOrganizer(userEmail);
        Instant now = Instant.now();
        Event event = Event.builder()
                .organizerId(organizer.getId())
                .eventType(request.eventType())
                .eventDate(request.eventDate())
                .participantCount(request.participantCount())
                .budget(request.budget())
                .preferences(request.preferences())
                .createdAt(now)
                .updatedAt(now)
                .build();
        Event saved = eventRepository.save(event);
        return toResponse(saved);
    }

    public List<EventResponse> getMine(String userEmail) {
        User organizer = getOrganizer(userEmail);
        return eventRepository.findByOrganizerIdOrderByEventDateAsc(organizer.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public EventResponse getById(String userEmail, Long eventId) {
        User organizer = getOrganizer(userEmail);
        Event event = findOwnedEvent(organizer.getId(), eventId);
        return toResponse(event);
    }

    public EventResponse update(String userEmail, Long eventId, UpsertEventRequest request) {
        User organizer = getOrganizer(userEmail);
        Event event = findOwnedEvent(organizer.getId(), eventId);
        event.setEventType(request.eventType());
        event.setEventDate(request.eventDate());
        event.setParticipantCount(request.participantCount());
        event.setBudget(request.budget());
        event.setPreferences(request.preferences());
        event.setUpdatedAt(Instant.now());
        Event saved = eventRepository.save(event);
        return toResponse(saved);
    }

    public void delete(String userEmail, Long eventId) {
        User organizer = getOrganizer(userEmail);
        Event event = findOwnedEvent(organizer.getId(), eventId);
        eventRepository.delete(event);
    }

    private User getOrganizer(String userEmail) {
        User user = userRepository.findByEmail(userEmail.toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (user.getRole() != Role.ORGANISATEUR) {
            throw new AccessDeniedException("Only ORGANISATEUR can manage events");
        }
        return user;
    }

    private Event findOwnedEvent(Long organizerId, Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));
        if (!event.getOrganizerId().equals(organizerId)) {
            throw new AccessDeniedException("You cannot access this event");
        }
        return event;
    }

    private EventResponse toResponse(Event event) {
        return new EventResponse(
                event.getId(),
                event.getEventType(),
                event.getEventDate(),
                event.getParticipantCount(),
                event.getBudget(),
                event.getPreferences()
        );
    }
}
