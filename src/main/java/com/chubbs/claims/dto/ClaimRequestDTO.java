package com.chubbs.claims.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import lombok.Data;

@Data
public class ClaimRequestDTO {

    @NotBlank
    private String claimantId;

    @NotBlank
    @Email
    private String claimantEmail;

    @NotBlank
    @Pattern(regexp = "MOTOR|PROPERTY", message = "policyType must be MOTOR or PROPERTY")
    private String policyType;

    @NotBlank
    private String description;

    @NotNull
    @PositiveOrZero
    private BigDecimal liabilityAmount;
}
