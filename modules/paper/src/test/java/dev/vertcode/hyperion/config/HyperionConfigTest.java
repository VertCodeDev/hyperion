package dev.vertcode.hyperion.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vertcode.hyperion.api.model.behavior.builtin.BuiltinBehaviors;
import dev.vertcode.hyperion.api.model.hitbox.HitboxMode;

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
        assertEquals(2, models.interpolation());
        assertTrue(models.autoAnimations().enabled());
        assertEquals("walk", models.autoAnimations().walk());
        assertEquals("pig", models.spawnEntity());
    }

    @Test
    void readsAutoAnimationChoicesPerEntityType() {
        this.bundled.set("models.auto-animations.entities.minecraft:armor_stand", false);
        AutoAnimations auto = HyperionConfig.of(this.bundled, this.logger, ITEMS).models().autoAnimations();

        assertEquals(false, auto.entities().get("armor_stand"));
        assertTrue(auto.enabled());
    }

    @Test
    void readsTheBehaviorDefaults() {
        BehaviorSettings behaviors = HyperionConfig.of(this.bundled, this.logger, ITEMS).models().behaviors();

        assertTrue(behaviors.matches(BuiltinBehaviors.HEAD, "Head_Top"));
        assertTrue(behaviors.matches(BuiltinBehaviors.SEAT, "seat_left"));
        assertFalse(behaviors.matches(BuiltinBehaviors.HEAD, "h_head"));
        assertEquals(HitboxMode.BONES, behaviors.hitboxMode());
        assertTrue(behaviors.forwardDamage());
        assertTrue(behaviors.clickToRide());
        assertFalse(behaviors.steering().enabled());
        assertEquals(50f, behaviors.headMaxYaw());
    }

    @Test
    void readsOtherPrefixesAndSteeringPerEntityType() {
        this.bundled.set("behaviors.prefixes.head", List.of("head", " H_ "));
        this.bundled.set("behaviors.hitboxes.mode", "model");
        this.bundled.set("behaviors.seats.steering.mode", "flying");
        this.bundled.set("behaviors.seats.steering.entities.minecraft:horse", true);
        BehaviorSettings behaviors = HyperionConfig.of(this.bundled, this.logger, ITEMS).models().behaviors();

        assertTrue(behaviors.matches(BuiltinBehaviors.HEAD, "h_neck"));
        assertEquals(HitboxMode.MODEL, behaviors.hitboxMode());
        assertTrue(behaviors.steering().flying());
        assertEquals(true, behaviors.steering().entities().get("horse"));
    }

    @Test
    void fallsBackOnAnUnknownHitboxMode() {
        this.bundled.set("behaviors.hitboxes.mode", "spheres");

        assertEquals(HitboxMode.BONES, HyperionConfig.of(this.bundled, this.logger, ITEMS).models().behaviors().hitboxMode());
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
        assertEquals(2, models.interpolation());
    }

    @Test
    void acceptsAnotherBaseItem() {
        this.bundled.set("pack.base-item", "minecraft:leather_horse_armor");

        assertEquals("leather_horse_armor", HyperionConfig.of(this.bundled, this.logger, ITEMS).pack().baseItem());
    }

    @Test
    void readsTheDebugAndGallerySections() {
        HyperionConfig config = HyperionConfig.of(this.bundled, this.logger, ITEMS);

        assertEquals(new DebugSettings(4, true, 0.4f), config.debug());
        assertEquals(120, config.gallery().previewSeconds());
    }

    @Test
    void fallsBackOnDebugValuesItCannotUse() {
        this.bundled.set("debug.interval", 0);
        this.bundled.set("debug.axis-length", -1);
        this.bundled.set("gallery.preview-seconds", 0);
        HyperionConfig config = HyperionConfig.of(this.bundled, this.logger, ITEMS);

        assertEquals(4, config.debug().interval());
        assertEquals(0.4f, config.debug().axisLength());
        assertEquals(120, config.gallery().previewSeconds());
    }

    @Test
    void readsTheArmorSection() {
        assertEquals(new ArmorSettings(false, 32, 16), HyperionConfig.of(this.bundled, this.logger, ITEMS).armor());
    }

    @Test
    void fallsBackOnArmorValuesItCannotUse() {
        this.bundled.set("armor.shader", true);
        this.bundled.set("armor.render-distance", 0);
        this.bundled.set("armor.max-layers", 500);

        assertEquals(new ArmorSettings(true, 32, 16), HyperionConfig.of(this.bundled, this.logger, ITEMS).armor());
    }
}
