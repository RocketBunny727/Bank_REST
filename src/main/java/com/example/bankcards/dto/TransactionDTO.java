package com.example.bankcards.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TransactionDTO {
    @NotNull(message = "Source card ID is required")
    private String sourceCardNumber;

    @NotNull(message = "Destination card ID is required")
    private String destinationCardNumber;

    @Positive(message = "Amount must be positive")
    @Pattern(regexp = "\\d.\\d{2}", message = "Invalid amount")
    private double amount;
}
