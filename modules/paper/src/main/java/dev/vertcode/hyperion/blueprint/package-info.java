/**
 * Blueprints: Blockbench models loaded, checked and kept ready to spawn.
 *
 * <p>{@link dev.vertcode.hyperion.blueprint.BlueprintRegistry} loads files off the tick
 * threads and holds the results; everything a blueprint is made of is immutable, so any thread
 * may read it.
 */
@NullMarked
package dev.vertcode.hyperion.blueprint;

import org.jspecify.annotations.NullMarked;
