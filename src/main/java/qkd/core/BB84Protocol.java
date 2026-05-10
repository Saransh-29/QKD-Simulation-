package qkd.core;

import java.util.ArrayList;
import java.util.List;

/**
 * BB84Protocol — orchestrates the complete BB84 QKD simulation.
 *
 * Protocol Flow:
 * ┌─────────────────────────────────────────────────────────────┐
 * │ 1. Alice generates random bits + encoding bases             │
 * │ 2. Bob generates random measurement bases                   │
 * │ 3. Quantum channel transmission (optional Eve intercept)    │
 * │ 4. Bob measures incoming qubits                             │
 * │ 5. Public basis sifting (keep matching-basis positions)     │
 * │ 6. Sifted key = bits at matching positions                  │
 * │ 7. QBER = error rate in sifted key (detects Eve)           │
 * └─────────────────────────────────────────────────────────────┘
 *
 * Security threshold: QBER > 20% → eavesdropping detected.
 */
public class BB84Protocol {

    // ── Participants ──────────────────────────────────────────────
    private final Alice alice = new Alice();
    private final Bob   bob   = new Bob();
    private final Eve   eve   = new Eve();

    // ── Results ───────────────────────────────────────────────────
    private List<Integer> matchingIndices = new ArrayList<>();
    private int[]         sharedKey;
    private double        qber;
    private boolean       eavesdropDetected;
    private boolean       evePresent;

    // ── Metrics ───────────────────────────────────────────────────
    private long keyGenTimeMs;  // total BB84 key-generation time

    // ── Log ───────────────────────────────────────────────────────
    private final List<String> logs = new ArrayList<>();

    // ── Threshold ─────────────────────────────────────────────────
    public static final double QBER_THRESHOLD = 0.20;

    // ─────────────────────────────────────────────────────────────
    // Main Protocol Entry Point
    // ─────────────────────────────────────────────────────────────

