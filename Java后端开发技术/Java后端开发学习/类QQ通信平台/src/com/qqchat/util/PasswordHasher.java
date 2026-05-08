package com.qqchat.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;

public class PasswordHasher {
    private static final int SALT_LENGTH = 16;

    public static String hash(String password) {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[SALT_LENGTH];
        random.nextBytes(salt);
        String saltHex = HexFormat.of().formatHex(salt);
        String hash = sha256(saltHex + password);
        return saltHex + ":" + hash;
    }

    public static boolean verify(String password, String storedHash) {
        String[] parts = storedHash.split(":", 2);
        if (parts.length != 2) return false;
        String salt = parts[0];
        String hash = parts[1];
        String computed = sha256(salt + password);
        return hash.equals(computed);
    }

    private static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
