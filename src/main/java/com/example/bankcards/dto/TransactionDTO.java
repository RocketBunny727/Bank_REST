package com.example.bankcards.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class TransactionDTO {
    @NotNull(message = "Source card ID is required")
    private String sourceCardNumber;

    @NotNull(message = "Destination card ID is required")
    private String destinationCardNumber;

    @Positive(message = "Amount must be positive")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "0.00")
    private Double amount;
}
