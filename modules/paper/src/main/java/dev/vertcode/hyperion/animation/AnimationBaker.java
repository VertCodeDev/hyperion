package dev.vertcode.hyperion.animation;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.jspecify.annotations.Nullable;

import dev.vertcode.hyperion.animation.molang.CompiledMolang;
import dev.vertcode.hyperion.animation.molang.Molang;
import dev.vertcode.hyperion.animation.molang.MolangContext;
import dev.vertcode.hyperion.animation.molang.MolangException;
import dev.vertcode.hyperion.animation.molang.MolangExpression;
import dev.vertcode.hyperion.api.animation.LoopMode;
import dev.vertcode.hyperion.blueprint.LoadReport;
import dev.vertcode.hyperion.blueprint.data.Animation;
import dev.vertcode.hyperion.blueprint.data.Animator;
import dev.vertcode.hyperion.blueprint.data.AnimatorType;
import dev.vertcode.hyperion.blueprint.data.Keyframe;
import dev.vertcode.hyperion.blueprint.data.KeyframeChannel;
import dev.vertcode.hyperion.blueprint.data.KeyframePoint;

/**
 * Bake Blockbench animations into a sample per tick for each bone.
 *
 * <p>Blockbench stores an animation's offsets in pixels with X mirrored, and its angles with X
 * and Y mirrored, against its own group space; baking turns both into the bone's space, in
 * blocks and degrees, so playing only reads numbers. Molang a keyframe holds is worked out at
 * every tick; what Hyperion cannot read is a warning and reads as 0.
 */
public final class AnimationBaker {

    /**
     * The ticks in one second, the rate animations are sampled at.
     */
    public static final int TICKS_PER_SECOND = 20;

    private static final float PIXELS_PER_BLOCK = 16f;
    private static final String[] AXES = {"x", "y", "z"};
    private static final String NO_BONE = "animates '%s', which is not a bone of this model";
    private static final String UNSUPPORTED = "uses %s in '%s', which Hyperion does not know, so it reads as 0";
    private static final String UNREADABLE = "has Molang Hyperion cannot read, so it reads as 0: %s";

    private final String name;
    private final LoadReport report;
    private final Map<String, MolangExpression> compiled = new HashMap<>();
    private final MolangContext context;

    /**
     * Prepare to bake one animation.
     *
     * @param name the animation's unique name
     * @param report where problems are noted
     */
    private AnimationBaker(final String name, final LoadReport report) {
        this.name = name;
        this.report = report;
        this.context = new MolangContext(new Random(name.hashCode()));
    }

    /**
     * Bake an animation.
     *
     * @param name the animation's unique name in the blueprint
     * @param animation the animation as read
     * @param bones the bone names by the id of the group Blockbench animates
     * @param report where problems are noted
     * @return the baked animation
     */
    public static BakedAnimation bake(
            final String name,
            final Animation animation,
            final Map<String, String> bones,
            final LoadReport report
    ) {
        return new AnimationBaker(name, report).bake(animation, bones);
    }

    private BakedAnimation bake(final Animation animation, final Map<String, String> bones) {
        int length = Math.max(0, Math.round(animation.length() * TICKS_PER_SECOND));
        Map<String, BoneTrack> tracks = new LinkedHashMap<>();
        for (Animator animator : animation.animators()) {
            if (animator.type() != AnimatorType.BONE || animator.keyframes().isEmpty()) {
                continue;
            }

            String bone = bones.get(animator.target());
            if (bone == null) {
                this.warn(String.format(NO_BONE, animator.name()));
                continue;
            }

            tracks.put(bone, this.track(animator, length));
        }

        return new BakedAnimation(
                this.name,
                loop(animation.loop()),
                length,
                animation.override(),
                this.delay(animation.startDelay()),
                this.delay(animation.loopDelay()),
                tracks
        );
    }

    /**
     * Bake one bone's keyframes.
     *
     * @param animator the bone's animator
     * @param length the animation's length in ticks
     * @return the track
     */
    private BoneTrack track(final Animator animator, final int length) {
        Map<KeyframeChannel, List<CurveKey>> channels = new EnumMap<>(KeyframeChannel.class);
        for (Keyframe keyframe : animator.keyframes()) {
            if (keyframe.channel().transform()) {
                String rest = keyframe.channel() == KeyframeChannel.SCALE ? "1" : "0";
                channels.computeIfAbsent(keyframe.channel(), channel -> new ArrayList<>()).add(this.key(keyframe, rest));
            }
        }

        return new BoneTrack(
                this.sample(channels.get(KeyframeChannel.POSITION), length, KeyframeChannel.POSITION),
                this.sample(channels.get(KeyframeChannel.ROTATION), length, KeyframeChannel.ROTATION),
                this.sample(channels.get(KeyframeChannel.SCALE), length, KeyframeChannel.SCALE)
        );
    }

