package com.chubbs.claims.dto;

import com.chubbs.claims.model.ClaimStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ClaimResponseDTO {

    private Long id;
    private String claimantId;
    private String claimantEmail;
    private String policyType;
    private String description;
    private ClaimStatus status;
    private BigDecimal liabilityAmount;
    private String assignedOfficerId;
    private String staffNotes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
