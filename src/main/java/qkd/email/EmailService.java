package qkd.email;

import qkd.core.BB84Protocol;
import qkd.security.AESEncryption;
import qkd.security.HashUtil;
import qkd.security.SignatureUtil;

import javax.crypto.SecretKey;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * EmailService — orchestrates the full Quantum-Secure Email pipeline.
 *
 * Send pipeline (7 steps):
 * ┌──────────────────────────────────────────────────────────────┐
 * │ 1. Run BB84 → sifted key bits                                │
 * │ 2. Derive AES-256 key from BB84 key bits                     │
 * │ 3. Compute SHA-256 hash of plaintext                         │
 * │ 4. RSA-sign the plaintext with sender's private key          │
 * │ 5. AES-GCM encrypt the plaintext                             │
 * │ 6. Build Email object (ciphertext + hash + signature + meta) │
 * │ 7. Deliver to receiver's inbox                               │
 * └──────────────────────────────────────────────────────────────┘
 *
 * Receive pipeline (3 steps):
 * ┌──────────────────────────────────────────────────────────────┐
 * │ 1. AES-GCM decrypt ciphertext using same BB84-derived key    │
 * │ 2. Verify SHA-256 hash  → integrity check                    │
 * │ 3. Verify RSA signature → authenticity check                 │
 * └──────────────────────────────────────────────────────────────┘
 *
 * User registry: stores users by name for lookup.
 */
public class EmailService {

    // ── State ─────────────────────────────────────────────────────
    private final Map<String, User>      users        = new LinkedHashMap<>();
    private final List<String>           logs         = new ArrayList<>();
    private final BB84Protocol           protocol     = new BB84Protocol();
    private final AESEncryption          aes          = new AESEncryption();

    // ── Last operation results (for UI display) ───────────────────
    private String  lastEncryptedContent;
    private String  lastDecryptedContent;
    private String  lastMessageHash;
    private String  lastSignature;
    private boolean lastHashValid;
    private boolean lastSigValid;
    private long    lastTotalTimeMs;
    private String  lastAesKeyHex;
    private String  lastQberStr;
    private boolean lastEveDetected;

    // ── Default config ────────────────────────────────────────────
    private static final int DEFAULT_KEY_SIZE = 64; // qubits for BB84

    // ─────────────────────────────────────────────────────────────
    // User Registry
    // ─────────────────────────────────────────────────────────────

    /**
     * Registers a user by name. Creates their RSA keypair.
     * Idempotent — calling twice with same name returns existing user.
     *
     * @param name Display name
     * @return The User object (new or existing)
     */
    public User registerUser(String name) {
        return users.computeIfAbsent(name.trim(), User::new);
    }

    /**
     * Returns a registered user by name, or null if not found.
     */
    public User getUser(String name) {
        return users.get(name.trim());
    }

    // ─────────────────────────────────────────────────────────────
    // Send Secure Email
    // ─────────────────────────────────────────────────────────────

