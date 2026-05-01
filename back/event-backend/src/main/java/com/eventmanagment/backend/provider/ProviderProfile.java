package com.eventmanagment.backend.provider;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "provider_profiles")
public class ProviderProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long providerUserId;

    @Column(nullable = false, length = 150)
    private String businessName;

    @Column(nullable = false)
    private Integer minCapacity;

    @Column(nullable = false)
    private Integer maxCapacity;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "provider_event_types", joinColumns = @JoinColumn(name = "provider_profile_id"))
    @Column(name = "event_type", nullable = false, length = 64)
    private Set<String> acceptedEventTypes = new HashSet<>();

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal minimumPrice;

    @Column(length = 1000)
    private String availabilityNotes;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;
}
