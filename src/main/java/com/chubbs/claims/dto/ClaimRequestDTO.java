package com.chubbs.claims.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import lombok.Data;

@Data
public class ClaimRequestDTO {

    @NotBlank(message = "claimantId is required")
    @Size(max = 100, message = "claimantId must be 100 characters or fewer")
    private String claimantId;

    @NotBlank(message = "claimantEmail is required")
    @Email(message = "claimantEmail must be a valid email address")
    @Size(max = 255, message = "claimantEmail must be 255 characters or fewer")
    private String claimantEmail;

    @NotBlank(message = "policyType is required")
    @Pattern(regexp = "MOTOR|PROPERTY", message = "policyType must be MOTOR or PROPERTY")
    private String policyType;

    @NotBlank(message = "description is required")
    @Size(max = 4000, message = "description must be 4000 characters or fewer")
    private String description;

    @NotNull(message = "liabilityAmount is required")
    @PositiveOrZero(message = "liabilityAmount must be zero or greater")
    private BigDecimal liabilityAmount;
}
