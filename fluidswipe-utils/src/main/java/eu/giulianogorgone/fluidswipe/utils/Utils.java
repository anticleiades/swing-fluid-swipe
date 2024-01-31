/*
 * Copyright 2024 Giuliano Gorgone
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package eu.giulianogorgone.fluidswipe.utils;

import java.util.regex.Pattern;

/**
 * This class is not part of the public API.
 *
 * @author Giuliano Gorgone (anticleiades)
 */
public final class Utils {
    private static final Pattern VERSION_PATTERN = Pattern.compile("\\.");

    private Utils() {
        throw new AssertionError();
    }

    public static boolean hasFlag(final int value, final int flag) {
        return (value & flag) == flag;
    }

    /**
     * BNF for the version format:
     * <blockquote>
     * <pre>
     *    {@code
     *    Version            ::= <decNumber> "." <Version> | <decNumber>
     *    decNumber          ::= <decDigit> | <decDigit> <decNumber>
     *    decDigit           ::=  "0" | "1" | "2" | "3" | "4" | "5" | "6" | "7" | "8" | "9"
     *    }
     *     </pre>
     * </blockquote>
     * Examples of valid string: "10.15", "0", "13.6.3".
     * If versions have different length, the following properties hold:
     * given <code>A = "x.y.z"</code>, <code>B = "a.b.c.[.d.e...].n")</code>, <code>E = "x.y.z.[.0.0...].0"</code>
     * <ul>
     * <li>
     *  <code>compareOSVersionString(A, B) = compareOSVersionString(E, B)</code>
     * </li>
     * <li>
     *  <code>compareOSVersionString(B, A) = compareOSVersionString(B, E)</code>
     * </li>
     * </ul>
     * @param a version string
     * @param b version string
     * @return <code>0</code> if <code>a</code> and <code>b</code> represents
     * the same version, <code>-1</code> if <code>a</code> represents a version less than <code>b</code>,
     * <code>1</code>  if <code>a</code> represents a version greater than <code>b</code>.
     * @throws NullPointerException  if any argument is null
     * @throws NumberFormatException if any input does not respect the format
     */
    public static int compareOSVersionString(final String a,
                                             final String b) {
        int i = 0;
        int comp = 0;
        final String[] subversA = VERSION_PATTERN.split(a), subversB = VERSION_PATTERN.split(b);
        final int maxLen = Math.max(subversA.length, subversB.length);

        if (a.contains("+") || b.contains("+")) // Obey to the BNF. Other checks are delegated to Integer#parseUnsignedInt(String)
            throw new NumberFormatException("Version string must not contain sign");

        while (comp == 0 && i < maxLen) {
            comp = Integer.compare(getInt(subversA, i), getInt(subversB,i));
            i++;
        }
        return comp;
    }

    private static int getInt(final String[] a, final int idx) {
        return idx < a.length ? Integer.parseUnsignedInt(a[idx]) : 0;
    }
}
