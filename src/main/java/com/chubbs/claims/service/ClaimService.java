package com.chubbs.claims.service;

import com.chubbs.claims.dto.AdditionalInfoRequestDTO;
import com.chubbs.claims.dto.AssessmentRequestDTO;
import com.chubbs.claims.dto.AssignmentRequestDTO;
import com.chubbs.claims.dto.ClaimRequestDTO;
import com.chubbs.claims.dto.ClaimResponseDTO;
import com.chubbs.claims.dto.LiabilityMetricDTO;
import com.chubbs.claims.model.Claim;
import com.chubbs.claims.model.ClaimStatus;
import com.chubbs.claims.repository.ClaimRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ClaimService {

    private final ClaimRepository claimRepository;
    private final KafkaTemplate<String, Map<String, Object>> kafkaTemplate;

    @Value("${claims.kafka.topic}")
    private String claimLifecycleTopic;

    /**
     * Creates a submitted claim.
     *
     * @param request claim request
     * @return saved claim id
     */
    public Long createClaim(ClaimRequestDTO request) {
        Claim claim = Claim.builder()
                .claimantId(request.getClaimantId())
                .claimantEmail(request.getClaimantEmail())
                .policyType(request.getPolicyType())
                .description(request.getDescription())
                .liabilityAmount(request.getLiabilityAmount())
                .status(ClaimStatus.SUBMITTED)
                .build();

        Claim savedClaim = claimRepository.save(claim);
        publishLifecycleEvent(savedClaim, "CLAIM_CREATED");
        return savedClaim.getId();
    }

    /**
     * Finds a claim by id.
     *
     * @param id claim id
     * @return claim response
     */
    public ClaimResponseDTO getClaim(Long id) {
        return toResponse(findClaim(id));
    }

    /**
     * Adds claimant information to an existing claim.
     *
     * @param id claim id
     * @param request additional info request
     * @return updated claim response
     */
    public ClaimResponseDTO provideAdditionalInfo(Long id, AdditionalInfoRequestDTO request) {
        Claim claim = findClaim(id);
        claim.setDescription(claim.getDescription() + "\n\nAdditional information: " + request.getAdditionalNotes());
        if (claim.getStatus() == ClaimStatus.INFO_REQUESTED) {
            claim.setStatus(ClaimStatus.IN_REVIEW);
        }

        Claim savedClaim = claimRepository.save(claim);
        publishLifecycleEvent(savedClaim, "ADDITIONAL_INFO_PROVIDED");
        return toResponse(savedClaim);
    }

    /**
     * Lists submitted claims that have no assigned officer.
     *
     * @return unassigned claim responses
     */
    public List<ClaimResponseDTO> getUnassignedQueue() {
        return claimRepository.findByStatusAndAssignedOfficerIdIsNull(ClaimStatus.SUBMITTED)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Assigns a claim to a staff officer.
     *
     * @param id claim id
     * @param request assignment request
     * @return updated claim response
     */
    public ClaimResponseDTO assignClaim(Long id, AssignmentRequestDTO request) {
        Claim claim = findClaim(id);
        claim.setAssignedOfficerId(request.getOfficerId());
        claim.setStatus(ClaimStatus.IN_REVIEW);

        Claim savedClaim = claimRepository.save(claim);
        publishLifecycleEvent(savedClaim, "CLAIM_ASSIGNED");
        return toResponse(savedClaim);
    }

    /**
     * Updates claim assessment fields.
     *
     * @param id claim id
     * @param request assessment request
     * @return updated claim response
     */
    public ClaimResponseDTO assessClaim(Long id, AssessmentRequestDTO request) {
        if (request.getLiabilityAmount() == null
                && request.getStatus() == null
                && isBlank(request.getStaffNotes())) {
            throw new IllegalArgumentException("At least one assessment field must be provided");
        }

        Claim claim = findClaim(id);
        ClaimStatus previousStatus = claim.getStatus();

        if (request.getLiabilityAmount() != null) {
            claim.setLiabilityAmount(request.getLiabilityAmount());
        }
        if (request.getStatus() != null) {
            claim.setStatus(request.getStatus());
        }
        if (!isBlank(request.getStaffNotes())) {
            claim.setStaffNotes(request.getStaffNotes());
        }

        Claim savedClaim = claimRepository.save(claim);
        if (request.getStatus() != null && previousStatus != request.getStatus()) {
            publishLifecycleEvent(savedClaim, "CLAIM_ASSESSED");
        }
        return toResponse(savedClaim);
    }

    /**
     * Calculates total open liability exposure.
     *
     * @return liability metric
     */
    public LiabilityMetricDTO getLiabilityMetric() {
        Collection<ClaimStatus> activeStatuses = List.of(
                ClaimStatus.SUBMITTED,
                ClaimStatus.IN_REVIEW,
                ClaimStatus.INFO_REQUESTED
        );
        BigDecimal total = claimRepository.sumLiabilityAmountByStatusIn(activeStatuses);
        return new LiabilityMetricDTO(total == null ? BigDecimal.ZERO : total);
    }

    private Claim findClaim(Long id) {
        return claimRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Claim not found"));
    }

    private void publishLifecycleEvent(Claim claim, String action) {
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("claimId", claim.getId());
        event.put("claimantId", claim.getClaimantId());
        event.put("status", claim.getStatus().name());
        event.put("assignedOfficerId", claim.getAssignedOfficerId());
        event.put("liabilityAmount", claim.getLiabilityAmount());
        event.put("action", action);
        event.put("eventTime", LocalDateTime.now().toString());
        kafkaTemplate.send(claimLifecycleTopic, String.valueOf(claim.getId()), event);
    }

    private ClaimResponseDTO toResponse(Claim claim) {
        return ClaimResponseDTO.builder()
                .id(claim.getId())
                .claimantId(claim.getClaimantId())
                .claimantEmail(claim.getClaimantEmail())
                .policyType(claim.getPolicyType())
                .description(claim.getDescription())
                .status(claim.getStatus())
                .liabilityAmount(claim.getLiabilityAmount())
                .assignedOfficerId(claim.getAssignedOfficerId())
                .staffNotes(claim.getStaffNotes())
                .createdAt(claim.getCreatedAt())
                .updatedAt(claim.getUpdatedAt())
                .build();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
