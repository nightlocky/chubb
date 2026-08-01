package com.chubbs.claims.dto;

import com.chubbs.claims.model.ClaimStatus;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import lombok.Data;

@Data
public class AssessmentRequestDTO {

    @PositiveOrZero
    private BigDecimal liabilityAmount;

    private ClaimStatus status;

    private String staffNotes;
}
