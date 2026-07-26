package com.example.bankcards.util;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class CardNumberGenerator {

    private static final String TEST_BIN = "4252";
    private static final int CARD_NUMBER_LENGTH = 16;

    private final SecureRandom secureRandom = new SecureRandom();

    public String generate() {
        StringBuilder cardNumber = new StringBuilder(TEST_BIN);

        while (cardNumber.length() < CARD_NUMBER_LENGTH - 1) {
            cardNumber.append(secureRandom.nextInt(10));
        }

        int checkDigit = calculateCheckDigit(cardNumber.toString());
        cardNumber.append(checkDigit);

        return cardNumber.toString();
    }

    private int calculateCheckDigit(String numberWithoutCheckDigit) {
        int sum = 0;
        boolean doubleDigit = true;

        for (int i = numberWithoutCheckDigit.length() - 1; i >= 0; i--) {
            int digit = Character.getNumericValue(
                    numberWithoutCheckDigit.charAt(i)
            );

            if (doubleDigit) {
                digit *= 2;

                if (digit > 9) {
                    digit -= 9;
                }
            }

            sum += digit;
            doubleDigit = !doubleDigit;
        }

        return (10 - sum % 10) % 10;
    }
}