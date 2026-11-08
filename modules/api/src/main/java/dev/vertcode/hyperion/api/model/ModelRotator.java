package dev.vertcode.hyperion.api.model;

/**
 * Works out which way a model faces every frame, starting from what its anchor says.
 *
 * <p>The base rotation is the body of the entity carrying the model, or the yaw and pitch of the
 * location a model on its own stands at. Called on the region that owns the model, once per
 * frame while it has viewers. Keep it cheap. Hyperion drops a rotator when the plugin it comes
 * from disables.
 *
 * <pre>{@code
 * // a turret that always faces north, whatever its entity does
 * model.rotator(ModelRotator.fixed(new ModelRotation(0f, 180f)));
 * }</pre>
 */
@FunctionalInterface
public interface ModelRotator {

    /**
     * Face the way the anchor faces. The default.
     *
     * @return the rotator
     */
    static ModelRotator anchor() {
        return (model, base) -> base;
    }

    /**
     * Always face one way.
     *
     * @param rotation the rotation to keep
     * @return the rotator
     */
    static ModelRotator fixed(final ModelRotation rotation) {
        return (model, base) -> rotation;
    }

    /**
     * Face the anchor's way, turned by a number of degrees.
     *
     * @param degrees the yaw to add
     * @return the rotator
     */
    static ModelRotator yawOffset(final float degrees) {
        return (model, base) -> base.withYaw(base.yaw() + degrees);
    }

    /**
     * Work out the rotation for this frame.
     *
     * @param model the model being turned
     * @param base the rotation the anchor gives
     * @return the rotation to use
     */
    ModelRotation rotate(Model model, ModelRotation base);

    /**
     * Run another rotator on the result of this one.
     *
     * @param next the rotator that runs second
     * @return the combined rotator
     */
    default ModelRotator then(final ModelRotator next) {
        return (model, base) -> next.rotate(model, this.rotate(model, base));
    }
}
