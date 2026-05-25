package com.eventmanagment.backend.recommendation;

import com.eventmanagment.backend.common.ResourceNotFoundException;
import com.eventmanagment.backend.config.RecommendationPythonProperties;
import com.eventmanagment.backend.event.Event;
import com.eventmanagment.backend.event.EventRepository;
import com.eventmanagment.backend.provider.ProviderProfile;
import com.eventmanagment.backend.provider.ProviderProfileRepository;
import com.eventmanagment.backend.recommendation.dto.RecommendationScoreResponse;
import com.eventmanagment.backend.recommendation.local.LocalRecommendationScorer;
import com.eventmanagment.backend.recommendation.python.PythonRecommendationClient;
import com.eventmanagment.backend.recommendation.python.PythonScoreRequest;
import com.eventmanagment.backend.recommendation.python.PythonScoreRequest.EventPayload;
import com.eventmanagment.backend.recommendation.python.PythonScoreRequest.ProviderPayload;
import com.eventmanagment.backend.reservation.ReservationRepository;
import com.eventmanagment.backend.reservation.ReservationStatus;
import com.eventmanagment.backend.user.Role;
import com.eventmanagment.backend.user.User;
import com.eventmanagment.backend.user.UserRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

/**
 * Orchestre les données métier : agrège événement + profils + historique, puis délègue le calcul au
 * service FastAPI (Python) ou au scorer local si désactivé.
 */
@Service
@RequiredArgsConstructor
public class RecommendationServiceImpl implements IARecommendationService {

    private final EventRepository eventRepository;
    private final ProviderProfileRepository providerProfileRepository;
    private final UserRepository userRepository;
    private final ReservationRepository reservationRepository;
    private final RecommendationPythonProperties pythonProperties;
    private final PythonRecommendationClient pythonRecommendationClient;
    private final LocalRecommendationScorer localRecommendationScorer;

    @Override
    public List<RecommendationScoreResponse> rankProvidersForEvent(String organizerEmail, Long eventId) {
        User organizer = userRepository
                .findByEmail(organizerEmail.toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (organizer.getRole() != Role.ORGANISATEUR) {
            throw new AccessDeniedException("Only ORGANISATEUR can request recommendations");
        }

        Event event = eventRepository.findById(eventId).orElseThrow(() -> new ResourceNotFoundException("Event not found"));
        if (!event.getOrganizerId().equals(organizer.getId())) {
            throw new AccessDeniedException("You do not own this event");
        }

        PythonScoreRequest payload = buildPayload(event);

        if (!pythonProperties.enabled()) {
            return localRecommendationScorer.rank(payload);
        }

        String baseUrl = pythonProperties.baseUrl();
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Le moteur Python est activé mais recommendation.python.base-url n'est pas défini.");
        }

        try {
            return pythonRecommendationClient.score(payload);
        } catch (RestClientException ex) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Le service FastAPI (Python) est injoignable. Démarrez-le : dossier ai/python-service "
                            + "(voir README.md), puis relancez la requête.",
                    ex);
        }
    }

    private PythonScoreRequest buildPayload(Event event) {
        EventPayload ev = new EventPayload(
                event.getEventType(),
                event.getParticipantCount(),
                event.getBudget().doubleValue());

        List<ProviderPayload> providers = new ArrayList<>();
        for (ProviderProfile profile : providerProfileRepository.findByApproved(true)) {
            if (!isListedProvider(profile)) {
                continue;
            }
            long pid = profile.getProviderUserId();
            int acc = (int) reservationRepository.countByProviderUserIdAndStatus(pid, ReservationStatus.ACCEPTEE);
            int ref = (int) reservationRepository.countByProviderUserIdAndStatus(pid, ReservationStatus.REFUSEE);

            providers.add(new ProviderPayload(
                    pid,
                    profile.getBusinessName(),
                    profile.getMinCapacity(),
                    profile.getMaxCapacity(),
                    new ArrayList<>(profile.getAcceptedEventTypes()),
                    profile.getMinimumPrice().doubleValue(),
                    acc,
                    ref));
        }
        return new PythonScoreRequest(ev, providers);
    }

    private boolean isListedProvider(ProviderProfile profile) {
        return userRepository
                .findById(profile.getProviderUserId())
                .filter(User::isEnabled)
                .filter(u -> u.getRole() == Role.PRESTATAIRE)
                .isPresent();
    }
}
