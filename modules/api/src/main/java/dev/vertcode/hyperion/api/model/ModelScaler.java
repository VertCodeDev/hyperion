package dev.vertcode.hyperion.api.model;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

/**
 * Works out a model's scale every frame, so the scale can follow the game.
 *
 * <p>Called on the region that owns the model, once per frame while it has viewers. Keep it
 * cheap. Hyperion drops a scaler when the plugin it comes from disables.
 *
 * <pre>{@code
 * // twice the size of the entity, following its scale attribute
 * model.scaler(ModelScaler.entityScale().times(2f));
 * }</pre>
 */
@FunctionalInterface
public interface ModelScaler {

    /**
     * Keep the model at one scale.
     *
     * @param scale the scale, above 0
     * @return the scaler
     * @throws IllegalArgumentException if the scale is not above 0
     */
    static ModelScaler fixed(final float scale) {
        if (scale <= 0f) {
            throw new IllegalArgumentException("A model scale must be above 0");
        }

        return _ -> scale;
    }

    /**
     * Follow the scale attribute of the entity carrying the model, so a model on a baby or a
     * shrunk mob shrinks with it. A model on its own stays at 1.
     *
     * @return the scaler
     */
    static ModelScaler entityScale() {
        return model -> {
            if (!(model.anchor() instanceof EntityAnchor(Entity entity)) || !(entity instanceof LivingEntity living)) {
                return 1f;
            }

            AttributeInstance attribute = living.getAttribute(Attribute.SCALE);

            return attribute == null ? 1f : (float) attribute.getValue();
        };
    }

    /**
     * Work out the scale for this frame.
     *
     * @param model the model being scaled
     * @return the scale, above 0
     */
    float scale(Model model);

    /**
     * Multiply this scale by a fixed factor.
     *
     * @param factor the factor, above 0
     * @return the combined scaler
     */
    default ModelScaler times(final float factor) {
        return model -> this.scale(model) * factor;
    }

    /**
     * Multiply this scale by another scaler's.
     *
     * @param other the other scaler
     * @return the combined scaler
     */
    default ModelScaler times(final ModelScaler other) {
        return model -> this.scale(model) * other.scale(model);
    }
}
