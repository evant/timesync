package me.tatarka.timesync.lib;

class MathUtil {
    /**
     * Divides 2 longs, but takes the ceiling instead of rounding towards zero.
     *
     * Implementation taken from: <a href="http://ericlippert.com/2013/01/28/integer-division-that-rounds-up/">
     * http://ericlippert.com/2013/01/28/integer-division-that-rounds-up/</a>
     *
     * @param a the numerator
     * @param b the denominator
     * @return ceil[a/b]
     * @throws java.lang.ArithmeticException if b is zero.
     */
    public static long divCeil(long a, long b) {
        if (b == 0) throw new ArithmeticException("divide by zero");
        if (b == -1 && a == Long.MIN_VALUE) return Long.MIN_VALUE;
        long roundedTowardsZero = a / b;
        boolean dividedEvenly = (a % b) == 0;
        if (dividedEvenly) {
            return roundedTowardsZero;
        } else {
            boolean roundedDown = (a > 0) == (b > 0);
            return roundedDown ? roundedTowardsZero + 1 : roundedTowardsZero;
        }
    }

    /**
     * For some pseudo-random uniformly distributed long seed, returns a pseudo-random uniformly
     * distributed long between lower and upper (both inclusive). A good source of the seed
     * would be {@link java.util.Random#nextLong()}.
     *
     * @param seed  the seed value
     * @param lower the lower bounds
     * @param upper the upper bounds
     * @return the new pseudo-random value
     */
    public static long randomInRange(long seed, long lower, long upper) {
        if (lower == upper) return upper; // Only valid value!
        long span = upper - lower;
        if (span < 0) {
            throw new IllegalArgumentException("upper must be greater than lower !(" + upper + ">" + lower + ")");
        }

        // Convert [-Long.MIN_VALUE,Long.MAX_VALUE] to [0, span]
        // source: http://stackoverflow.com/a/2546186 which was in turn taken from the javadoc for
        // Random.nextInt(n).
        long bits, val;
        do {
            bits = (seed << 1) >>> 1;
            val = bits % span;
        } while (bits - val + span < 0L);

        return val + lower;
    }
}
