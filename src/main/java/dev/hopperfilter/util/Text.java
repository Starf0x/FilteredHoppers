package dev.hopperfilter.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public final class Text {

    private static final LegacyComponentSerializer SERIALIZER =
            LegacyComponentSerializer.legacyAmpersand();

    private Text() {
    }

    public static Component colorize(String message) {
        return SERIALIZER.deserialize(message);
    }
}