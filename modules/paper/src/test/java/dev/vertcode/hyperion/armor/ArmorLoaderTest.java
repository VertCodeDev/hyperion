package dev.vertcode.hyperion.armor;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import dev.vertcode.hyperion.api.armor.ArmorPiece;
import dev.vertcode.hyperion.api.armor.ArmorSlot;
import dev.vertcode.hyperion.api.armor.ArmorStats;
import dev.vertcode.hyperion.api.version.MinecraftVersion;
import dev.vertcode.hyperion.blueprint.IssueSeverity;
import dev.vertcode.hyperion.blueprint.LoadReport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArmorLoaderTest {

    private static final ArmorItems ITEMS = new ArmorItems(MinecraftVersion.parse("1.21.4"), "paper");

    @Test
    void makesAPieceForEverySlotTheModelHasBonesFor() {
        HyperionArmorSet set = ArmorFixtures.set(ArmorFixtures.frost(), "", ITEMS);

        assertEquals("hyperion:frost", set.key().asString());
        assertEquals("Frost", set.displayName());
        assertEquals(List.of(ArmorSlot.HEAD, ArmorSlot.CHEST, ArmorSlot.FEET), set.pieces().stream().map(ArmorPiece::slot).toList());
        assertTrue(set.piece(ArmorSlot.LEGS).isEmpty());
        assertEquals(List.of(ArmorBone.CHESTPLATE, ArmorBone.RIGHT_ARM), set.bones(ArmorSlot.CHEST));
    }

    @Test
    void takesTheBaseArmorsStatsWithoutSettings() {
        HyperionArmorSet set = ArmorFixtures.set(ArmorFixtures.frost(), "", ITEMS);

        assertEquals(ArmorMaterial.NETHERITE, set.material());
        assertEquals(new ArmorStats(8, 3, 0.1), set.piece(ArmorSlot.CHEST).orElseThrow().stats());
    }

    @Test
    void readsStatsNameAndDataFromTheSettings() {
        String yaml = """
                name: "<aqua>Frost King"
                base: diamond
                armor: [1, 2, 3, 4]
                toughness: 5
                dyeable: true
                data:
                  element: ice
                  bonus-damage: 1.5
                  nested:
                    level: 3
                """;
        HyperionArmorSet set = ArmorFixtures.set(ArmorFixtures.frost(), yaml, ITEMS);

        assertEquals("<aqua>Frost King", set.displayName());
        assertEquals(ArmorMaterial.DIAMOND, set.material());
        assertTrue(set.dyeable());
        assertEquals(new ArmorStats(2, 5, 0), set.piece(ArmorSlot.CHEST).orElseThrow().stats());
        assertEquals(new ArmorStats(4, 5, 0), set.piece(ArmorSlot.FEET).orElseThrow().stats());
        assertEquals("ice", set.data().getString("element", ""));
        assertEquals(1.5, set.data().getDouble("bonus-damage", 0));
        assertTrue(set.data().get("nested").orElseThrow() instanceof Map<?, ?>);
    }

    @Test
    void warnsAboutABoneThatPivotsAwayFromItsLimb() {
        ArmorFixtures model = new ArmorFixtures();
        int texture = model.texture("frost", 4, 4, 0xFFFFFFFF);
        model.bone("right_arm", new float[] {0, 22, 0}, model.cube(new float[] {4, 12, -2}, new float[] {8, 24, 2}, texture, 0f));
        LoadReport report = new LoadReport("frost.bbmodel");
        Optional<HyperionArmorSet> set = ArmorLoader.read("frost.bbmodel", model.build(), new YamlConfiguration(), ITEMS, report);

        assertTrue(set.isPresent());
        assertEquals(1, report.count(IssueSeverity.WARNING));
        assertTrue(report.issues().get(0).message().contains("right arm pivots at [5, 22, 0]"));
    }

    @Test
    void refusesAModelWithoutArmorBones() {
        ArmorFixtures model = new ArmorFixtures();
        int texture = model.texture("frost", 4, 4, 0xFFFFFFFF);
        model.bone("body", new float[] {0, 24, 0}, model.cube(new float[] {0, 0, 0}, new float[] {1, 1, 1}, texture, 0f));
        LoadReport report = new LoadReport("frost.bbmodel");

        assertTrue(ArmorLoader.read("frost.bbmodel", model.build(), null, ITEMS, report).isEmpty());
        assertTrue(report.hasErrors());
    }

    @Test
    void makesANameFromTheKey() {
        assertEquals("Ice King", ArmorLoader.title("sets/ice_king"));
        assertEquals("Frost", ArmorLoader.title("frost"));
    }
}
