package com.maildesk.util;

import java.util.Base64;

public class EncryptionUtil {
    // Basic Base64 encoding — replace with proper encryption for production
    public static String encrypt(String text) {
        return Base64.getEncoder().encodeToString(text.getBytes());
    }

    public static String decrypt(String encoded) {
        return new String(Base64.getDecoder().decode(encoded));
    }
}
