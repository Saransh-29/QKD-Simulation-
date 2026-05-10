package qkd.security;

import java.security.MessageDigest;

/**
 * SHA-256 message hashing for integrity verification.
 *
 * Role in the system:
 *  - Sender hashes the plaintext BEFORE encryption.
 *  - Receiver hashes the DECRYPTED plaintext after decryption.
 *  - Matching hashes → message arrived intact, unmodified.
 *
 * Quantum note: SHA-256 is quantum-resistant.
 * Grover's reduces pre-image strength from 2^256 → 2^128 —
 * still computationally infeasible to attack.
 */
public final class HashUtil {

    private HashUtil() {}

    /**
     * Computes SHA-256 hash of a string.
     * @return 64-character lowercase hex string
     */
    public static String sha256(String input) {
        if (input == null) input = "";
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(input.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return "HASH_ERROR";
        }
    }

    /**
     * Verifies a message against a known SHA-256 hex hash.
     * Uses constant-time comparison to prevent timing side-channels.
     */
    public static boolean verify(String message, String expectedHash) {
        if (message == null || expectedHash == null) return false;
        String actual = sha256(message);
        if (actual.length() != expectedHash.length()) return false;
        int diff = 0;
        for (int i = 0; i < actual.length(); i++)
            diff |= actual.charAt(i) ^ expectedHash.charAt(i);
        return diff == 0;
    }

    /**
     * Returns a compact display version: "abcd1234...ef56" (8 + 4 chars).
     */
    public static String shortHash(String fullHash) {
        if (fullHash == null || fullHash.length() < 12) return fullHash;
        return fullHash.substring(0, 8) + "…" + fullHash.substring(fullHash.length() - 4);
    }

    /**
     * Formats a 64-char hash into groups of 8 for readability.
     */
    public static String formatted(String fullHash) {
        if (fullHash == null) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < fullHash.length(); i += 8) {
            if (i > 0) sb.append(' ');
            sb.append(fullHash, i, Math.min(i + 8, fullHash.length()));
        }
        return sb.toString();
    }
}