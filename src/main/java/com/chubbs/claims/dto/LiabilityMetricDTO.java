package com.chubbs.claims.dto;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LiabilityMetricDTO {

    private BigDecimal totalLiability;
}
