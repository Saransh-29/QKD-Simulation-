package qkd.core;

import java.util.Random;

/**
 * Eve — the eavesdropper in the BB84 QKD Protocol.
 *
 * How Eve attacks:
 *  1. Intercepts each qubit Alice sends to Bob.
 *  2. Randomly picks a measurement basis (50% chance of matching Alice's).
 *  3. Measures the qubit — collapses quantum state if basis differs.
 *  4. Re-encodes with her measured value and forwards to Bob.
 *
 * Why Eve is detected:
 *  • Eve guesses the wrong basis ~50% of the time.
 *  • Wrong-basis measurements cause ~25% errors in the sifted key.
 *  • QBER ≈ 25%  →  exceeds 20% threshold  →  DETECTED.
 *
 * This is BB84's core security guarantee:
 * "Any interception attempt leaves a detectable statistical trace."
 */
public class Eve {

    private char[] bases;           // Eve's randomly chosen measurement bases
    private int[]  interceptedBits; // bits Eve actually read

    private final Random rng = new Random();

    /**
     * Intercepts Alice's qubit stream, measures with random bases,
     * and returns the forwarded (possibly corrupted) bits to Bob.
     *
     * @param aliceBits  Alice's original bits
     * @param aliceBases Alice's encoding bases
     * @return int[] of bits forwarded to Bob (encoded with Eve's bases)
     */
    public int[] intercept(int[] aliceBits, char[] aliceBases) {
        int n = aliceBits.length;
        bases           = new char[n];
        interceptedBits = new int[n];
        int[] forwarded = new int[n];

        for (int i = 0; i < n; i++) {
            bases[i] = rng.nextBoolean() ? '+' : 'x';

            if (bases[i] == aliceBases[i]) {
                // Lucky match — reads correct qubit
                interceptedBits[i] = aliceBits[i];
            } else {
                // Basis mismatch — quantum state collapses → random result
                interceptedBits[i] = rng.nextInt(2);
            }

            // Eve forwards her measured value (wrong 25% of the time overall)
            forwarded[i] = interceptedBits[i];
        }
        return forwarded;
    }

    public char[] getBases()           { return bases; }
    public int[]  getInterceptedBits() { return interceptedBits; }
}