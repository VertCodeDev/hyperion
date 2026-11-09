package dev.vertcode.hyperion.animation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import dev.vertcode.hyperion.blueprint.data.Animator;
import dev.vertcode.hyperion.blueprint.data.AnimatorType;
import dev.vertcode.hyperion.blueprint.data.Interpolation;
import dev.vertcode.hyperion.blueprint.data.Keyframe;
import dev.vertcode.hyperion.blueprint.data.KeyframeChannel;
import dev.vertcode.hyperion.blueprint.data.KeyframePoint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TimedEffectsTest {

    @Test
    void readsSoundsParticlesAndScriptsFromTheEffectsTimeline() {
        Animator effects = new Animator("effects", "Effects", AnimatorType.EFFECT, List.of(
                keyframe(KeyframeChannel.TIMELINE, 1f, Map.of("script", "  breath_damage  ")),
                keyframe(KeyframeChannel.SOUND, 0f, Map.of("effect", "entity.wolf.howl", "file", "")),
                keyframe(KeyframeChannel.PARTICLE, 0.5f, Map.of("effect", "hyperion:fire_breath", "locator", "mouth")),
                keyframe(KeyframeChannel.SOUND, 0.25f, Map.of("effect", " "))
        ));
        List<TimedEffect> found = new ArrayList<>();
        AnimationBaker.effects(effects, found);

        assertEquals(3, found.size());
        assertEquals(new TimedEffect(0, TimedEffect.Kind.SOUND, "entity.wolf.howl", null), found.get(0));
        assertEquals(new TimedEffect(10, TimedEffect.Kind.PARTICLE, "hyperion:fire_breath", "mouth"), found.get(1));
        assertEquals(20, found.get(2).tick());
        assertEquals("breath_damage", found.get(2).effect());
        assertNull(found.get(2).locator());
    }

    private static Keyframe keyframe(final KeyframeChannel channel, final float time, final Map<String, String> values) {
        return new Keyframe(channel, time, Interpolation.LINEAR, List.of(new KeyframePoint(values)), null);
    }
}
