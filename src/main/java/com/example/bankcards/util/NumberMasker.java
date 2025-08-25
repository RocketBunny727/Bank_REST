package com.example.bankcards.util;

import com.example.bankcards.exception.MissingCardNumberException;

public class NumberMasker {
    public static String mask(String number) {
        if (number == null) {
            throw new MissingCardNumberException("Missing card number");
        }
        return "**** **** **** " + number.substring(number.length() - 4);
    }
}