    /**
     * Executes the full quantum-secure send pipeline.
     *
     * @param senderName   Sender's registered name
     * @param receiverName Receiver's registered name
     * @param plaintext    The message to send
     * @param keySize      BB84 qubit count (min 32 for viable key)
     * @return The Email object stored in receiver's inbox
     * @throws Exception if any cryptographic step fails
     */
    public Email sendEmail(String senderName, String receiverName,
                           String plaintext, int keySize) throws Exception {

        long totalStart = System.currentTimeMillis();
        logs.clear();

        // ── Resolve users ──────────────────────────────────────────
        User sender   = registerUser(senderName);
        User receiver = registerUser(receiverName);

        logHdr("SECURE EMAIL — SEND PIPELINE");
        log("  Sender   : " + senderName);
        log("  Receiver : " + receiverName);
        log("  Message  : \"" + plaintext + "\"");
        log("");

        // ── STEP 1: BB84 key generation ───────────────────────────
        log("[ STEP 1 ]  BB84 Key Generation");
        protocol.runProtocol(keySize, false);
        int[] keyBits = protocol.getSharedKey();
        lastQberStr    = String.format("%.2f%%", protocol.getQber() * 100);
        lastEveDetected = protocol.isEavesdropDetected();

        if (keyBits == null || keyBits.length == 0) {
            log("  ✗ BB84 key is empty — not enough matching bases. Increase key size.");
            throw new IllegalStateException("BB84 produced no sifted key bits. Use larger key size.");
        }
        log("  ▸ BB84 key bits      : " + keyBits.length + " sifted bits");
        log("  ▸ Key (binary)       : " + bitsStr(keyBits));
        log("  ▸ QBER               : " + lastQberStr);
        log("  ▸ Channel status     : SECURE ✓");
        log("  ▸ Generation time    : " + protocol.getKeyGenTimeMs() + " ms");
        log("");

        // ── STEP 2: Derive AES key ────────────────────────────────
        log("[ STEP 2 ]  AES-256 Key Derivation (from BB84 bits)");
        SecretKey aesKey = aes.deriveKeyFromBits(keyBits);
        lastAesKeyHex = AESEncryption.keyToHex(aesKey);
        log("  ▸ AES-256 Key (hex)  : " + lastAesKeyHex.substring(0, 16) + "…");
        log("  ▸ Key size           : 256 bits  (32 bytes)");
        log("");

        // ── STEP 3: SHA-256 hash of plaintext ─────────────────────
        log("[ STEP 3 ]  SHA-256 Message Hashing");
        String hash = HashUtil.sha256(plaintext);
        lastMessageHash = hash;
        log("  ▸ SHA-256 hash       : " + HashUtil.formatted(hash));
        log("");

        // ── STEP 4: RSA digital signature ─────────────────────────
        log("[ STEP 4 ]  RSA-2048 Digital Signature");
        String signature = SignatureUtil.sign(plaintext, sender.getPrivateKey());
        lastSignature = signature;
        log("  ▸ Signed with        : " + senderName + "'s RSA-2048 private key");
        log("  ▸ Signature (preview): " + SignatureUtil.preview(signature));
        log("");

        // ── STEP 5: AES-GCM encryption ────────────────────────────
        log("[ STEP 5 ]  AES-256-GCM Encryption");
        String ciphertext = aes.encrypt(plaintext, aesKey);
        lastEncryptedContent = ciphertext;
        log("  ▸ Ciphertext (preview): " + AESEncryption.preview(ciphertext));
        log("  ▸ Encryption time     : " + aes.getLastOpTimeMs() + " ms");
        log("");

        // ── STEP 6: Assemble & deliver Email ──────────────────────
        log("[ STEP 6 ]  Assembling Secure Email Packet");
        Email email = new Email(
            senderName, receiverName,
            ciphertext, hash, signature, keyBits,
            protocol.getKeyGenTimeMs(), aes.getLastOpTimeMs()
        );
        log("  ▸ Email assembled     : hash + signature + ciphertext");

        log("[ STEP 7 ]  Delivering to " + receiverName + "'s inbox");
        receiver.receiveEmail(email);
        log("  ▸ Email delivered ✓");
        log("");

        lastTotalTimeMs = System.currentTimeMillis() - totalStart;
        logHdr("SEND COMPLETE  —  Total: " + lastTotalTimeMs + " ms");

        return email;
    }

    // ─────────────────────────────────────────────────────────────
    // Receive / Decrypt / Verify
    // ─────────────────────────────────────────────────────────────

