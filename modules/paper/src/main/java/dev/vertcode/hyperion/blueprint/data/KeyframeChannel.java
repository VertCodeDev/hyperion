package dev.vertcode.hyperion.blueprint.data;

import java.util.Optional;

/**
 * What a keyframe changes: a bone's transform, or an effect on the timeline.
 */
public enum KeyframeChannel {

    ROTATION("rotation", true),
    POSITION("position", true),
    SCALE("scale", true),
    PARTICLE("particle", false),
    SOUND("sound", false),
    TIMELINE("timeline", false);

    private final String key;
    private final boolean transform;

    KeyframeChannel(final String key, final boolean transform) {
        this.key = key;
        this.transform = transform;
    }

    /**
     * Return whether this channel moves a bone, rather than playing an effect.
     *
     * @return true for rotation, position and scale
     */
    public boolean transform() {
        return this.transform;
    }

    /**
     * Find the channel Blockbench writes under a name.
     *
     * @param key the name in the file
     * @return the channel, or empty for a channel Hyperion does not know
     */
    public static Optional<KeyframeChannel> fromKey(final String key) {
        for (KeyframeChannel channel : values()) {
            if (channel.key.equals(key)) {
                return Optional.of(channel);
            }
        }

        return Optional.empty();
    }
}
