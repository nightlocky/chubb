package com.chubbs.claims.dto;

import com.chubbs.claims.model.ClaimStatus;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import lombok.Data;

@Data
public class AssessmentRequestDTO {

    @PositiveOrZero(message = "liabilityAmount must be zero or greater")
    private BigDecimal liabilityAmount;

    private ClaimStatus status;

    @Size(max = 4000, message = "staffNotes must be 4000 characters or fewer")
    private String staffNotes;
}
