package com.example.userservice.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;

@Service
public class ImageCryptoService {
    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH_BITS = 128;
    private final SecureRandom secureRandom = new SecureRandom();
    private final SecretKeySpec keySpec;

    public ImageCryptoService(@Value("${app.image-encryption-secret:realmicroservice-image-secret-change-me}") String secret) {
        try {
            byte[] key = MessageDigest.getInstance("SHA-256").digest(secret.getBytes(StandardCharsets.UTF_8));
            this.keySpec = new SecretKeySpec(Arrays.copyOf(key, 32), "AES");
        } catch (Exception e) {
            throw new IllegalStateException("Image encryption key tayyorlanmadi", e);
        }
    }

    public byte[] encrypt(byte[] plainBytes) {
        try {
            byte[] iv = new byte[IV_LENGTH];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            byte[] encrypted = cipher.doFinal(plainBytes);
            return ByteBuffer.allocate(iv.length + encrypted.length).put(iv).put(encrypted).array();
        } catch (Exception e) {
            throw new IllegalStateException("Rasm shifrlanmadi", e);
        }
    }

    public byte[] decrypt(byte[] encryptedBytes) {
        try {
            ByteBuffer buffer = ByteBuffer.wrap(encryptedBytes);
            byte[] iv = new byte[IV_LENGTH];
            buffer.get(iv);
            byte[] cipherBytes = new byte[buffer.remaining()];
            buffer.get(cipherBytes);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, keySpec, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            return cipher.doFinal(cipherBytes);
        } catch (Exception e) {
            throw new IllegalStateException("Rasm ochilmadi", e);
        }
    }
}
