package qkd.core;

import java.util.Random;

/**
 * Alice — the qubit sender in the BB84 QKD Protocol.
 *
 * Steps Alice performs:
 *  1. Generates N random classical bits  (0 or 1).
 *  2. Randomly picks an encoding basis per bit:
 *       '+' = Rectilinear  → encodes |0⟩ / |1⟩
 *       'x' = Diagonal     → encodes |+⟩ / |−⟩
 *  3. Transmits the encoded qubits over the quantum channel.
 *
 * After transmission, Alice publicly announces her bases so that
 * she and Bob can perform basis sifting to generate a shared key.
 */
public class Alice {

    private int[]  bits;   // random classical bits (0 or 1)
    private char[] bases;  // encoding bases ('+' or 'x')

    private final Random rng = new Random();

    /**
     * Generates N random bits and corresponding encoding bases.
     * @param n Number of qubits to prepare
     */
    public void generateBitsAndBases(int n) {
        bits  = new int[n];
        bases = new char[n];
        for (int i = 0; i < n; i++) {
            bits[i]  = rng.nextInt(2);
            bases[i] = rng.nextBoolean() ? '+' : 'x';
        }
    }

    public int[]  getBits()    { return bits; }
    public char[] getBases()   { return bases; }
    public int    getKeySize() { return bits != null ? bits.length : 0; }
}