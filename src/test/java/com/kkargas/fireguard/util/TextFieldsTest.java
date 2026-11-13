package com.kkargas.fireguard.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class TextFieldsTest {

    @Test
    void requiredTextIsTrimmed() {
        assertEquals("Lobby", TextFields.required("  Lobby  ", "Name", 20));
    }

    @Test
    void requiredTextRejectsBlankAndTooLongValues() {
        assertThrows(IllegalArgumentException.class,
                () -> TextFields.required("  ", "Name", 20));
        assertThrows(IllegalArgumentException.class,
                () -> TextFields.required(null, "Name", 20));
        assertThrows(IllegalArgumentException.class,
                () -> TextFields.required("LongName", "Name", 4));
    }

    @Test
    void optionalTextNormalizesBlankAndTrimsOtherValues() {
        assertNull(TextFields.optional("   ", 10));
        assertNull(TextFields.optional(null, 10));
        assertEquals("Office", TextFields.optional(" Office ", 10));
    }

    @Test
    void optionalTextChecksLengthAfterTrimming() {
        assertEquals("abc", TextFields.optional(" abc ", 3));
        assertThrows(IllegalArgumentException.class,
                () -> TextFields.optional("abcd", 3));
    }
}
