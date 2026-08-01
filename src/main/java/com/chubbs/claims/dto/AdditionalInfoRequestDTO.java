package com.chubbs.claims.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AdditionalInfoRequestDTO {

    @NotBlank(message = "additionalNotes is required")
    @Size(max = 4000, message = "additionalNotes must be 4000 characters or fewer")
    private String additionalNotes;
}
