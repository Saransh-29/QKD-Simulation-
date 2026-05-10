package qkd.core;

import java.util.Random;

/**
 * Bob — the qubit receiver in the BB84 QKD Protocol.
 *
 * Steps Bob performs:
 *  1. Independently generates a random measurement basis per qubit.
 *  2. Measures each incoming qubit with his chosen basis.
 *
 * Measurement Rule (quantum mechanics):
 *  • Same basis as sender  → reads the CORRECT bit (100%).
 *  • Different basis       → reads a RANDOM bit (50/50) — Heisenberg.
 *
 * After measurement, Bob compares bases with Alice publicly
 * and keeps only the bits where bases matched.
 */
public class Bob {

    private char[] bases;        // randomly chosen measurement bases
    private int[]  measuredBits; // bits obtained after measurement

    private final Random rng = new Random();

    /**
     * Generates N random measurement bases for Bob.
     * @param n Number of expected incoming qubits
     */
    public void generateBases(int n) {
        bases = new char[n];
        for (int i = 0; i < n; i++) {
            bases[i] = rng.nextBoolean() ? '+' : 'x';
        }
    }

    /**
     * Simulates quantum measurement of incoming qubits.
     *
     * @param incomingBits    Bits coming over the channel (Alice's or Eve's)
     * @param encodingBases   Bases used to encode those bits
     */
    public void measureBits(int[] incomingBits, char[] encodingBases) {
        measuredBits = new int[incomingBits.length];
        for (int i = 0; i < incomingBits.length; i++) {
            if (bases[i] == encodingBases[i]) {
                measuredBits[i] = incomingBits[i]; // correct measurement
            } else {
                measuredBits[i] = rng.nextInt(2);  // random (basis mismatch)
            }
        }
    }

    public char[] getBases()        { return bases; }
    public int[]  getMeasuredBits() { return measuredBits; }
}