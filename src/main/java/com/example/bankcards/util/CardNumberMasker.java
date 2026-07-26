package com.example.bankcards.util;

import org.springframework.stereotype.Component;

@Component
public class CardNumberMasker {

    private static final String MASK_PREFIX = "**** **** **** ";

    public String mask(String lastFour) {
        if (lastFour == null || !lastFour.matches("\\d{4}")) {
            throw new IllegalArgumentException(
                    "Последние четыре цифры карты должны состоять из 4 цифр"
            );
        }

        return MASK_PREFIX + lastFour;
    }
}