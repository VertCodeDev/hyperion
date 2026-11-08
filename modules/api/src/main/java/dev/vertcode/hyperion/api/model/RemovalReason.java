package dev.vertcode.hyperion.api.model;

/**
 * Why a model was removed.
 */
public enum RemovalReason {

    /**
     * {@link Model#remove()} was called.
     */
    REMOVED,

    /**
     * The entity carrying the model died or was removed.
     */
    ENTITY_REMOVED,

    /**
     * The entity carrying the model unloaded with its chunk.
     */
    ENTITY_UNLOADED,

    /**
     * The model's blueprint was unregistered.
     */
    BLUEPRINT_UNREGISTERED,

    /**
     * The plugin that owns the blueprint, or Hyperion itself, disabled.
     */
    PLUGIN_DISABLED
}
