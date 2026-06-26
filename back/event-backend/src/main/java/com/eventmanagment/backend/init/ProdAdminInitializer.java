package com.eventmanagment.backend.init;

import com.eventmanagment.backend.user.Role;
import com.eventmanagment.backend.user.User;
import com.eventmanagment.backend.user.UserRepository;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Crée un compte ADMIN en production si ADMIN_INIT_EMAIL et ADMIN_INIT_PASSWORD
 * sont définis dans les variables d'environnement Railway.
 * Idempotent : ne fait rien si un admin avec cet email existe déjà.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProdAdminInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${ADMIN_INIT_EMAIL:}")
    private String adminEmail;

    @Value("${ADMIN_INIT_PASSWORD:}")
    private String adminPassword;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (adminEmail.isBlank() || adminPassword.isBlank()) {
            return;
        }

        String normalizedEmail = adminEmail.trim().toLowerCase();

        if (userRepository.existsByEmail(normalizedEmail)) {
            log.info("ProdAdminInitializer : admin {} existe déjà, skip.", normalizedEmail);
            return;
        }

        userRepository.save(User.builder()
                .email(normalizedEmail)
                .fullName("Admin")
                .password(passwordEncoder.encode(adminPassword))
                .role(Role.ADMIN)
                .enabled(true)
                .createdAt(Instant.now())
                .build());

        log.info("ProdAdminInitializer : compte ADMIN créé pour {}.", adminEmail);
    }
}
