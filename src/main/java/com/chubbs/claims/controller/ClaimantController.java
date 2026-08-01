package com.chubbs.claims.controller;

import com.chubbs.claims.dto.AdditionalInfoRequestDTO;
import com.chubbs.claims.dto.ClaimRequestDTO;
import com.chubbs.claims.dto.ClaimResponseDTO;
import com.chubbs.claims.service.ClaimService;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
public class ClaimantController {

    private final ClaimService claimService;

    /**
     * Creates a new claim.
     *
     * @param request claim request
     * @return created claim id
     */
    @PostMapping
    public ResponseEntity<Map<String, Long>> createClaim(@Valid @RequestBody ClaimRequestDTO request) {
        Long id = claimService.createClaim(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("id", id));
    }

    /**
     * Tracks a claim by id.
     *
     * @param id claim id
     * @return claim response
     */
    @GetMapping("/{id}")
    public ResponseEntity<ClaimResponseDTO> trackClaim(@PathVariable Long id) {
        return ResponseEntity.ok(claimService.getClaim(id));
    }

    /**
     * Provides additional claim information.
     *
     * @param id claim id
     * @param request additional info request
     * @return updated claim response
     */
    @PatchMapping("/{id}/info")
    public ResponseEntity<ClaimResponseDTO> provideAdditionalInfo(
            @PathVariable Long id,
            @Valid @RequestBody AdditionalInfoRequestDTO request
    ) {
        return ResponseEntity.ok(claimService.provideAdditionalInfo(id, request));
    }
}
