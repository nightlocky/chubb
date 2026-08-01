package com.chubbs.claims.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AssignmentRequestDTO {

    @NotBlank
    private String officerId;
}
