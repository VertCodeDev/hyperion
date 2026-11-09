package dev.vertcode.hyperion.geyser;

import java.util.Set;

/**
 * What one Bedrock player's copy of a model shows, as the server last said.
 *
 * @param playing the numbers of the animations that play
 * @param speed how fast they play
 * @param scale the model's scale
 */
record ModelState(Set<Integer> playing, float speed, float scale) {

    /**
     * A model that plays nothing at its normal size.
     */
    static final ModelState IDLE = new ModelState(Set.of(), 1f, 1f);

    /**
     * Copy the set, so a state never changes.
     */
    ModelState {
        playing = Set.copyOf(playing);
    }
}
