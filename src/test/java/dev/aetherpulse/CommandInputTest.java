package dev.aetherpulse;

import dev.aetherpulse.command.CommandInput;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class CommandInputTest {
    @Test void normalizesIDs() { assertEquals("crystal_1", CommandInput.id("Crystal_1")); }
    @ParameterizedTest @ValueSource(strings = {"", "has space", "../x", "a.b", "abcdefghijklmnopqrstuvwxyz1234567"})
    void rejectsUnsafeIDs(String id) { assertThrows(IllegalArgumentException.class, () -> CommandInput.id(id)); }
    @ParameterizedTest @ValueSource(strings = {"-1", "NaN", "1.5", "9223372036854775808", ""})
    void rejectsAmounts(String value) { assertThrows(IllegalArgumentException.class, () -> CommandInput.points(value)); }
    @Test void parsesExtremesAndStage() {
        assertEquals(Long.MAX_VALUE, CommandInput.points("9223372036854775807")); assertEquals(0, CommandInput.points("0"));
        assertEquals(5, CommandInput.stage("5"));
        assertThrows(IllegalArgumentException.class, () -> CommandInput.stage("6"));
        assertThrows(IllegalArgumentException.class, () -> CommandInput.stage("0"));
    }
}
