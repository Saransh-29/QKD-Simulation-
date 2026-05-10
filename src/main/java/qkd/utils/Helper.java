package qkd.utils;

/**
 * Helper — shared utility methods used across the QKD application.
 *
 * Covers:
 *  - Bit / basis array formatting for display
 *  - QBER percentage formatting
 *  - Risk classification labels
 */
public final class Helper {

    private Helper() {}

    // ─────────────────────────────────────────────────────────────
    // Bit / Basis Formatting
    // ─────────────────────────────────────────────────────────────

    /** Converts int[] bits → space-separated string: "0 1 1 0 …" */
    public static String bitsToString(int[] bits) {
        if (bits == null || bits.length == 0) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bits.length; i++) {
            if (i > 0) sb.append(' ');
            sb.append(bits[i]);
        }
        return sb.toString();
    }

    /** Converts char[] bases → space-separated string: "+ x + x …" */
    public static String basesToString(char[] bases) {
        if (bases == null || bases.length == 0) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bases.length; i++) {
            if (i > 0) sb.append(' ');
            sb.append(bases[i]);
        }
        return sb.toString();
    }

    /** Converts int[] bits → compact binary: "01101011" */
    public static String bitsCompact(int[] bits) {
        if (bits == null || bits.length == 0) return "(empty)";
        StringBuilder sb = new StringBuilder();
        for (int b : bits) sb.append(b);
        return sb.toString();
    }

    /** Groups compact binary string into blocks of 4: "0110 1011 …" */
    public static String bitsGrouped(int[] bits) {
        if (bits == null || bits.length == 0) return "(empty)";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bits.length; i++) {
            if (i > 0 && i % 4 == 0) sb.append(' ');
            sb.append(bits[i]);
        }
        return sb.toString();
    }

    // ─────────────────────────────────────────────────────────────
    // QBER Formatting
    // ─────────────────────────────────────────────────────────────

    /** Formats 0.0–1.0 QBER value as "15.75%" */
    public static String formatQber(double qber) {
        return String.format("%.2f%%", qber * 100.0);
    }

    /**
     * Returns a risk label based on QBER value.
     * LOW → < 10%, MODERATE → < 20%, HIGH → ≥ 20%
     */
    public static String qberRisk(double qber) {
        if (qber < 0.10) return "LOW";
        if (qber < 0.20) return "MODERATE";
        return "HIGH";
    }

    // ─────────────────────────────────────────────────────────────
    // Array Utilities
    // ─────────────────────────────────────────────────────────────

    /** Returns number of positions where a[i] != b[i]. */
    public static int countMismatches(int[] a, int[] b) {
        if (a == null || b == null) return 0;
        int n = Math.min(a.length, b.length), diff = 0;
        for (int i = 0; i < n; i++) if (a[i] != b[i]) diff++;
        return diff;
    }
}