    /**
     * Sample one channel at every tick, turned into the bone's space.
     *
     * @param keys the channel's keyframes, or null when it has none
     * @param length the animation's length in ticks
     * @param channel the channel
     * @return three floats per tick, or null without keyframes
     */
    private float @Nullable [] sample(final @Nullable List<CurveKey> keys, final int length, final KeyframeChannel channel) {
        if (keys == null || keys.isEmpty()) {
            return null;
        }

        KeyframeCurve curve = new KeyframeCurve(keys);
        float[] samples = new float[(length + 1) * 3];
        for (int tick = 0; tick <= length; tick++) {
            float seconds = (float) tick / TICKS_PER_SECOND;
            this.context.animTime(seconds);
            curve.value(seconds, this.context, samples, tick * 3);
            convert(channel, samples, tick * 3);
        }

        return samples;
    }

    /**
     * Turn Blockbench's values into the bone's space: offsets from pixels to blocks with X
     * mirrored, angles with X and Y mirrored.
     *
     * @param channel the channel
     * @param samples the samples
     * @param at where the three values start
     */
    static void convert(final KeyframeChannel channel, final float[] samples, final int at) {
        switch (channel) {
            case POSITION -> {
                samples[at] = -samples[at] / PIXELS_PER_BLOCK;
                samples[at + 1] = samples[at + 1] / PIXELS_PER_BLOCK;
                samples[at + 2] = samples[at + 2] / PIXELS_PER_BLOCK;
            }
            case ROTATION -> {
                samples[at] = -samples[at];
                samples[at + 1] = -samples[at + 1];
            }
            default -> {
                // Scale is the same in both spaces.
            }
        }
    }

    /**
     * Compile one keyframe.
     *
     * @param keyframe the keyframe
     * @param rest the value of an axis it leaves out
     * @return the compiled keyframe
     */
    private CurveKey key(final Keyframe keyframe, final String rest) {
        List<KeyframePoint> points = keyframe.points();
        MolangExpression[] arriving = this.point(points.isEmpty() ? null : points.getFirst(), rest);
        MolangExpression[] leaving = points.size() > 1 ? this.point(points.get(1), rest) : arriving;

        return new CurveKey(keyframe.time(), keyframe.interpolation(), arriving, leaving, keyframe.bezier());
    }

    /**
     * Compile the three axes of one value of a keyframe.
     *
     * @param point the value, or null for none
     * @param rest the value of an axis it leaves out
     * @return the three compiled axes
     */
    private MolangExpression[] point(final @Nullable KeyframePoint point, final String rest) {
        MolangExpression[] axes = new MolangExpression[3];
        for (int axis = 0; axis < 3; axis++) {
            String text = point == null ? rest : point.value(AXES[axis], rest);
            axes[axis] = this.compile(text.isBlank() ? rest : text);
        }

        return axes;
    }

    /**
     * Compile a Molang text once per animation, warning about what it cannot read.
     *
     * @param text the text
     * @return the expression
     */
    private MolangExpression compile(final String text) {
        MolangExpression cached = this.compiled.get(text);
        if (cached != null) {
            return cached;
        }

        MolangExpression expression;
        try {
            CompiledMolang result = Molang.compile(text);
            for (String unknown : result.unsupported()) {
                this.warn(String.format(UNSUPPORTED, unknown, text));
            }

            if (result.constant()) {
                float value = result.expression().evaluate(this.context);
                expression = context -> value;
            } else {
                expression = result.expression();
            }
        } catch (MolangException exception) {
            this.warn(String.format(UNREADABLE, exception.getMessage()));
            expression = context -> 0f;
        }

        this.compiled.put(text, expression);

        return expression;
    }

    /**
     * Work out a delay, which Blockbench saves as Molang in seconds.
     *
     * @param text the delay
     * @return the delay in ticks, 0 or more
     */
    private int delay(final String text) {
        this.context.animTime(0f);
        float seconds = this.compile(text).evaluate(this.context);

        return Math.max(0, Math.round(seconds * TICKS_PER_SECOND));
    }

    /**
     * Note a problem with this animation.
     *
     * @param message the problem
     */
    private void warn(final String message) {
        this.report.warn("animation '" + this.name + "'", message);
    }

    /**
     * Turn the file's loop mode into the api's.
     *
     * @param loop the loop mode as read
     * @return the api's loop mode
     */
    // The file's LoopMode is qualified: the api's, which this class returns, has the same name.
    private static LoopMode loop(final dev.vertcode.hyperion.blueprint.data.LoopMode loop) {
        return switch (loop) {
            case ONCE -> LoopMode.ONCE;
            case LOOP -> LoopMode.LOOP;
            case HOLD -> LoopMode.HOLD;
        };
    }
}
