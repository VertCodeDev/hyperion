/**
 * The plain records a Blockbench file is read into, before it becomes a blueprint.
 *
 * <p>Nothing here touches the server, so the reader can be tested without one. Cube geometry
 * stays in Blockbench pixels because the resource pack is written in pixels; the blueprint
 * converts bones and locators to blocks.
 */
@NullMarked
package dev.vertcode.hyperion.blueprint.data;

import org.jspecify.annotations.NullMarked;
