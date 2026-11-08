package dev.vertcode.hyperion.api.model;

/**
 * What a model's position comes from: an entity it rides on, or a fixed location.
 *
 * <p>Switch over it to handle both:
 *
 * <pre>{@code
 * switch (model.anchor()) {
 *     case EntityAnchor(Entity entity) -> ...
 *     case LocationAnchor anchor -> ...
 * }
 * }</pre>
 */
public sealed interface ModelAnchor permits EntityAnchor, LocationAnchor {
}
