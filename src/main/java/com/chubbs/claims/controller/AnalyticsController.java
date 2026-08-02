package com.chubbs.claims.controller;

import com.chubbs.claims.dto.LiabilityMetricDTO;
import com.chubbs.claims.service.ClaimService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/claims")
@RequiredArgsConstructor
public class AnalyticsController {

    private final ClaimService claimService;

    /**
     * Calculates claim liability by status.
     *
     * @return liability metric
     */
    @GetMapping("/analytics/liability")
    public ResponseEntity<LiabilityMetricDTO> getLiabilityMetric() {
        return ResponseEntity.ok(claimService.getLiabilityMetric());
    }
}
