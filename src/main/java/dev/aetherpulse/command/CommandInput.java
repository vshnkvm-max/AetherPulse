package dev.aetherpulse.command;

import java.util.Locale;

/** Pure argument validation shared by commands and tests. */
public final class CommandInput {
    private CommandInput() { }
    public static String id(String input) {
        if (!input.matches("[a-zA-Z0-9_-]{1,32}")) throw new IllegalArgumentException("ID: 1–32 letters, digits, _ or -");
        return input.toLowerCase(Locale.ROOT);
    }
    public static long points(String input) {
        try {
            long n = Long.parseLong(input);
            if (n < 0) throw new NumberFormatException();
            return n;
        } catch (NumberFormatException e) { throw new IllegalArgumentException("Amount must be an integer from 0 to " + Long.MAX_VALUE); }
    }
    public static int stage(String input) {
        long n = points(input);
        if (n < 1 || n > 5) throw new IllegalArgumentException("Stage must be 1–5");
        return (int) n;
    }
}
