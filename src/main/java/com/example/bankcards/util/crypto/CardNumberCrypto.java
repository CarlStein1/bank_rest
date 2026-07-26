package com.example.bankcards.util.crypto;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class CardNumberCrypto {

    private static final String KEY_ALGORITHM = "AES";
    private static final String CIPHER_TRANSFORMATION = "AES/GCM/NoPadding";

    private static final int IV_LENGTH_BYTES = 12;
    private static final int AUTHENTICATION_TAG_LENGTH_BITS = 128;

    private final SecretKey secretKey;
    private final short keyVersion;
    private final SecureRandom secureRandom;

    public CardNumberCrypto(
            @Value("${card.encryption-key}")
            String encodedKey,

            @Value("${card.encryption-key-version:1}")
            short keyVersion
    ) {
        this.secretKey = createSecretKey(encodedKey);
        this.keyVersion = keyVersion;
        this.secureRandom = new SecureRandom();
    }

    public EncryptedCardData encrypt(String cardNumber) {
        validateCardNumber(cardNumber);

        byte[] iv = generateIv();

        try {
            Cipher cipher = Cipher.getInstance(
                    CIPHER_TRANSFORMATION
            );

            GCMParameterSpec parameterSpec =
                    new GCMParameterSpec(
                            AUTHENTICATION_TAG_LENGTH_BITS,
                            iv
                    );

            cipher.init(
                    Cipher.ENCRYPT_MODE,
                    secretKey,
                    parameterSpec
            );

            byte[] encryptedNumber = cipher.doFinal(
                    cardNumber.getBytes(StandardCharsets.UTF_8)
            );

            return new EncryptedCardData(
                    encryptedNumber,
                    iv,
                    keyVersion
            );
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException(
                    "Не удалось зашифровать номер карты",
                    exception
            );
        }
    }

    public String decrypt(
            byte[] encryptedNumber,
            byte[] iv,
            short encryptedKeyVersion
    ) {
        validateEncryptedData(
                encryptedNumber,
                iv,
                encryptedKeyVersion
        );

        try {
            Cipher cipher = Cipher.getInstance(
                    CIPHER_TRANSFORMATION
            );

            GCMParameterSpec parameterSpec =
                    new GCMParameterSpec(
                            AUTHENTICATION_TAG_LENGTH_BITS,
                            iv
                    );

            cipher.init(
                    Cipher.DECRYPT_MODE,
                    secretKey,
                    parameterSpec
            );

            byte[] decryptedNumber = cipher.doFinal(
                    encryptedNumber
            );

            return new String(
                    decryptedNumber,
                    StandardCharsets.UTF_8
            );
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException(
                    "Не удалось расшифровать номер карты",
                    exception
            );
        }
    }

    private SecretKey createSecretKey(String encodedKey) {
        if (encodedKey == null || encodedKey.isBlank()) {
            throw new IllegalArgumentException(
                    "Ключ шифрования карты не указан"
            );
        }

        final byte[] keyBytes;

        try {
            keyBytes = Base64.getDecoder().decode(encodedKey);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "Ключ шифрования должен быть записан в Base64",
                    exception
            );
        }

        if (keyBytes.length != 16
                && keyBytes.length != 24
                && keyBytes.length != 32) {

            throw new IllegalArgumentException(
                    "AES-ключ должен содержать 16, 24 или 32 байта"
            );
        }

        return new SecretKeySpec(
                keyBytes,
                KEY_ALGORITHM
        );
    }

    private byte[] generateIv() {
        byte[] iv = new byte[IV_LENGTH_BYTES];
        secureRandom.nextBytes(iv);

        return iv;
    }

    private void validateCardNumber(String cardNumber) {
        if (cardNumber == null
                || !cardNumber.matches("\\d{16}")) {

            throw new IllegalArgumentException(
                    "Номер карты должен состоять из 16 цифр"
            );
        }
    }

    private void validateEncryptedData(
            byte[] encryptedNumber,
            byte[] iv,
            short encryptedKeyVersion
    ) {
        if (encryptedNumber == null
                || encryptedNumber.length == 0) {

            throw new IllegalArgumentException(
                    "Зашифрованный номер карты отсутствует"
            );
        }

        if (iv == null || iv.length != IV_LENGTH_BYTES) {
            throw new IllegalArgumentException(
                    "IV должен содержать 12 байт"
            );
        }

        if (encryptedKeyVersion != keyVersion) {
            throw new IllegalArgumentException(
                    "Версия ключа шифрования не поддерживается"
            );
        }
    }
}