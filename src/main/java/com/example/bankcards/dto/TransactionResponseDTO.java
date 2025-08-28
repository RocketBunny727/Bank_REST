package com.example.bankcards.dto;

import jakarta.validation.constraints.Pattern;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TransactionResponseDTO {
    private long sourceId;
    private long destinationId;
    private String maskedSourceCardNumber;
    private String maskedDestinationCardNumber;

    @Pattern(regexp = "\\d.\\d{2}")
    private double sourceBalance;

    @Pattern(regexp = "\\d.\\d{2}")
    private double destinationBalance;

    private String owner;
}
