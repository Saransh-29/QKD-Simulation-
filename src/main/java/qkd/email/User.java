package qkd.email;

import qkd.security.SignatureUtil;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;

/**
 * User — a participant in the Quantum-Secure Email system.
 *
 * Each user holds:
 *  - A display name
 *  - An RSA-2048 keypair (auto-generated at construction)
 *  - An inbox of received Email objects
 *
 * Public keys are distributed openly for signature verification.
 * Private keys are kept secret and used only for signing.
 */
public class User {

    private final String      name;
    private final PrivateKey  privateKey;
    private final PublicKey   publicKey;
    private final List<Email> inbox = new ArrayList<>();

    /**
     * Creates a new user and generates their RSA-2048 keypair.
     * @param name Display name (e.g., "Alice", "Bob")
     */
    public User(String name) {
        this.name = name;
        try {
            KeyPair kp = SignatureUtil.generateKeyPair();
            this.privateKey = kp.getPrivate();
            this.publicKey  = kp.getPublic();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate keypair for: " + name, e);
        }
    }

    /** Adds an email to this user's inbox. */
    public void receiveEmail(Email email) { inbox.add(email); }

    /** Returns the most recent email in inbox, or null if empty. */
    public Email getLatestEmail() {
        return inbox.isEmpty() ? null : inbox.get(inbox.size() - 1);
    }

    public String      getName()       { return name; }
    public PrivateKey  getPrivateKey() { return privateKey; }
    public PublicKey   getPublicKey()  { return publicKey; }
    public List<Email> getInbox()      { return inbox; }

    @Override
    public String toString() { return "User[" + name + "]"; }
}