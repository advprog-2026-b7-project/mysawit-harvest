package id.ac.ui.cs.advprog.mysawit.harvest.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class HarvestCreateRequest {

    @NotBlank
    private String plantationId;

    @NotBlank
    private String buruhId;

    @NotNull
    @Positive
    private BigDecimal weightKg;

    @NotBlank
    private String description;
}