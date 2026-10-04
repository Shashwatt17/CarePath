package com.carepath.sharing;

import java.security.*;
import java.util.*;
import java.nio.charset.StandardCharsets;

final class ShareTokens {
    private static final SecureRandom RANDOM = new SecureRandom();
    static String create() {
        byte[] bytes = new byte[32]; RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
    static boolean valid(String value) {
        if (value == null || !value.matches("[A-Za-z0-9_-]{43}")) return false;
        return Base64.getUrlEncoder().withoutPadding().encodeToString(Base64.getUrlDecoder().decode(value)).equals(value);
    }
    static String digest(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.US_ASCII))); }
        catch (NoSuchAlgorithmException e) { throw new IllegalStateException("Digest unavailable"); }
    }
}
