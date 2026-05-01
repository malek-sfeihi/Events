package com.eventmanagment.backend.provider;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProviderProfileRepository extends JpaRepository<ProviderProfile, Long> {
    Optional<ProviderProfile> findByProviderUserId(Long providerUserId);

    List<ProviderProfile> findByApproved(boolean approved);

    long countByApproved(boolean approved);
}
