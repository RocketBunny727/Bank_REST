package com.example.bankcards.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class TransactionResponseDTO {
    private Long sourceId;
    private Long destinationId;
    private String maskedSourceCardNumber;
    private String maskedDestinationCardNumber;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "0.00")
    private Double sourceBalance;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "0.00")
    private Double destinationBalance;

    private String owner;
}
