package com.example.bankcards.util.crypto;

public record EncryptedCardData(
        byte[] encryptedNumber,
        byte[] iv,
        short keyVersion
) {
}