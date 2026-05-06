package com.leetcode.solution.dynamicprogramming;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DecodeWaysTest {

    @Test
    void testNumDecodingsExample() {
        DecodeWays.Solution solution = new DecodeWays.Solution();
        assertEquals(2, solution.numDecodings("11106"));
    }

    @Test
    void testNumDecodingsLeadingZero() {
        DecodeWays.Solution solution = new DecodeWays.Solution();
        assertEquals(0, solution.numDecodings("0"));
    }

    @Test
    void testNumDecodingsDoubleDigit() {
        DecodeWays.Solution solution = new DecodeWays.Solution();
        assertEquals(2, solution.numDecodings("26"));
    }

    @Test
    void testNumDecodingsWithTen() {
        DecodeWays.Solution solution = new DecodeWays.Solution();
        assertEquals(1, solution.numDecodings("10"));
    }

    @Test
    void testNumDecodingsEmptyString() {
        DecodeWays.Solution solution = new DecodeWays.Solution();
        assertEquals(0, solution.numDecodings(""));
    }

    @Test
    void testNumDecodingsNullInput() {
        DecodeWays.Solution solution = new DecodeWays.Solution();
        assertEquals(0, solution.numDecodings(null));
    }
}
