package dev.vertcode.hyperion.armor;

import java.util.Map;

import org.junit.jupiter.api.Test;

import dev.vertcode.hyperion.api.armor.ArmorData;
import dev.vertcode.hyperion.api.armor.ArmorSlot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArmorDataTest {

    @Test
    void readsValuesAsTheTypeAskedFor() {
        ArmorData data = new ArmorData(Map.of("damage", 1.5, "level", "3", "active", "true", "element", "ice"));

        assertEquals(1.5, data.getDouble("damage", 0));
        assertEquals(3, data.getInt("level", 0));
        assertTrue(data.getBoolean("active", false));
        assertEquals("ice", data.getString("element", ""));
    }

    @Test
    void fallsBackWhenAValueIsMissingOrWrong() {
        ArmorData data = new ArmorData(Map.of("element", "ice"));

        assertEquals(2.0, data.getDouble("element", 2.0));
        assertEquals(4, data.getInt("missing", 4));
        assertFalse(data.getBoolean("element", false));
        assertTrue(ArmorData.empty().keys().isEmpty());
    }

    @Test
    void findsSlotsByPieceOrSlotName() {
        assertEquals(ArmorSlot.FEET, ArmorSlot.fromName("Boots").orElseThrow());
        assertEquals(ArmorSlot.FEET, ArmorSlot.fromName("feet").orElseThrow());
        assertTrue(ArmorSlot.fromName("gloves").isEmpty());
    }
}