    /**
     * Runs the full BB84 protocol and populates all result fields.
     *
     * @param keySize Number of qubits to transmit
     * @param withEve Whether Eve intercepts the channel
     */
    public void runProtocol(int keySize, boolean withEve) {
        long startTime = System.currentTimeMillis();
        logs.clear();
        matchingIndices.clear();
        evePresent = withEve;

        log("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log("   BB84 QUANTUM KEY DISTRIBUTION  —  SIMULATION START");
        log("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log("  ▸ Qubit count : " + keySize);
        log("  ▸ Eve active  : " + (withEve ? "YES — ATTACK MODE" : "NO  — CLEAN CHANNEL"));
        log("");

        // ── STEP 1 : Alice prepares qubits ────────────────────────
        log("[ STEP 1 ]  Alice — Qubit Preparation");
        alice.generateBitsAndBases(keySize);
        log("  ▸ Random bits  generated : " + preview(alice.getBits(),  keySize));
        log("  ▸ Encoding bases chosen  : " + previewC(alice.getBases(), keySize));
        log("");

        // ── STEP 2 : Bob selects measurement bases ────────────────
        log("[ STEP 2 ]  Bob — Measurement Basis Selection");
        bob.generateBases(keySize);
        log("  ▸ Measurement bases      : " + previewC(bob.getBases(), keySize));
        log("");

        // ── STEP 3 : Quantum channel transmission ─────────────────
        int[]  bitsForBob;
        char[] encodingBases;

        if (withEve) {
            log("[ STEP 3 ]  ⚠  Eve — Interception Attack");
            bitsForBob    = eve.intercept(alice.getBits(), alice.getBases());
            encodingBases = eve.getBases();
            log("  ▸ Eve's random bases     : " + previewC(eve.getBases(), keySize));
            log("  ▸ Bits Eve forwarded     : " + preview(bitsForBob,      keySize));
        } else {
            bitsForBob    = alice.getBits();
            encodingBases = alice.getBases();
            log("[ STEP 3 ]  ✓  Quantum Channel — Clean Transmission");
            log("  ▸ Qubits delivered to Bob intact.");
        }
        log("");

        // ── STEP 4 : Bob measures ─────────────────────────────────
        log("[ STEP 4 ]  Bob — Quantum Measurement");
        bob.measureBits(bitsForBob, encodingBases);
        log("  ▸ Bob measured bits      : " + preview(bob.getMeasuredBits(), keySize));
        log("");

        // ── STEP 5 : Basis sifting ────────────────────────────────
        log("[ STEP 5 ]  Public Basis Comparison (Sifting)");
        char[] aliceBases = alice.getBases();
        char[] bobBases   = bob.getBases();
        for (int i = 0; i < keySize; i++) {
            if (aliceBases[i] == bobBases[i]) {
                matchingIndices.add(i);
            }
        }
        int matchCount = matchingIndices.size();
        log("  ▸ Positions compared     : " + keySize);
        log("  ▸ Matching bases         : " + matchCount
                + " / " + keySize
                + "  (" + pct(matchCount, keySize) + "%)");
        log("");

        // ── STEP 6 : Build sifted key ─────────────────────────────
        log("[ STEP 6 ]  Sifted Key Generation");
        int[] aliceBits = alice.getBits();
        sharedKey = new int[matchCount];
        for (int i = 0; i < matchCount; i++) {
            sharedKey[i] = aliceBits[matchingIndices.get(i)];
        }
        log("  ▸ Sifted key length      : " + sharedKey.length + " bits");
        log("  ▸ Shared key             : " + compact(sharedKey));
        log("");

        // ── STEP 7 : QBER analysis ────────────────────────────────
        log("[ STEP 7 ]  QBER — Quantum Bit Error Rate Analysis");
        calculateQBER();
        log("  ▸ QBER                   : " + String.format("%.2f%%", qber * 100));
        log("  ▸ Safety threshold       : " + (int)(QBER_THRESHOLD * 100) + "%");
        log("");

        // ── Final verdict ─────────────────────────────────────────
        log("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        if (eavesdropDetected) {
            log("  RESULT ►  ⛔  EAVESDROPPING DETECTED");
            log("  QBER = " + String.format("%.2f%%", qber * 100)
                    + "  exceeds " + (int)(QBER_THRESHOLD * 100) + "% threshold.");
            log("  Shared key is COMPROMISED — discard and re-run.");
        } else {
            log("  RESULT ►  ✅  SECURE CHANNEL CONFIRMED");
            log("  QBER = " + String.format("%.2f%%", qber * 100)
                    + "  within acceptable limit.");
            log("  Shared key is SAFE for encryption.");
        }
        log("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        keyGenTimeMs = System.currentTimeMillis() - startTime;
    }

    // ─────────────────────────────────────────────────────────────
    // QBER Calculation
    // ─────────────────────────────────────────────────────────────

    /**
     * QBER = (mismatched bits at sifted positions) / (total sifted bits)
     * Threshold: 20% — BB84 security guarantee.
     */
    private void calculateQBER() {
        if (matchingIndices.isEmpty()) {
            qber = 0.0;
            eavesdropDetected = false;
            return;
        }
        int[] aliceBits = alice.getBits();
        int[] bobBits   = bob.getMeasuredBits();
        int   errors    = 0;
        for (int idx : matchingIndices) {
            if (aliceBits[idx] != bobBits[idx]) errors++;
        }
        qber = (double) errors / matchingIndices.size();
        eavesdropDetected = qber > QBER_THRESHOLD;
    }

    // ─────────────────────────────────────────────────────────────
    // Log / Format Helpers
    // ─────────────────────────────────────────────────────────────

    private void log(String msg) { logs.add(msg); }

    private String preview(int[] arr, int total) {
        int limit = Math.min(arr.length, 20);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < limit; i++) sb.append(arr[i]).append(' ');
        if (arr.length > 20) sb.append("... (+").append(arr.length - 20).append(")");
        return sb.toString().trim();
    }

    private String previewC(char[] arr, int total) {
        int limit = Math.min(arr.length, 20);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < limit; i++) sb.append(arr[i]).append(' ');
        if (arr.length > 20) sb.append("... (+").append(arr.length - 20).append(")");
        return sb.toString().trim();
    }

    private String compact(int[] arr) {
        if (arr == null || arr.length == 0) return "(empty)";
        StringBuilder sb = new StringBuilder();
        for (int v : arr) sb.append(v);
        return sb.toString();
    }

    private int pct(int part, int total) {
        return (int) Math.round((double) part / total * 100);
    }

    // ─────────────────────────────────────────────────────────────
    // Getters
    // ─────────────────────────────────────────────────────────────

    public Alice         getAlice()            { return alice; }
    public Bob           getBob()              { return bob; }
    public Eve           getEve()              { return eve; }
    public int[]         getSharedKey()        { return sharedKey; }
    public double        getQber()             { return qber; }
    public boolean       isEavesdropDetected() { return eavesdropDetected; }
    public boolean       isEvePresent()        { return evePresent; }
    public List<Integer> getMatchingIndices()  { return matchingIndices; }
    public List<String>  getLogs()             { return logs; }
    public long          getKeyGenTimeMs()     { return keyGenTimeMs; }
}