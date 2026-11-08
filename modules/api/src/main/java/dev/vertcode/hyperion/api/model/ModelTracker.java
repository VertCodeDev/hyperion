package dev.vertcode.hyperion.api.model;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Unmodifiable;

/**
 * Finds the live models in the world.
 *
 * <p>Every method returns a snapshot and is safe from any thread. To change a model you found,
 * use its setters from anywhere, or hop to its region for {@link Model#remove()}.
 */
@ApiStatus.NonExtendable
public interface ModelTracker {

    /**
     * Return the models an entity carries.
     *
     * @param entity the entity
     * @return the entity's models, empty when it has none
     */
    @Unmodifiable List<Model> of(Entity entity);

    /**
     * Find a model by its id.
     *
     * @param id the model's {@link Model#uniqueId()}
     * @return the model, or empty if no live model has that id
     */
    Optional<Model> byId(UUID id);

    /**
     * Return the models whose origin is within a radius of a location.
     *
     * @param center the center of the search
     * @param radius the radius in blocks
     * @return the models in range, nearest first
     */
    @Unmodifiable List<Model> near(Location center, double radius);
}
