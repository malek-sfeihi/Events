package com.eventmanagment.backend.provider;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProviderProfileRepository extends JpaRepository<ProviderProfile, Long> {
    Optional<ProviderProfile> findByProviderUserId(Long providerUserId);
}
