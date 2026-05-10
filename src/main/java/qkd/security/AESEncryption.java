package qkd.security;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/**
 * AES-256-GCM Encryption / Decryption.
 *
 * ┌────────────────────────────────────────────────────────┐
 * │  Mode     : AES/GCM/NoPadding (Authenticated)          │
 * │  Key size : 256 bits (32 bytes), derived from BB84 key │
 * │  IV size  : 96 bits (12 bytes), random per message     │
 * │  Auth tag : 128 bits (16 bytes)                        │
 * │  Encoding : Base64 for safe transport                  │
 * └────────────────────────────────────────────────────────┘
 *
 * Wire format:  Base64( [12-byte IV] ‖ [ciphertext+GCM-tag] )
 *
 * Quantum note: AES-256 is considered quantum-safe.
 * Grover's algorithm reduces effective key strength to 128 bits —
 * still computationally infeasible for any foreseeable quantum computer.
 */
public class AESEncryption {

    private static final String ALGO      = "AES";
    private static final String CIPHER    = "AES/GCM/NoPadding";
    private static final int    IV_LEN    = 12;   // 96-bit IV (NIST recommended)
    private static final int    TAG_BITS  = 128;  // GCM authentication tag length
    private static final int    KEY_BYTES = 32;   // 256-bit key

    private long lastOpTimeMs = 0;

    // ─────────────────────────────────────────────────────────────
    // Key Derivation
    // ─────────────────────────────────────────────────────────────

    /**
     * Derives a 256-bit AES SecretKey from BB84 sifted key bits.
     *
     * Bit-packing:  8 bits → 1 byte.
     * Stretching:   cyclic repetition to fill exactly 32 bytes.
     * (Production systems use HKDF/SHA-256 for proper key derivation.)
     *
     * @param keyBits BB84 sifted key as int array (values 0 or 1)
     * @return 256-bit AES SecretKeySpec
     */
    public SecretKey deriveKeyFromBits(int[] keyBits) {
        if (keyBits == null || keyBits.length == 0)
            throw new IllegalArgumentException("BB84 key bits cannot be empty.");

        // Pack bits into bytes
        int numBytes = (keyBits.length + 7) / 8;
        byte[] packed = new byte[numBytes];
        for (int i = 0; i < keyBits.length; i++)
            if (keyBits[i] == 1)
                packed[i / 8] |= (byte)(1 << (7 - (i % 8)));

        // Stretch / truncate to exactly 32 bytes via cyclic wrap
        byte[] aesKey = new byte[KEY_BYTES];
        for (int i = 0; i < KEY_BYTES; i++)
            aesKey[i] = packed[i % packed.length];

        return new SecretKeySpec(aesKey, ALGO);
    }

    // ─────────────────────────────────────────────────────────────
    // Encryption
    // ─────────────────────────────────────────────────────────────

    /**
     * Encrypts plaintext with AES-256-GCM.
     *
     * @param plaintext  Message to encrypt (UTF-8)
     * @param key        256-bit AES SecretKey
     * @return Base64 string: IV(12) ‖ Ciphertext ‖ GCM-tag(16)
     */
    public String encrypt(String plaintext, SecretKey key) throws Exception {
        long t0 = System.currentTimeMillis();

        byte[] iv = new byte[IV_LEN];
        new SecureRandom().nextBytes(iv);

        Cipher c = Cipher.getInstance(CIPHER);
        c.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
        byte[] ct = c.doFinal(plaintext.getBytes("UTF-8"));

        // Prepend IV so receiver can extract it
        byte[] combined = new byte[IV_LEN + ct.length];
        System.arraycopy(iv, 0, combined, 0,       IV_LEN);
        System.arraycopy(ct, 0, combined, IV_LEN,  ct.length);

        lastOpTimeMs = System.currentTimeMillis() - t0;
        return Base64.getEncoder().encodeToString(combined);
    }

    // ─────────────────────────────────────────────────────────────
    // Decryption
    // ─────────────────────────────────────────────────────────────

    /**
     * Decrypts an AES-256-GCM ciphertext.
     * GCM automatically verifies the authentication tag —
     * throws AEADBadTagException if the ciphertext was tampered.
     *
     * @param encB64 Base64-encoded IV ‖ ciphertext ‖ tag
     * @param key    256-bit AES SecretKey (same as encryption)
     * @return Decrypted plaintext
     */
    public String decrypt(String encB64, SecretKey key) throws Exception {
        long t0 = System.currentTimeMillis();

        byte[] combined = Base64.getDecoder().decode(encB64);
        byte[] iv = Arrays.copyOfRange(combined, 0,       IV_LEN);
        byte[] ct = Arrays.copyOfRange(combined, IV_LEN,  combined.length);

        Cipher c = Cipher.getInstance(CIPHER);
        c.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
        byte[] pt = c.doFinal(ct);

        lastOpTimeMs = System.currentTimeMillis() - t0;
        return new String(pt, "UTF-8");
    }

    // ─────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────

    /** Milliseconds taken by the last encrypt/decrypt call. */
    public long getLastOpTimeMs() { return lastOpTimeMs; }

    /**
     * Returns hex representation of the AES key (for display only).
     * NEVER expose the real key in production systems.
     */
    public static String keyToHex(SecretKey k) {
        StringBuilder sb = new StringBuilder();
        for (byte b : k.getEncoded()) sb.append(String.format("%02X", b));
        return sb.toString();
    }

    /** Short preview of a Base64 ciphertext for display. */
    public static String preview(String b64) {
        if (b64 == null) return "(none)";
        return b64.length() <= 44 ? b64
             : b64.substring(0, 40) + "… [+" + (b64.length() - 40) + " chars]";
    }
}