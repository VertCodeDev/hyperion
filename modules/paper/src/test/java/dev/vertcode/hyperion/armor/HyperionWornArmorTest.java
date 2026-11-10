package dev.vertcode.hyperion.armor;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

import dev.vertcode.hyperion.api.armor.ArmorSlot;
import dev.vertcode.hyperion.api.version.MinecraftVersion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HyperionWornArmorTest {

    @Test
    void countsPiecesAndTellsAFullSet() {
        HyperionArmorSet set = ArmorFixtures.frostSet(MinecraftVersion.parse("1.21.4"));
        Map<ArmorSlot, HyperionArmorPiece> pieces = new EnumMap<>(ArmorSlot.class);
        pieces.put(ArmorSlot.HEAD, (HyperionArmorPiece) set.piece(ArmorSlot.HEAD).orElseThrow());
        pieces.put(ArmorSlot.CHEST, (HyperionArmorPiece) set.piece(ArmorSlot.CHEST).orElseThrow());
        HyperionWornArmor part = new HyperionWornArmor(UUID.randomUUID(), pieces, Map.<ArmorSlot, ItemStack>of());

        assertEquals(2, part.count(set));
        assertFalse(part.hasFullSet(set));
        assertEquals(Set.of(set), part.sets());

        pieces.put(ArmorSlot.FEET, (HyperionArmorPiece) set.piece(ArmorSlot.FEET).orElseThrow());
        HyperionWornArmor full = new HyperionWornArmor(UUID.randomUUID(), pieces, Map.<ArmorSlot, ItemStack>of());

        assertTrue(full.hasFullSet(set));
        assertTrue(full.piece(ArmorSlot.LEGS).isEmpty());
    }

    @Test
    void wearsNothingWhenEmpty() {
        HyperionArmorSet set = ArmorFixtures.frostSet(MinecraftVersion.parse("1.21.4"));
        HyperionWornArmor empty = HyperionWornArmor.empty(UUID.randomUUID());

        assertEquals(0, empty.count(set));
        assertTrue(empty.sets().isEmpty());
    }
}
