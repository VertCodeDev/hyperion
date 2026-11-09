package dev.vertcode.hyperion.effect;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import net.kyori.adventure.key.Key;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import dev.vertcode.hyperion.api.effect.Easing;
import dev.vertcode.hyperion.api.effect.Effect;
import dev.vertcode.hyperion.api.effect.EffectPart;
import dev.vertcode.hyperion.api.effect.EffectViewers;
import dev.vertcode.hyperion.blueprint.IssueSeverity;
import dev.vertcode.hyperion.blueprint.LoadReport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EffectLoaderTest {

    private static final Set<Key> TYPES = Set.of(EffectPart.PARTICLE, EffectPart.SOUND, EffectPart.BURST, EffectPart.EVENT, EffectPart.COMMAND);
    private static final Key KEY = Key.key("hyperion", "breath");

    @Test
    void readsAnEffectWithItsPartsAndKeyframes() throws InvalidConfigurationException {
        LoadReport report = new LoadReport("test.yml");
        Effect effect = load("""
                breath:
                  duration: 40
                  loop: true
                  viewers: nearby:24
                  parameters: { color: "#FF5500" }
                  parts:
                    smoke:
                      type: particle
                      at: locator:mouth
                      shape: ring
                      color: "%color%"
                      keyframes:
                        8: { offset: [0, 1, 2], radius: 0.2 }
                        30: { radius: 1.2, easing: ease-out }
                    roar:
                      type: sound
                      keyframes:
                        0: { sound: entity.ender_dragon.growl, pitch: 0.8 }
                """, report).orElseThrow();

        assertTrue(report.issues().isEmpty());
        assertEquals(40, effect.duration());
        assertTrue(effect.loop());
        assertEquals(EffectViewers.nearby(24), effect.viewers());
        assertEquals("#FF5500", effect.parameters().get("color"));

        EffectPart smoke = effect.parts().getFirst();
        assertEquals(EffectPart.PARTICLE, smoke.type());
        assertEquals("locator:mouth", smoke.settings().string("at", ""));
        assertEquals(new Vector3f(0f, 1f, 2f), smoke.keyframes().getFirst().frame().vector("offset"));
        assertEquals(Easing.EASE_OUT, smoke.keyframes().get(1).easing());
        assertEquals(0.8f, effect.parts().get(1).keyframes().getFirst().frame().number("pitch", 1f));
    }

    @Test
    void skipsWhatItCannotUseAndWarns() throws InvalidConfigurationException {
        LoadReport report = new LoadReport("test.yml");
        Effect effect = load("""
                breath:
                  duration: 10
                  viewers: somewhere
                  parts:
                    good:
                      type: sound
                      keyframes:
                        0: { sound: block.note_block.bell, easing: wobble }
                        soon: { sound: block.note_block.bell }
                    strange:
                      type: myplugin:shake
                """, report).orElseThrow();

        assertEquals(1, effect.parts().size());
        assertEquals(1, effect.parts().getFirst().keyframes().size());
        assertEquals(EffectViewers.model(), effect.viewers());
        assertEquals(4, report.count(IssueSeverity.WARNING));
    }

    @Test
    void refusesAnEffectWithoutADuration() throws InvalidConfigurationException {
        LoadReport report = new LoadReport("test.yml");
        Optional<Effect> effect = load("""
                breath:
                  parts:
                    roar: { type: sound }
                """, report);

        assertFalse(effect.isPresent());
        assertTrue(report.hasErrors());
    }

    @Test
    void refusesCommandsInsideModels() throws InvalidConfigurationException {
        String yaml = """
                breath:
                  duration: 10
                  parts:
                    roar:
                      type: sound
                    reward:
                      type: command
                      keyframes:
                        0: { command: "give %player% diamond" }
                """;
        LoadReport fromFile = new LoadReport("test.yml");
        LoadReport fromModel = new LoadReport("model");

        assertEquals(2, load(yaml, fromFile, true).orElseThrow().parts().size());
        assertEquals(1, load(yaml, fromModel, false).orElseThrow().parts().size());
        assertEquals(1, fromModel.count(IssueSeverity.WARNING));
    }

    private static Optional<Effect> load(final String yaml, final LoadReport report) throws InvalidConfigurationException {
        return load(yaml, report, true);
    }

    private static Optional<Effect> load(
            final String yaml,
            final LoadReport report,
            final boolean commands
    ) throws InvalidConfigurationException {
        YamlConfiguration file = new YamlConfiguration();
        file.loadFromString(yaml);
        Map<String, Object> tree = EffectTrees.of(file);

        return EffectLoader.effect(KEY, Objects.requireNonNull(tree.get("breath")), report, TYPES::contains, commands);
    }
}
