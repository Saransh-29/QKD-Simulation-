package qkd.security;

import java.security.*;
import java.util.Base64;

/**
 * RSA-2048 Digital Signatures (SHA256withRSA).
 *
 * ┌────────────────────────────────────────────────────────┐
 * │  Algorithm : SHA256withRSA                              │
 * │  Key size  : 2048-bit RSA keypair                       │
 * │  Purpose   : Non-repudiation + Sender authentication   │
 * └────────────────────────────────────────────────────────┘
 *
 * Security properties a valid signature provides:
 *  ✓ AUTHENTICATION    — proves message came from claimed sender
 *  ✓ INTEGRITY         — message was not modified after signing
 *  ✓ NON-REPUDIATION   — sender cannot deny authorship
 *
 * ⚠ Quantum Vulnerability:
 *  RSA is broken by Shor's algorithm on a sufficiently large
 *  quantum computer. For quantum-safe signing, use CRYSTALS-Dilithium
 *  (NIST FIPS 204). See PostQuantumSimulator for simulation.
 */
public final class SignatureUtil {

    private static final String SIG_ALGO = "SHA256withRSA";
    private static final String KEY_ALGO = "RSA";
    private static final int    KEY_SIZE = 2048;

    private SignatureUtil() {}

    /**
     * Generates a fresh RSA-2048 keypair.
     * Call once per user at account creation time.
     */
    public static KeyPair generateKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance(KEY_ALGO);
        kpg.initialize(KEY_SIZE, new SecureRandom());
        return kpg.generateKeyPair();
    }

    /**
     * Signs a message with the sender's RSA private key.
     * Internally computes SHA-256(message) then RSA-encrypts the hash.
     *
     * @param message    Plaintext to sign
     * @param privateKey Sender's private key
     * @return Base64-encoded RSA signature (~344 chars for 2048-bit key)
     */
    public static String sign(String message, PrivateKey privateKey) throws Exception {
        Signature sig = Signature.getInstance(SIG_ALGO);
        sig.initSign(privateKey, new SecureRandom());
        sig.update(message.getBytes("UTF-8"));
        return Base64.getEncoder().encodeToString(sig.sign());
    }

    /**
     * Verifies a RSA signature using the sender's public key.
     *
     * @param message     The decrypted plaintext
     * @param sigB64      Base64 signature to verify
     * @param publicKey   Sender's public key
     * @return true = signature valid; false = invalid / tampered
     */
    public static boolean verify(String message, String sigB64, PublicKey publicKey) {
        if (message == null || sigB64 == null || publicKey == null) return false;
        try {
            byte[] sigBytes = Base64.getDecoder().decode(sigB64);
            Signature sig = Signature.getInstance(SIG_ALGO);
            sig.initVerify(publicKey);
            sig.update(message.getBytes("UTF-8"));
            return sig.verify(sigBytes);
        } catch (Exception e) {
            return false;
        }
    }

    /** Compact display of a long RSA signature for UI panels. */
    public static String preview(String sigB64) {
        if (sigB64 == null || sigB64.length() < 30) return sigB64;
        return sigB64.substring(0, 20) + "… [RSA-2048, " + sigB64.length() + " chars]";
    }
}