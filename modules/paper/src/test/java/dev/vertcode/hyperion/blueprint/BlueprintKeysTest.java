package dev.vertcode.hyperion.blueprint;

import net.kyori.adventure.key.Key;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlueprintKeysTest {

    @Test
    void usesThePathUnderTheHyperionNamespace() {
        LoadReport report = new LoadReport("bosses/dragon.bbmodel");

        assertEquals(Key.key("hyperion", "bosses/dragon"), BlueprintKeys.fromPath("bosses/dragon.bbmodel", report).orElseThrow());
        assertTrue(report.issues().isEmpty());
    }

    @Test
    void lowercasesAndReplacesSpacesWithAWarning() {
        LoadReport report = new LoadReport("Dragon Boss.bbmodel");

        assertEquals(Key.key("hyperion", "dragon_boss"), BlueprintKeys.fromPath("Dragon Boss.bbmodel", report).orElseThrow());
        assertEquals(1, report.count(IssueSeverity.WARNING));
        assertFalse(report.hasErrors());
    }

    @Test
    void refusesWhatAKeyCannotHold() {
        LoadReport report = new LoadReport("dragon+boss.bbmodel");

        assertTrue(BlueprintKeys.fromPath("dragon+boss.bbmodel", report).isEmpty());
        assertTrue(report.hasErrors());
    }
}
