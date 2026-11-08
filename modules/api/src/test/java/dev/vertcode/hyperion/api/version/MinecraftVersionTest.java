package dev.vertcode.hyperion.api.version;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MinecraftVersionTest {

    @Test
    void readsEveryShapeOfVersion() {
        assertEquals(new MinecraftVersion(1, 21, 0), MinecraftVersion.parse("1.21"));
        assertEquals(new MinecraftVersion(1, 21, 4), MinecraftVersion.parse("1.21.4"));
        assertEquals(new MinecraftVersion(26, 3, 0), MinecraftVersion.parse("26.3"));
        assertEquals(new MinecraftVersion(1, 21, 9), MinecraftVersion.parse("1.21.9-rc1"));
        assertThrows(IllegalArgumentException.class, () -> MinecraftVersion.parse("snapshot"));
    }

    @Test
    void comparesLikeNumbers() {
        assertTrue(MinecraftVersion.parse("1.21.11").atLeast(MinecraftVersion.parse("1.21.4")));
        assertTrue(MinecraftVersion.parse("26.1").atLeast(MinecraftVersion.parse("1.21.11")));
        assertTrue(MinecraftVersion.parse("1.21.4").atLeast(MinecraftVersion.V1_21_4));
        assertFalse(MinecraftVersion.parse("1.21.3").atLeast(MinecraftVersion.V1_21_4));
        assertEquals("1.21.4", MinecraftVersion.V1_21_4.toString());
        assertEquals("1.21", MinecraftVersion.V1_21.toString());
    }

    @Test
    void featuresArriveWithTheirVersion() {
        assertFalse(Feature.ITEM_MODEL_DEFINITIONS.availableOn(MinecraftVersion.parse("1.21.3")));
        assertTrue(Feature.ITEM_MODEL_DEFINITIONS.availableOn(MinecraftVersion.parse("1.21.4")));
        assertTrue(Feature.ITEM_MODEL_DEFINITIONS.availableOn(MinecraftVersion.parse("26.3")));
        assertFalse(Feature.PLAIN_ATTRIBUTE_KEYS.availableOn(MinecraftVersion.V1_21));
        assertTrue(Feature.PLAIN_ATTRIBUTE_KEYS.availableOn(MinecraftVersion.V1_21_2));
    }
}
