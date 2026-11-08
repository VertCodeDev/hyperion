package dev.vertcode.hyperion.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HyperionConfigTest {

    private static final Predicate<String> ITEMS = Set.of("paper", "leather_horse_armor")::contains;

    private YamlConfiguration bundled;
    private Logger logger;

    @BeforeEach
    void read() throws IOException {
        try (InputStream stream = Objects.requireNonNull(getClass().getResourceAsStream("/" + HyperionConfig.FILE));
             Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            this.bundled = YamlConfiguration.loadConfiguration(reader);
        }

        this.logger = Logger.getAnonymousLogger();
        this.logger.setLevel(Level.OFF);
    }

    @Test
    void readsTheDefaults() {
        PackSettings pack = HyperionConfig.of(this.bundled, this.logger, ITEMS).pack();

        assertEquals(PackHosting.SELF_HOST, pack.hosting());
        assertEquals(8163, pack.port());
        assertEquals("", pack.address());
        assertTrue(pack.sendOnJoin());
        assertFalse(pack.required());
        assertEquals("paper", pack.baseItem());
        assertEquals(10_000, pack.customModelDataStart());

        ModelSettings models = HyperionConfig.of(this.bundled, this.logger, ITEMS).models();
        assertEquals(48, models.viewDistance());
        assertEquals(3, models.interpolation());
    }

    @Test
    void readsEveryHostingChoice() {
        this.bundled.set("pack.hosting", "url");
        assertEquals(PackHosting.URL, HyperionConfig.of(this.bundled, this.logger, ITEMS).pack().hosting());

        this.bundled.set("pack.hosting", "OFF");
        assertEquals(PackHosting.OFF, HyperionConfig.of(this.bundled, this.logger, ITEMS).pack().hosting());
    }

    @Test
    void fallsBackOnValuesItCannotUse() {
        this.bundled.set("pack.hosting", "dropbox");
        this.bundled.set("pack.self-host.port", 0);
        this.bundled.set("pack.base-item", "not_an_item");
        PackSettings pack = HyperionConfig.of(this.bundled, this.logger, ITEMS).pack();

        assertEquals(PackHosting.SELF_HOST, pack.hosting());
        assertEquals(8163, pack.port());
        assertEquals("paper", pack.baseItem());
    }

    @Test
    void fallsBackOnModelValuesItCannotUse() {
        this.bundled.set("models.view-distance", 0);
        this.bundled.set("models.interpolation", -1);
        ModelSettings models = HyperionConfig.of(this.bundled, this.logger, ITEMS).models();

        assertEquals(48, models.viewDistance());
        assertEquals(3, models.interpolation());
    }

    @Test
    void acceptsAnotherBaseItem() {
        this.bundled.set("pack.base-item", "minecraft:leather_horse_armor");

        assertEquals("leather_horse_armor", HyperionConfig.of(this.bundled, this.logger, ITEMS).pack().baseItem());
    }
}
