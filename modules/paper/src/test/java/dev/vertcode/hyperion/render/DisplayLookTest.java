package dev.vertcode.hyperion.render;

import org.bukkit.Color;
import org.bukkit.entity.Display;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DisplayLookTest {

    private static final DisplayLook PLAIN = new DisplayLook(true, null, false, false, null, Display.Billboard.FIXED, null);

    @Test
    void tellsItemChangesFromStyleChanges() {
        DisplayLook tinted = new DisplayLook(true, Color.RED, false, false, null, Display.Billboard.FIXED, null);
        DisplayLook glowing = new DisplayLook(true, null, false, true, Color.AQUA, Display.Billboard.FIXED, null);

        assertTrue(PLAIN.itemDiffers(tinted));
        assertFalse(PLAIN.styleDiffers(tinted));
        assertTrue(PLAIN.styleDiffers(glowing));
        assertFalse(PLAIN.itemDiffers(glowing));
    }
}
