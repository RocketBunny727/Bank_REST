package com.example.bankcards.entity;

import com.example.bankcards.exception.MissingExpiryDateException;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Entity
@Table(name = "cards")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Card {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(nullable = false)
    private String number;

    public String getMaskedNumber() {
        return "**** **** ****" + number.substring(number.length() - 4);
    }

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private CardStatus status;

    @Column(nullable = false)
    private String owner;

    @Column(nullable = false)
    private double balance;

    @Column(nullable = false)
    private LocalDate expiryDate;

    public String getFormattedExpiryDate() {
        if (expiryDate == null) {
            throw new MissingExpiryDateException("No expiry date");
        }
        return expiryDate.format(DateTimeFormatter.ofPattern("MM/yy"));
    }

    public void blockCard() {
        this.status = CardStatus.BLOCKED;
    }

    public void unblockCard() {
        this.status = CardStatus.ACTIVE;
    }

    public boolean updateExpireStatus() {
        if (this.expiryDate != null && this.expiryDate.isBefore(LocalDate.now())) {
            this.status = CardStatus.EXPIRED;
            return true;
        }
        return false;
    }

    public boolean canTransact() {
        return this.status == CardStatus.ACTIVE;
    }

    public double makeTransaction(double amount) {
        return this.balance += amount;
    }

    @JoinColumn(name = "user_id")
    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private User user;
}
