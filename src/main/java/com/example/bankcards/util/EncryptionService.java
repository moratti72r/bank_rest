package com.example.bankcards.util;

import org.jasypt.encryption.StringEncryptor;
import org.springframework.stereotype.Component;

@Component
public class EncryptionService {

    private final StringEncryptor encryptor;

    public EncryptionService(StringEncryptor encryptor) {
        this.encryptor = encryptor;
    }

    public String encrypt(String data) {
        return encryptor.encrypt(data);
    }

    public String decrypt(String encryptedData) {
        return encryptor.decrypt(encryptedData);
    }
}
