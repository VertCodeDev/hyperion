package dev.vertcode.hyperion.render;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityTeleport;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetPassengers;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity;
import org.bukkit.Location;

/**
 * The packets that show, change and remove displays.
 *
 * <p>Packets are sent silently, past every PacketEvents listener, Hyperion's own included, so
 * what a model sends reaches the client as built. Sending is safe from any thread.
 */
public final class Packets {

    private Packets() {
    }

    /**
     * Send a packet to one player.
     *
     * @param user the player's connection
     * @param packet the packet
     */
    public static void send(final User user, final PacketWrapper<?> packet) {
        user.sendPacketSilently(packet);
    }

    /**
     * Build the packet that spawns an item display.
     *
     * @param entityId the display's entity id
     * @param uniqueId the display's id
     * @param location where it spawns
     * @return the packet
     */
    public static WrapperPlayServerSpawnEntity spawn(final int entityId, final UUID uniqueId, final Location location) {
        return new WrapperPlayServerSpawnEntity(
                entityId,
                Optional.of(uniqueId),
                EntityTypes.ITEM_DISPLAY,
                new Vector3d(location.getX(), location.getY(), location.getZ()),
                0f,
                0f,
                0f,
                0,
                Optional.empty()
        );
    }

    /**
     * Build the packet that changes some of an entity's data.
     *
     * @param entityId the entity id
     * @param data the entries that changed
     * @return the packet
     */
    public static WrapperPlayServerEntityMetadata metadata(final int entityId, final List<EntityData<?>> data) {
        return new WrapperPlayServerEntityMetadata(entityId, List.copyOf(data));
    }

    /**
     * Build the packet that moves a display.
     *
     * @param entityId the display's entity id
     * @param location where it moves to
     * @return the packet
     */
    public static WrapperPlayServerEntityTeleport teleport(final int entityId, final Location location) {
        Vector3d position = new Vector3d(location.getX(), location.getY(), location.getZ());

        return new WrapperPlayServerEntityTeleport(entityId, position, 0f, 0f, false);
    }

    /**
     * Build the packet that sets who rides an entity.
     *
     * @param vehicle the entity id of the one ridden
     * @param passengers the entity ids of every passenger, real ones and displays
     * @return the packet
     */
    public static WrapperPlayServerSetPassengers passengers(final int vehicle, final int[] passengers) {
        return new WrapperPlayServerSetPassengers(vehicle, passengers.clone());
    }

    /**
     * Build the packet that removes displays.
     *
     * @param entityIds the displays' entity ids
     * @return the packet
     */
    public static WrapperPlayServerDestroyEntities destroy(final int[] entityIds) {
        return new WrapperPlayServerDestroyEntities(entityIds.clone());
    }
}
