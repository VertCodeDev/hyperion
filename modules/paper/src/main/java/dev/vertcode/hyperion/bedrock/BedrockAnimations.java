package dev.vertcode.hyperion.bedrock;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.jspecify.annotations.Nullable;

import dev.vertcode.hyperion.animation.AnimationBaker;
import dev.vertcode.hyperion.animation.BakedAnimation;
import dev.vertcode.hyperion.animation.BoneTrack;
import dev.vertcode.hyperion.blueprint.Blueprint;

/**
 * Write a model's animations as Bedrock animations, from the samples Hyperion baked.
 *
 * <p>The samples are already worked out, Molang and curves included, so Bedrock plays exactly what
 * Java players see. They are turned back into Bedrock's space, offsets in pixels with X mirrored
 * and angles with X and Y mirrored, and a sample a straight line through its neighbours already
 * gives is left out. Each animation's time runs at the speed the {@code hyperion:speed} property
 * says, so the server can speed a model up.
 */
final class BedrockAnimations {

    /**
     * The property every model reads its animation speed from.
     */
    static final String SPEED = "hyperion:speed";

    private static final float PIXELS_PER_BLOCK = 16f;
    private static final float TOLERANCE = 0.01f;
    private static final float MIN_LENGTH = 0.05f;
    private static final String TIME = "%.2f";

    private BedrockAnimations() {
    }

    /**
     * Write the animations of a blueprint.
     *
     * @param blueprint the blueprint
     * @param slug the model's Bedrock name
     * @param names the animations, in the order the model's properties number them
     * @return the animation file
     */
    static JsonObject of(final Blueprint blueprint, final String slug, final List<String> names) {
        JsonObject animations = new JsonObject();
        for (int i = 0; i < names.size(); i++) {
            BakedAnimation baked = blueprint.baked(names.get(i)).orElse(null);
            if (baked != null) {
                animations.add(name(slug, i), animation(baked));
            }
        }

        JsonObject file = new JsonObject();
        file.addProperty("format_version", "1.8.0");
        file.add("animations", animations);

        return file;
    }

    /**
     * Return the Bedrock name of one animation.
     *
     * @param slug the model's Bedrock name
     * @param index the animation's number
     * @return the name
     */
    static String name(final String slug, final int index) {
        return "animation.hyperion." + slug + "." + index;
    }

    private static JsonObject animation(final BakedAnimation baked) {
        JsonObject json = new JsonObject();
        switch (baked.loop()) {
            case LOOP -> json.addProperty("loop", true);
            case HOLD -> json.addProperty("loop", "hold_on_last_frame");
            case ONCE -> json.addProperty("loop", false);
        }

        json.addProperty("animation_length", Math.max(MIN_LENGTH, baked.seconds()));
        json.addProperty("anim_time_update", "query.anim_time + query.delta_time * query.property('" + SPEED + "')");
        JsonObject bones = new JsonObject();
        for (Map.Entry<String, BoneTrack> track : baked.tracks().entrySet()) {
            JsonObject channels = new JsonObject();
            add(channels, "position", track.getValue().position(), Channel.POSITION);
            add(channels, "rotation", track.getValue().rotation(), Channel.ROTATION);
            add(channels, "scale", track.getValue().scale(), Channel.SCALE);
            if (!channels.isEmpty()) {
                bones.add(track.getKey(), channels);
            }
        }

        json.add("bones", bones);

        return json;
    }

    /**
     * Add one channel's keyframes, leaving out the samples a straight line already gives.
     *
     * @param channels where the channel goes
     * @param name the channel's name
     * @param samples three floats per tick in the bone's space, or null when not keyed
     * @param channel which channel it is, for turning it back into Bedrock's space
     */
    private static void add(
            final JsonObject channels,
            final String name,
            final float @Nullable [] samples,
            final Channel channel
    ) {
        if (samples == null) {
            return;
        }

        int count = samples.length / 3;
        JsonObject keys = new JsonObject();
        int kept = 0;
        for (int tick = 0; tick < count; tick++) {
            boolean edge = tick == 0 || tick == count - 1;
            if (!edge && onLine(samples, kept, tick, tick + 1)) {
                continue;
            }

            kept = tick;
            float seconds = (float) tick / AnimationBaker.TICKS_PER_SECOND;
            keys.add(String.format(Locale.ROOT, TIME, seconds), channel.toBedrock(samples, tick * 3));
        }

        channels.add(name, keys);
    }

    /**
     * Return whether a sample lies on the straight line between two others.
     *
     * @param samples the samples
     * @param before the earlier sample kept
     * @param tick the sample to check
     * @param after the next sample
     * @return true when leaving it out changes nothing visible
     */
    private static boolean onLine(final float[] samples, final int before, final int tick, final int after) {
        float blend = (float) (tick - before) / (after - before);
        for (int axis = 0; axis < 3; axis++) {
            float from = samples[before * 3 + axis];
            float to = samples[after * 3 + axis];
            float expected = from + (to - from) * blend;
            if (Math.abs(samples[tick * 3 + axis] - expected) > TOLERANCE) {
                return false;
            }
        }

        return true;
    }

    /**
     * The three transform channels and how each goes back to Bedrock's space.
     */
    private enum Channel {

        POSITION {
            @Override
            JsonArray toBedrock(final float[] samples, final int at) {
                return array(-samples[at] * PIXELS_PER_BLOCK, samples[at + 1] * PIXELS_PER_BLOCK, samples[at + 2] * PIXELS_PER_BLOCK);
            }
        },
        ROTATION {
            @Override
            JsonArray toBedrock(final float[] samples, final int at) {
                return array(-samples[at], -samples[at + 1], samples[at + 2]);
            }
        },
        SCALE {
            @Override
            JsonArray toBedrock(final float[] samples, final int at) {
                return array(samples[at], samples[at + 1], samples[at + 2]);
            }
        };

        /**
         * Turn one sample back into Bedrock's space.
         *
         * @param samples the samples
         * @param at where the three values start
         * @return the keyframe value
         */
        abstract JsonArray toBedrock(float[] samples, int at);

        private static JsonArray array(final float x, final float y, final float z) {
            JsonArray array = new JsonArray();
            array.add(new JsonPrimitive(Math.round(x * 1000f) / 1000f));
            array.add(new JsonPrimitive(Math.round(y * 1000f) / 1000f));
            array.add(new JsonPrimitive(Math.round(z * 1000f) / 1000f));

            return array;
        }
    }
}
