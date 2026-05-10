package qkd.email;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Email — an immutable quantum-secured email message.
 *
 * Security properties stored per message:
 * ┌──────────────────────────────────────────────────────┐
 * │  encryptedContent  AES-256-GCM ciphertext (Base64)   │
 * │  messageHash       SHA-256 of original plaintext     │
 * │  digitalSignature  RSA-2048 signature (Base64)       │
 * │  bb84KeyBits       BB84 sifted key used for AES      │
 * │  keyGenTimeMs      BB84 generation time (ms)         │
 * │  encTimeMs         AES encryption time (ms)          │
 * └──────────────────────────────────────────────────────┘
 */
public class Email {

    private static final DateTimeFormatter FMT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // Metadata
    private final String        senderName;
    private final String        receiverName;
    private final LocalDateTime timestamp;

    // Cryptographic components
    private final String encryptedContent;   // AES-GCM Base64
    private final String messageHash;        // SHA-256 hex
    private final String digitalSignature;   // RSA-2048 Base64

    // Key material + metrics
    private final int[]  bb84KeyBits;
    private final long   keyGenTimeMs;
    private final long   encTimeMs;

    public Email(
        String senderName, String receiverName,
        String encryptedContent, String messageHash,
        String digitalSignature, int[] bb84KeyBits,
        long keyGenTimeMs, long encTimeMs
    ) {
        this.senderName       = senderName;
        this.receiverName     = receiverName;
        this.encryptedContent = encryptedContent;
        this.messageHash      = messageHash;
        this.digitalSignature = digitalSignature;
        this.bb84KeyBits      = bb84KeyBits;
        this.keyGenTimeMs     = keyGenTimeMs;
        this.encTimeMs        = encTimeMs;
        this.timestamp        = LocalDateTime.now();
    }

    public String  getSenderName()       { return senderName; }
    public String  getReceiverName()     { return receiverName; }
    public String  getEncryptedContent() { return encryptedContent; }
    public String  getMessageHash()      { return messageHash; }
    public String  getDigitalSignature() { return digitalSignature; }
    public int[]   getBb84KeyBits()      { return bb84KeyBits; }
    public long    getKeyGenTimeMs()     { return keyGenTimeMs; }
    public long    getEncTimeMs()        { return encTimeMs; }
    public String  getTimestamp()        { return timestamp.format(FMT); }

    @Override
    public String toString() {
        return String.format("Email[%s→%s | %s]", senderName, receiverName, timestamp.format(FMT));
    }
}