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
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClaimService {

    private final ClaimRepository claimRepository;
    private final ClaimEventPublisher claimEventPublisher;

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
        log.info(
                "Claim created: claimId={}, claimantId={}, status={}, liabilityAmount={}",
                savedClaim.getId(),
                savedClaim.getClaimantId(),
                savedClaim.getStatus(),
                savedClaim.getLiabilityAmount()
        );
        claimEventPublisher.publishLifecycleEvent(savedClaim, "CLAIM_CREATED");
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
        log.info(
                "Additional claim information provided: claimId={}, claimantId={}, status={}",
                savedClaim.getId(),
                savedClaim.getClaimantId(),
                savedClaim.getStatus()
        );
        claimEventPublisher.publishLifecycleEvent(savedClaim, "ADDITIONAL_INFO_PROVIDED");
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
        log.info(
                "Claim assigned: claimId={}, officerId={}, status={}",
                savedClaim.getId(),
                savedClaim.getAssignedOfficerId(),
                savedClaim.getStatus()
        );
        claimEventPublisher.publishLifecycleEvent(savedClaim, "CLAIM_ASSIGNED");
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
        log.info(
                "Claim assessment updated: claimId={}, status={}, liabilityAmount={}",
                savedClaim.getId(),
                savedClaim.getStatus(),
                savedClaim.getLiabilityAmount()
        );
        if (request.getStatus() != null && previousStatus != request.getStatus()) {
            claimEventPublisher.publishLifecycleEvent(savedClaim, "CLAIM_ASSESSED");
        }
        return toResponse(savedClaim);
    }

    /**
     * Calculates liability exposure by claim status.
     *
     * @return liability metric
     */
    public LiabilityMetricDTO getLiabilityMetric() {
        List<ClaimStatus> outstandingStatuses = List.of(
                ClaimStatus.SUBMITTED,
                ClaimStatus.IN_REVIEW,
                ClaimStatus.INFO_REQUESTED
        );

        long submittedCount = claimRepository.countByStatus(ClaimStatus.SUBMITTED);
        long inReviewCount = claimRepository.countByStatus(ClaimStatus.IN_REVIEW);
        long infoRequestedCount = claimRepository.countByStatus(ClaimStatus.INFO_REQUESTED);
        long settledCount = claimRepository.countByStatus(ClaimStatus.SETTLED);
        long rejectedCount = claimRepository.countByStatus(ClaimStatus.REJECTED);
        long outstandingCount = claimRepository.countByStatusIn(outstandingStatuses);
        long totalCount = claimRepository.count();

        BigDecimal submittedLiability = getLiabilityForStatus(ClaimStatus.SUBMITTED);
        BigDecimal inReviewLiability = getLiabilityForStatus(ClaimStatus.IN_REVIEW);
        BigDecimal infoRequestedLiability = getLiabilityForStatus(ClaimStatus.INFO_REQUESTED);
        BigDecimal settledLiability = getLiabilityForStatus(ClaimStatus.SETTLED);
        BigDecimal rejectedLiability = getLiabilityForStatus(ClaimStatus.REJECTED);
        BigDecimal outstandingLiability = getLiabilityForStatuses(outstandingStatuses);
        BigDecimal totalLiability = submittedLiability
                .add(inReviewLiability)
                .add(infoRequestedLiability)
                .add(settledLiability)
                .add(rejectedLiability);

        log.info(
                "Liability metric calculated: outstandingCount={}, outstandingLiability={}, totalCount={}, totalLiability={}",
                outstandingCount,
                outstandingLiability,
                totalCount,
                totalLiability
        );
        log.info(
                "Liability by status calculated: submittedCount={}, submitted={}, inReviewCount={}, inReview={}, infoRequestedCount={}, infoRequested={}, settledCount={}, settled={}, rejectedCount={}, rejected={}",
                submittedCount,
                submittedLiability,
                inReviewCount,
                inReviewLiability,
                infoRequestedCount,
                infoRequestedLiability,
                settledCount,
                settledLiability,
                rejectedCount,
                rejectedLiability
        );

        return LiabilityMetricDTO.builder()
                .submittedCount(submittedCount)
                .submittedLiability(submittedLiability)
                .inReviewCount(inReviewCount)
                .inReviewLiability(inReviewLiability)
                .infoRequestedCount(infoRequestedCount)
                .infoRequestedLiability(infoRequestedLiability)
                .settledCount(settledCount)
                .settledLiability(settledLiability)
                .rejectedCount(rejectedCount)
                .rejectedLiability(rejectedLiability)
                .outstandingCount(outstandingCount)
                .outstandingLiability(outstandingLiability)
                .totalCount(totalCount)
                .totalLiability(totalLiability)
                .build();
    }

    private Claim findClaim(Long id) {
        return claimRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Claim not found"));
    }

    private BigDecimal getLiabilityForStatus(ClaimStatus status) {
        BigDecimal total = claimRepository.sumLiabilityAmountByStatus(status);
        return total == null ? BigDecimal.ZERO : total;
    }

    private BigDecimal getLiabilityForStatuses(List<ClaimStatus> statuses) {
        BigDecimal total = claimRepository.sumLiabilityAmountByStatusIn(statuses);
        return total == null ? BigDecimal.ZERO : total;
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
