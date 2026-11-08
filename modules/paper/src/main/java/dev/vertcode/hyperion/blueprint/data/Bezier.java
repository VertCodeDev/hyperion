package dev.vertcode.hyperion.blueprint.data;

import org.joml.Vector3f;
import org.joml.Vector3fc;

/**
 * The two handles of a bezier keyframe, per axis.
 *
 * @param leftTime how far before the keyframe the left handle sits, in seconds
 * @param leftValue the left handle's value offset
 * @param rightTime how far after the keyframe the right handle sits, in seconds
 * @param rightValue the right handle's value offset
 */
public record Bezier(Vector3fc leftTime, Vector3fc leftValue, Vector3fc rightTime, Vector3fc rightValue) {

    /**
     * Copy every vector, so the handles never change after they are read.
     */
    public Bezier {
        leftTime = new Vector3f(leftTime);
        leftValue = new Vector3f(leftValue);
        rightTime = new Vector3f(rightTime);
        rightValue = new Vector3f(rightValue);
    }
}
