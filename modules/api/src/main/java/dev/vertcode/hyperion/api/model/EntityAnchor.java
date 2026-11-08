package dev.vertcode.hyperion.api.model;

import org.bukkit.entity.Entity;

/**
 * A model that rides an entity and follows its position, body and head.
 *
 * @param entity the entity carrying the model
 */
public record EntityAnchor(Entity entity) implements ModelAnchor {
}
