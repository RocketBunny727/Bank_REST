package com.example.bankcards.dto;

import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.User;

import java.time.LocalDate;

public class CardCreateDTO {
    private String cardNumber;
    private LocalDate expiryDate;
    private String owner;
    private CardStatus status;
    private User user;
}
