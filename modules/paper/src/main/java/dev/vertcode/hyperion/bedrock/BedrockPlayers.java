package dev.vertcode.hyperion.bedrock;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.jspecify.annotations.Nullable;

/**
 * Tell Bedrock players apart, so they get native Bedrock models instead of display packets.
 *
 * <p>A player counts as Bedrock when the Geyser extension says so, which is exact wherever Geyser
 * runs; when Floodgate says so, which covers linked accounts; or when the UUID is the kind
 * Floodgate gives unlinked players, which starts with 64 zero bits. The answer is worked out once
 * when a player joins and kept in a set, since the packet listener asks for every packet.
 */
public final class BedrockPlayers {

    private static final String FLOODGATE = "org.geysermc.floodgate.api.FloodgateApi";

    private final Set<UUID> bedrock = ConcurrentHashMap.newKeySet();
    private final Set<UUID> reported = ConcurrentHashMap.newKeySet();
    private final @Nullable Object floodgate;
    private final @Nullable Method isFloodgatePlayer;

    /**
     * Find Floodgate, when it is installed.
     */
    public BedrockPlayers() {
        Object api = null;
        Method check = null;
        try {
            Class<?> type = Class.forName(FLOODGATE);
            api = type.getMethod("getInstance").invoke(null);
            check = type.getMethod("isFloodgatePlayer", UUID.class);
        } catch (ReflectiveOperationException | LinkageError exception) {
            // No Floodgate: the extension and the UUID still tell.
        }

        this.floodgate = api;
        this.isFloodgatePlayer = check;
    }

    /**
     * Return whether a player is on Bedrock. Cheap, for every packet.
     *
     * @param player the player's id
     * @return true for a Bedrock player
     */
    public boolean isBedrock(final UUID player) {
        return this.bedrock.contains(player);
    }

    /**
     * Work out whether a player who joined is on Bedrock.
     *
     * @param player the player's id
     */
    public void joined(final UUID player) {
        boolean zeroTop = player.getMostSignificantBits() == 0L;
        if (zeroTop || this.reported.contains(player) || this.floodgate(player)) {
            this.bedrock.add(player);
        }
    }

    /**
     * Forget a player who left.
     *
     * @param player the player's id
     */
    public void left(final UUID player) {
        this.bedrock.remove(player);
    }

    /**
     * Take the Geyser extension's word on who joined or left through Geyser.
     *
     * @param joined the Bedrock players who joined
     * @param left the Bedrock players who left
     */
    void report(final Collection<UUID> joined, final Collection<UUID> left) {
        this.reported.addAll(joined);
        this.bedrock.addAll(joined);
        this.reported.removeAll(left);
    }

    /**
     * Ask Floodgate about a player.
     *
     * @param player the player's id
     * @return true when Floodgate is installed and says the player is on Bedrock
     */
    private boolean floodgate(final UUID player) {
        if (this.floodgate == null || this.isFloodgatePlayer == null) {
            return false;
        }

        try {
            return Boolean.TRUE.equals(this.isFloodgatePlayer.invoke(this.floodgate, player));
        } catch (ReflectiveOperationException exception) {
            return false;
        }
    }
}
