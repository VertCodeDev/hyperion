package dev.vertcode.hyperion.armor;

import java.util.Map;

import org.bukkit.Color;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import dev.vertcode.hyperion.api.armor.ArmorSlot;
import dev.vertcode.hyperion.api.version.MinecraftVersion;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ArmorItemsTest {

    private static final String SOUND = "equip_sound:\"minecraft:item.armor.equip_netherite\"";

    @Test
    void namesTheItemModelAndEquipmentAssetFrom1214() {
        assertEquals(
                "minecraft:netherite_chestplate[minecraft:item_model=\"hyperion:armor/frost/chestplate\","
                        + "minecraft:equippable={slot:\"chest\",asset_id:\"hyperion:frost\"," + SOUND + "}]",
                describe("1.21.4", ArmorSlot.CHEST, null)
        );
    }

    @Test
    void wearsTheHelmetWithoutAnAssetSoItsModelShowsOnTheHead() {
        assertEquals(
                "minecraft:netherite_helmet[minecraft:item_model=\"hyperion:armor/frost/helmet\","
                        + "minecraft:equippable={slot:\"head\"," + SOUND + "}]",
                describe("26.3", ArmorSlot.HEAD, null)
        );
    }

    @Test
    void namesTheAssetModelBefore1214() {
        assertEquals(
                "minecraft:netherite_boots[minecraft:equippable={slot:\"feet\",model:\"hyperion:frost\"," + SOUND + "}]",
                describe("1.21.2", ArmorSlot.FEET, null)
        );
    }

    @Test
    void picksTheIconByCustomModelDataBefore1214() {
        MinecraftVersion version = MinecraftVersion.parse("1.21.2");
        ArmorItems items = new ArmorItems(version, "paper");
        HyperionArmorSet set = ArmorFixtures.set(ArmorFixtures.frost(), "", items);
        ArmorPackInfo info = new ArmorPackInfo(Map.of(set.key(), Map.of(ArmorSlot.FEET, 10_004)), Map.of());
        HyperionArmorPiece piece = (HyperionArmorPiece) set.piece(ArmorSlot.FEET).orElseThrow();

        assertEquals(
                "minecraft:netherite_boots[minecraft:custom_model_data=10004,minecraft:equippable={slot:\"feet\","
                        + "model:\"hyperion:frost\"," + SOUND + "}]",
                items.describe(piece, info, null)
        );
    }

    @Test
    void dyesThePieceWhenAskedTo() {
        assertEquals(
                "minecraft:netherite_boots[minecraft:equippable={slot:\"feet\",model:\"hyperion:frost\"," + SOUND + "},"
                        + "minecraft:dyed_color=16711680]",
                describe("1.21.2", ArmorSlot.FEET, Color.RED)
        );
    }

    @Test
    void makesLeatherInTheSetsColorOn1211() {
        assertEquals("minecraft:leather_chestplate[minecraft:dyed_color=" + 0x074859 + "]", describe("1.21.1", ArmorSlot.CHEST, null));
        assertEquals("minecraft:paper[minecraft:max_stack_size=1]", describe("1.21.1", ArmorSlot.HEAD, null));
    }

    @Test
    void writesTheTagAPieceIsKnownBy() {
        HyperionArmorSet set = ArmorFixtures.frostSet(MinecraftVersion.parse("1.21.4"));

        assertEquals("hyperion:frost|CHEST", ArmorItems.tag(set.key(), ArmorSlot.CHEST));
    }

    /**
     * Describe a piece of the test set.
     *
     * @param version the server's version
     * @param slot the piece
     * @param color the dye, or null
     * @return the item in Minecraft's syntax
     */
    private static String describe(final String version, final ArmorSlot slot, final @Nullable Color color) {
        MinecraftVersion parsed = MinecraftVersion.parse(version);
        ArmorItems items = new ArmorItems(parsed, "paper");
        HyperionArmorSet set = ArmorFixtures.set(ArmorFixtures.frost(), "", items);
        ArmorPackInfo info = new ArmorPackInfo(Map.of(), Map.of(set.key(), 7));
        HyperionArmorPiece piece = (HyperionArmorPiece) set.piece(slot).orElseThrow();

        return items.describe(piece, info, color);
    }
}
