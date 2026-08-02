package com.chubbs.claims.dto;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LiabilityMetricDTO {

    private long submittedCount;

    private BigDecimal submittedLiability;

    private long inReviewCount;

    private BigDecimal inReviewLiability;

    private long infoRequestedCount;

    private BigDecimal infoRequestedLiability;

    private long settledCount;

    private BigDecimal settledLiability;

    private long rejectedCount;

    private BigDecimal rejectedLiability;

    private long outstandingCount;

    private BigDecimal outstandingLiability;

    private long totalCount;

    private BigDecimal totalLiability;
}
