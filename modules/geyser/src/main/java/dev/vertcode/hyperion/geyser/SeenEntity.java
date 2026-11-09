package dev.vertcode.hyperion.geyser;

import java.util.UUID;

/**
 * One entity as one Bedrock player sees it.
 *
 * @param player the player's Java UUID
 * @param entity the entity's Java id
 */
record SeenEntity(UUID player, int entity) {
}
