package com.chubbs.claims.controller;

import com.chubbs.claims.dto.AssessmentRequestDTO;
import com.chubbs.claims.dto.AssignmentRequestDTO;
import com.chubbs.claims.dto.ClaimResponseDTO;
import com.chubbs.claims.service.ClaimService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/claims")
@RequiredArgsConstructor
public class StaffController {

    private final ClaimService claimService;

    /**
     * Lists submitted claims without an assigned officer.
     *
     * @return unassigned claims
     */
    @GetMapping("/queue")
    public ResponseEntity<List<ClaimResponseDTO>> getUnassignedQueue() {
        return ResponseEntity.ok(claimService.getUnassignedQueue());
    }

    /**
     * Assigns a claim to an officer.
     *
     * @param id claim id
     * @param request assignment request
     * @return updated claim response
     */
    @PostMapping("/{id}/assign")
    public ResponseEntity<ClaimResponseDTO> assignClaim(
            @PathVariable Long id,
            @Valid @RequestBody AssignmentRequestDTO request
    ) {
        return ResponseEntity.ok(claimService.assignClaim(id, request));
    }

    /**
     * Updates assessment details for a claim.
     *
     * @param id claim id
     * @param request assessment request
     * @return updated claim response
     */
    @PatchMapping("/{id}/assessment")
    public ResponseEntity<ClaimResponseDTO> assessClaim(
            @PathVariable Long id,
            @Valid @RequestBody AssessmentRequestDTO request
    ) {
        return ResponseEntity.ok(claimService.assessClaim(id, request));
    }
}
