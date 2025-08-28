package com.example.bankcards.dto;

import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.User;
import jakarta.validation.constraints.Pattern;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class CardResponseDTO {
    private Long id;
    private String maskedNumber;
    private String owner;
    private String expiryDate;
    private CardStatus status;

    @Pattern(regexp = "\\d.\\d{2}")
    private double balance;

    private boolean isBlockRequested;
}
