package dev.vertcode.hyperion.animation;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.junit.jupiter.api.Test;

import dev.vertcode.hyperion.api.animation.LoopMode;
import dev.vertcode.hyperion.blueprint.LoadReport;
import dev.vertcode.hyperion.blueprint.data.Animation;
import dev.vertcode.hyperion.blueprint.data.Animator;
import dev.vertcode.hyperion.blueprint.data.AnimatorType;
import dev.vertcode.hyperion.blueprint.data.Interpolation;
import dev.vertcode.hyperion.blueprint.data.Keyframe;
import dev.vertcode.hyperion.blueprint.data.KeyframeChannel;
import dev.vertcode.hyperion.blueprint.data.KeyframePoint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnimationBakerTest {

    private static final float EPSILON = 1.0e-4f;

    @Test
    void samplesEveryTickInTheBonesSpace() {
        Animator arm = new Animator("group-1", "arm", AnimatorType.BONE, List.of(
                keyframe(KeyframeChannel.ROTATION, 0f, "0", "0", "0"),
                keyframe(KeyframeChannel.ROTATION, 1f, "20", "40", "60"),
                keyframe(KeyframeChannel.POSITION, 0f, "16", "16", "16")
        ));
        Animation animation = animation("wave", 1f, dev.vertcode.hyperion.blueprint.data.LoopMode.LOOP, List.of(arm));

        BakedAnimation baked = AnimationBaker.bake("wave", animation, Map.of("group-1", "arm"), new LoadReport("test"));
        BoneTrack track = Objects.requireNonNull(baked.tracks().get("arm"));
        float[] rotation = Objects.requireNonNull(track.rotation());
        float[] position = Objects.requireNonNull(track.position());

        assertEquals(LoopMode.LOOP, baked.loop());
        assertEquals(20f, baked.length(), EPSILON);
        assertEquals(21 * 3, rotation.length);
        assertEquals(-10f, rotation[10 * 3], EPSILON);
        assertEquals(-20f, rotation[10 * 3 + 1], EPSILON);
        assertEquals(30f, rotation[10 * 3 + 2], EPSILON);
        assertEquals(-1f, position[0], EPSILON);
        assertEquals(1f, position[1], EPSILON);
        assertNull(track.scale());
    }

    @Test
    void worksOutMolangAtEveryTick() {
        Animator spin = new Animator("group-1", "head", AnimatorType.BONE, List.of(
                keyframe(KeyframeChannel.ROTATION, 0f, "0", "query.anim_time * 100", "0")
        ));
        Animation animation = animation("spin", 1f, dev.vertcode.hyperion.blueprint.data.LoopMode.ONCE, List.of(spin));

        BakedAnimation baked = AnimationBaker.bake("spin", animation, Map.of("group-1", "head"), new LoadReport("test"));
        float[] rotation = Objects.requireNonNull(Objects.requireNonNull(baked.tracks().get("head")).rotation());

        assertEquals(-50f, rotation[10 * 3 + 1], EPSILON);
    }

    @Test
    void warnsAboutGroupsAndMolangItCannotUse() {
        Animator ghost = new Animator("missing", "ghost", AnimatorType.BONE, List.of(
                keyframe(KeyframeChannel.ROTATION, 0f, "0", "0", "0")
        ));
        Animator odd = new Animator("group-1", "arm", AnimatorType.BONE, List.of(
                keyframe(KeyframeChannel.POSITION, 0f, "query.ground_speed", "0", "0")
        ));
        LoadReport report = new LoadReport("test");

        AnimationBaker.bake("walk", animation("walk", 1f, dev.vertcode.hyperion.blueprint.data.LoopMode.LOOP, List.of(ghost, odd)),
                Map.of("group-1", "arm"), report);

        assertEquals(2, report.issues().size());
        assertTrue(report.issues().getFirst().describe().contains("ghost"));
    }

    private static Animation animation(
            final String name,
            final float length,
            final dev.vertcode.hyperion.blueprint.data.LoopMode loop,
            final List<Animator> animators
    ) {
        return new Animation(name, loop, length, false, "0", "0", animators);
    }

    private static Keyframe keyframe(
            final KeyframeChannel channel,
            final float time,
            final String x,
            final String y,
            final String z
    ) {
        return new Keyframe(channel, time, Interpolation.LINEAR, List.of(new KeyframePoint(Map.of("x", x, "y", y, "z", z))), null);
    }
}
