package dev.vertcode.hyperion.bedrock;

import java.util.List;
import java.util.UUID;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * What a Bedrock player's copy of a model shows: which animations play, how fast, and how big.
 *
 * @param playing the numbers of the animations that play, as the model's properties number them
 * @param speed how fast they play
 * @param scale the model's scale
 */
public record BedrockState(List<Integer> playing, float speed, float scale) {

    /**
     * Copy the list, so a state never changes.
     */
    public BedrockState {
        playing = List.copyOf(playing);
    }

    /**
     * Write the event that shows this state to some Bedrock players.
     *
     * @param entity the Java id of the entity the model is, for those players
     * @param players the Bedrock players who see it
     * @return the event
     */
    public JsonObject event(final int entity, final List<UUID> players) {
        JsonObject event = header("state", entity, players);
        JsonArray animations = new JsonArray();
        this.playing.forEach(animations::add);
        event.add("playing", animations);
        event.addProperty("speed", this.speed);
        event.addProperty("scale", this.scale);

        return event;
    }

    /**
     * Write the event that tells the extension some players no longer see a model.
     *
     * @param entity the Java id of the entity the model was
     * @param players the Bedrock players who stopped seeing it
     * @return the event
     */
    public static JsonObject gone(final int entity, final List<UUID> players) {
        return header("gone", entity, players);
    }

    private static JsonObject header(final String type, final int entity, final List<UUID> players) {
        JsonObject event = new JsonObject();
        event.addProperty("t", type);
        event.addProperty("entity", entity);
        JsonArray uuids = new JsonArray();
        for (UUID player : players) {
            uuids.add(player.toString());
        }

        event.add("players", uuids);

        return event;
    }
}
