package com.example.bankcards.entity;

import com.example.bankcards.util.ExpireDateConverter;
import com.example.bankcards.util.NumberMasker;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnTransformer;

import java.time.LocalDate;

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
    @ColumnTransformer(
            read = "pgp_sym_decrypt(number, current_setting('encrypt.key'))",
            write = "pgp_sym_encrypt(?, current_setting('encrypt.key'))"
    )
    private String number;

    public String getMaskedNumber() {
        return NumberMasker.mask(number);
    }

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private CardStatus status;

    @Column(nullable = false)
    private String owner;

    @Column(nullable = false)
    private double balance;

    @Column(nullable = false)
    private LocalDate expireDate;

    public String getFormattedExpiryDate() {
        return ExpireDateConverter.convertDateToString(this.expireDate);
    }

    public void blockCard() {
        this.status = CardStatus.BLOCKED;
    }

    public void unblockCard() {
        this.status = CardStatus.ACTIVE;
    }

    public boolean updateExpireStatus() {
        if (this.expireDate != null && this.expireDate.isBefore(LocalDate.now())) {
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
    @ManyToOne(cascade = CascadeType.PERSIST, fetch = FetchType.EAGER)
    private User user;
}
