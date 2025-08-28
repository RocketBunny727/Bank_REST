package com.example.bankcards.dto;

import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.User;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class CardCreateDTO {
    @NotBlank(message = "Card number is required")
    @Pattern(regexp = "\\d{4} \\d{4} \\d{4} \\d{4}", message = "Invalid card number format")
    private String cardNumber;

    @NotNull(message = "Expiry date is required")
    @Pattern(regexp = "\\d{2}/\\d{2}", message = "Invalid expiry date format")
    private String expiryDate;

    @NotNull(message = "User is required")
    private Long userId;

    @Pattern(regexp = "\\d.\\d{2}", message = "Invalid balance")
    private Double balance;
}
