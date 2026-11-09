package dev.vertcode.hyperion.geyser;

import java.util.List;

/**
 * One model as Hyperion's manifest lists it.
 *
 * @param id the model's id, which spawn UUIDs carry
 * @param identifier the Bedrock entity identifier, such as {@code hyperion:bosses_dragon}
 * @param animations the animations, in the order the properties number them
 */
record BedrockModel(int id, String identifier, List<String> animations) {

    /**
     * Copy the list, so a model never changes.
     */
    BedrockModel {
        animations = List.copyOf(animations);
    }
}
