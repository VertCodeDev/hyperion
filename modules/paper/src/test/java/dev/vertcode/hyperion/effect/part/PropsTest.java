package dev.vertcode.hyperion.effect.part;

import java.util.Map;

import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import dev.vertcode.hyperion.api.effect.EffectFrame;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PropsTest {

    private static final float EPSILON = 1.0e-4f;

    @Test
    void readsAScaleAsOneNumberOrOnePerAxis() {
        assertEquals(new Vector3f(2f), DisplayActor.scale(new EffectFrame(Map.of("scale", 2f))));
        assertEquals(new Vector3f(1f, 2f, 3f), DisplayActor.scale(new EffectFrame(Map.of("scale", new Vector3f(1f, 2f, 3f)))));
        assertEquals(new Vector3f(1f), DisplayActor.scale(EffectFrame.EMPTY));
    }

    @Test
    void turnsDegreesIntoARotation() {
        Vector3f forward = DisplayActor.euler(new Vector3f(0f, 90f, 0f)).transform(new Vector3f(0f, 0f, 1f));

        assertTrue(new Vector3f(1f, 0f, 0f).equals(forward, EPSILON));
        assertTrue(DisplayActor.euler(null).equals(DisplayActor.euler(new Vector3f()), EPSILON));
    }

    @Test
    void readsTextBackgrounds() {
        assertEquals(0x80FF0000, TextActor.argb("#80FF0000"));
        assertEquals(0xFF00FF00, TextActor.argb("00FF00"));
        assertEquals(0x40000000, TextActor.argb("nope"));
    }
}
