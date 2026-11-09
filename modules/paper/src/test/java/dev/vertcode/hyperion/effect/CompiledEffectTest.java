package dev.vertcode.hyperion.effect;

import java.util.List;

import net.kyori.adventure.key.Key;
import org.junit.jupiter.api.Test;

import dev.vertcode.hyperion.api.effect.Easing;
import dev.vertcode.hyperion.api.effect.Effect;
import dev.vertcode.hyperion.api.effect.EffectFrame;
import dev.vertcode.hyperion.api.effect.EffectPart;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class CompiledEffectTest {

    private static final float EPSILON = 1.0e-4f;

    private final Effect effect = Effect.builder(Key.key("test", "zap"), 20)
            .part(EffectPart.particle("ring")
                    .setting("particle", "end_rod")
                    .keyframe(4, frame -> frame.radius(0f))
                    .keyframe(14, Easing.EASE_IN, frame -> frame.radius(1f)))
            .part(EffectPart.sound("crack")
                    .setting("volume", 0.5f)
                    .keyframe(10, frame -> frame.sound("b"))
                    .keyframe(2, frame -> frame.sound("a")))
            .build();
    private final CompiledEffect compiled = new CompiledEffect(this.effect, type -> type.equals(EffectPart.PARTICLE));

    @Test
    void turnsKeyframesIntoStepsByTick() {
        List<CompiledEffect.Step> steps = this.compiled.steps();

        assertEquals(2, steps.size());
        assertEquals(2, steps.get(0).tick());
        assertEquals("a", steps.get(0).frame().string("sound", ""));
        assertEquals(0.5f, steps.get(0).frame().number("volume", 1f), EPSILON);
        assertEquals("b", steps.get(1).frame().string("sound", ""));
    }

    @Test
    void easesASampledPartBetweenItsKeyframes() {
        assertNull(this.compiled.state(0, 3));

        EffectFrame halfway = this.compiled.state(0, 9);
        assertNotNull(halfway);
        assertEquals(0.25f, halfway.number("radius", -1f), EPSILON);
        assertEquals("end_rod", halfway.string("particle", ""));

        EffectFrame after = this.compiled.state(0, 19);
        assertNotNull(after);
        assertEquals(1f, after.number("radius", -1f), EPSILON);
    }

    @Test
    void easesEveryWay() {
        for (Easing easing : Easing.values()) {
            assertEquals(0f, easing.apply(0f), EPSILON, easing.key());
            assertEquals(1f, easing.apply(1f), EPSILON, easing.key());
        }

        assertEquals(Easing.EASE_IN_OUT, Easing.fromKey("ease-in-out").orElseThrow());
    }
}
