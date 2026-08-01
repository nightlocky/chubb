package com.chubbs.claims.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AssignmentRequestDTO {

    @NotBlank(message = "officerId is required")
    @Size(max = 100, message = "officerId must be 100 characters or fewer")
    private String officerId;
}
