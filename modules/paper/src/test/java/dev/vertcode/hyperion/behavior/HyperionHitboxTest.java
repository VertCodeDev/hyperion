package dev.vertcode.hyperion.behavior;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import dev.vertcode.hyperion.api.model.hitbox.Hitbox;
import dev.vertcode.hyperion.blueprint.Blueprint;
import dev.vertcode.hyperion.blueprint.Fixtures;
import dev.vertcode.hyperion.blueprint.LoadReport;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HyperionHitboxTest {

    private static final float DELTA = 1.0e-5f;

    @Test
    void sizesAHitboxFromTheBonesOwnCubes() throws IOException {
        Blueprint blueprint = Fixtures.blueprint("groups_layout", new LoadReport("groups_layout.bbmodel"));

        // The head holds one 2 by 4 by 2 pixel cube, grown by half a pixel, on a pivot 8 pixels up.
        Hitbox head = HyperionHitbox.sizeOf(blueprint.bone("head").orElseThrow());
        assertEquals(3f / 16f, head.width(), DELTA);
        assertEquals(5f / 16f, head.height(), DELTA);
        assertEquals(0f, head.offset().x(), DELTA);
        assertEquals(-0.5f / 16f, head.offset().y(), DELTA);
        assertEquals(0f, head.offset().z(), DELTA);

        // The body's cubes reach 8 pixels wide and high, from its pivot at the origin.
        Hitbox body = HyperionHitbox.sizeOf(blueprint.bone("body").orElseThrow());
        assertEquals(0.5f, body.width(), DELTA);
        assertEquals(0.5f, body.height(), DELTA);
        assertEquals(0f, body.offset().y(), DELTA);
    }
}
