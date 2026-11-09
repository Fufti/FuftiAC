package ru.fufti.fuftiac.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public final class FuftiACText {

    private static final MiniMessage MINI = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    private FuftiACText() {
    }

    public static Component parse(String text) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }
        if (text.indexOf('<') >= 0) {
            return MINI.deserialize(text);
        }
        return LEGACY.deserialize(text);
    }

    public static String bar(int vl, int max) {
        int size = Math.max(3, Math.min(8, max));
        int filled = (int) Math.round(size * (Math.min(vl, max) / (double) max));
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < size; i++) {
            builder.append(i < filled ? "<gradient:#38BDF8:#818CF8>▌</gradient>" : "<dark_gray>▌</dark_gray>");
        }
        return builder.toString();
    }
}
