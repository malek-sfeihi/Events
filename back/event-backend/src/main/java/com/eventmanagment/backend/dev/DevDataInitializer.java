package com.eventmanagment.backend.dev;

import com.eventmanagment.backend.event.Event;
import com.eventmanagment.backend.event.EventRepository;
import com.eventmanagment.backend.provider.ProviderProfile;
import com.eventmanagment.backend.provider.ProviderProfileRepository;
import com.eventmanagment.backend.user.Role;
import com.eventmanagment.backend.user.User;
import com.eventmanagment.backend.user.UserRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Crée des utilisateurs et données minimales pour tester tout le flux en local (profil {@code dev} uniquement).
 *
 * <p><strong>Mot de passe pour tous les comptes démo : {@value #DEMO_PASSWORD}</strong></p>
 *
 * <ul>
 *   <li>{@code admin@align.local} — ADMIN</li>
 *   <li>{@code orga@align.local} — ORGANISATEUR (+ un événement &quot;Mariage&quot; déjà créé)</li>
 *   <li>{@code presta.ok@align.local} — PRESTATAIRE, profil <strong>approuvé</strong> (visible catalogue)</li>
 *   <li>{@code presta.attente@align.local} — PRESTATAIRE, profil <strong>non approuvé</strong> (admin doit valider)</li>
 * </ul>
 */
@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class DevDataInitializer implements ApplicationRunner {

    static final String DEMO_PASSWORD = "Test1234";

    private static final String SEED_MARKER_EMAIL = "admin@align.local";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ProviderProfileRepository providerProfileRepository;
    private final EventRepository eventRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.existsByEmail(SEED_MARKER_EMAIL)) {
            log.info("Seed dev ignoré : {} existe déjà.", SEED_MARKER_EMAIL);
            return;
        }

        Instant now = Instant.now();
        String enc = passwordEncoder.encode(DEMO_PASSWORD);

        saveUser("admin@align.local", "Admin align", Role.ADMIN, enc, now);
        User orga = saveUser("orga@align.local", "Léa Organisateur", Role.ORGANISATEUR, enc, now);
        User prestaOk = saveUser("presta.ok@align.local", "Studio Lumière", Role.PRESTATAIRE, enc, now);
        User prestaWait = saveUser("presta.attente@align.local", "Traiteur Horizon", Role.PRESTATAIRE, enc, now);

        providerProfileRepository.save(ProviderProfile.builder()
                .providerUserId(prestaOk.getId())
                .businessName("Maison des fêtes — démo")
                .minCapacity(20)
                .maxCapacity(300)
                .acceptedEventTypes(Set.of("Mariage", "Séminaire", "Anniversaire"))
                .minimumPrice(new BigDecimal("1500.00"))
                .availabilityNotes("Espace modulable, jardin, parking. Idéal mariage 80–150 invités.")
                .createdAt(now)
                .updatedAt(now)
                .approved(true)
                .build());

        providerProfileRepository.save(ProviderProfile.builder()
                .providerUserId(prestaWait.getId())
                .businessName("DJ et Son — en attente")
                .minCapacity(10)
                .maxCapacity(200)
                .acceptedEventTypes(Set.of("Mariage", "Soirée"))
                .minimumPrice(new BigDecimal("800.00"))
                .availabilityNotes("Matériel pro, déplacement 50 km.")
                .createdAt(now)
                .updatedAt(now)
                .approved(false)
                .build());

        LocalDate wedding = LocalDate.now().plusMonths(4);
        eventRepository.save(Event.builder()
                .organizerId(orga.getId())
                .eventType("Mariage")
                .eventDate(wedding)
                .participantCount(80)
                .budget(new BigDecimal("12000.00"))
                .preferences("Ambiance champêtre, possibilité extérieur.")
                .createdAt(now)
                .updatedAt(now)
                .build());

        log.info(
                "Seed dev : 4 comptes + 2 profils prestataires + 1 événement. Mot de passe partagé : {}",
                DEMO_PASSWORD);
    }

    private User saveUser(String email, String fullName, Role role, String encodedPassword, Instant createdAt) {
        User u = User.builder()
                .email(email)
                .fullName(fullName)
                .password(encodedPassword)
                .role(role)
                .enabled(true)
                .createdAt(createdAt)
                .build();
        return userRepository.save(u);
    }
}
