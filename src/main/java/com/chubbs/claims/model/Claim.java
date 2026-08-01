package com.chubbs.claims.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Claim {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String claimantId;

    @Column(nullable = false)
    private String claimantEmail;

    @Column(nullable = false)
    private String policyType;

    @Column(length = 4000)
    private String description;

    @Enumerated(EnumType.STRING)
    private ClaimStatus status;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal liabilityAmount;

    private String assignedOfficerId;

    @Column(length = 4000)
    private String staffNotes;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    // Set creation and update timestamp before the claim is inserted into DB
    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    // Refresh the update timestamp before the claim is saved
    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