    /**
     * Decrypts and verifies an email for the receiver.
     *
     * @param email      The Email object from the inbox
     * @param senderName The claimed sender (for public key lookup)
     * @return Decrypted plaintext message
     */
    public String receiveEmail(Email email, String senderName) throws Exception {
        long t0 = System.currentTimeMillis();
        logs.clear();

        logHdr("SECURE EMAIL — RECEIVE PIPELINE");
        log("  From     : " + email.getSenderName());
        log("  To       : " + email.getReceiverName());
        log("  Received : " + email.getTimestamp());
        log("");

        // ── STEP 1: Re-derive AES key from stored BB84 bits ───────
        log("[ STEP 1 ]  AES Key Re-Derivation");
        SecretKey aesKey = aes.deriveKeyFromBits(email.getBb84KeyBits());
        log("  ▸ AES key re-derived from BB84 key bits (" + email.getBb84KeyBits().length + " bits)");
        log("");

        // ── STEP 2: AES-GCM decryption ────────────────────────────
        log("[ STEP 2 ]  AES-256-GCM Decryption");
        String decrypted = aes.decrypt(email.getEncryptedContent(), aesKey);
        lastDecryptedContent = decrypted;
        log("  ▸ Decrypted message  : \"" + decrypted + "\"");
        log("  ▸ Decryption time    : " + aes.getLastOpTimeMs() + " ms");
        log("");

        // ── STEP 3: Hash verification ─────────────────────────────
        log("[ STEP 3 ]  SHA-256 Integrity Check");
        lastHashValid = HashUtil.verify(decrypted, email.getMessageHash());
        log("  ▸ Expected hash      : " + HashUtil.shortHash(email.getMessageHash()));
        log("  ▸ Computed hash      : " + HashUtil.shortHash(HashUtil.sha256(decrypted)));
        log("  ▸ Hash match         : " + (lastHashValid ? "✓  VALID" : "✗  INVALID — TAMPERED!"));
        log("");

        // ── STEP 4: Signature verification ────────────────────────
        log("[ STEP 4 ]  RSA-2048 Signature Verification");
        User sender = getUser(senderName);
        if (sender != null) {
            lastSigValid = SignatureUtil.verify(
                decrypted, email.getDigitalSignature(), sender.getPublicKey()
            );
            log("  ▸ Verified with      : " + senderName + "'s RSA-2048 public key");
        } else {
            lastSigValid = false;
            log("  ▸ Sender not found in registry — cannot verify signature.");
        }
        log("  ▸ Signature status   : " + (lastSigValid ? "✓  VALID" : "✗  INVALID"));
        log("");

        lastTotalTimeMs = System.currentTimeMillis() - t0;
        logHdr("RECEIVE COMPLETE  —  Total: " + lastTotalTimeMs + " ms");

        return decrypted;
    }

    // ─────────────────────────────────────────────────────────────
    // Log Helpers
    // ─────────────────────────────────────────────────────────────

    private void log(String msg)    { logs.add(msg); }
    private void logHdr(String msg) {
        logs.add("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        logs.add("  " + msg);
        logs.add("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }

    private String bitsStr(int[] bits) {
        if (bits == null) return "";
        int lim = Math.min(bits.length, 24);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lim; i++) sb.append(bits[i]);
        if (bits.length > 24) sb.append("…(+").append(bits.length - 24).append(")");
        return sb.toString();
    }

    // ─────────────────────────────────────────────────────────────
    // Getters (for UI)
    // ─────────────────────────────────────────────────────────────

    public List<String> getLogs()               { return logs; }
    public BB84Protocol getProtocol()           { return protocol; }
    public String  getLastEncryptedContent()    { return lastEncryptedContent; }
    public String  getLastDecryptedContent()    { return lastDecryptedContent; }
    public String  getLastMessageHash()         { return lastMessageHash; }
    public String  getLastSignature()           { return lastSignature; }
    public boolean isLastHashValid()            { return lastHashValid; }
    public boolean isLastSigValid()             { return lastSigValid; }
    public long    getLastTotalTimeMs()         { return lastTotalTimeMs; }
    public String  getLastAesKeyHex()           { return lastAesKeyHex; }
    public String  getLastQberStr()             { return lastQberStr; }
    public boolean isLastEveDetected()          { return lastEveDetected; }
    public Map<String, User> getUsers()         { return users; }
}