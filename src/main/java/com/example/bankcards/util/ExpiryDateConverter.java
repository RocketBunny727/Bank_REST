package com.example.bankcards.util;

import com.example.bankcards.exception.MissingExpiryDateException;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class ExpiryDateConverter {
    public static String convertDateToString(LocalDate expireDate) {
        if (expireDate == null) {
            throw new MissingExpiryDateException("Missing expiry date");
        }
        return expireDate.format(DateTimeFormatter.ofPattern("MM/yy"));
    }

    public static LocalDate convertStringToDate(String date) {
        if (date == null || date.isBlank()) {
            throw new MissingExpiryDateException("Missing expiry date");
        }

        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/yy");
            YearMonth yearMonth = YearMonth.parse(date, formatter);
            return yearMonth.atDay(1);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid date format, expected MM/yy: " + date, e);
        }
    }
}
