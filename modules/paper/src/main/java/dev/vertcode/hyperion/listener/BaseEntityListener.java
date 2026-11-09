package dev.vertcode.hyperion.listener;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;

import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.protocol.player.Equipment;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityEquipment;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetPassengers;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity;

import dev.vertcode.hyperion.api.model.EntityPart;
import dev.vertcode.hyperion.bedrock.BedrockIds;
import dev.vertcode.hyperion.model.ModelManager;
import dev.vertcode.hyperion.render.DisplayData;
import dev.vertcode.hyperion.render.HiddenParts;

/**
 * Change the packets of an entity that carries a model, for each player who sees the model: hide
 * the parts the model hides, and keep the model's displays riding it when the server sends who
 * rides it.
 *
 * <p>Runs on Netty threads for every entity data, equipment and passenger packet, so it returns
 * at once while no entity carries a model, and reads only concurrent state.
 */
public final class BaseEntityListener implements PacketListener {

    private final ModelManager models;

    /**
     * Create the listener.
     *
     * @param models the live models
     */
    public BaseEntityListener(final ModelManager models) {
        this.models = models;
    }

    @Override
    public void onPacketSend(final PacketSendEvent event) {
        PacketTypeCommon type = event.getPacketType();
        boolean watched = type == PacketType.Play.Server.ENTITY_METADATA
                || type == PacketType.Play.Server.ENTITY_EQUIPMENT
                || type == PacketType.Play.Server.SET_PASSENGERS
                || type == PacketType.Play.Server.SPAWN_ENTITY;
        if (!watched || !this.models.anyOnEntities()) {
            return;
        }

        UUID viewer = event.getUser().getUUID();
        if (viewer == null) {
            return;
        }

        // A Bedrock player sees the entity itself turned into the model by Geyser: nothing of it
        // is hidden and no display rides it, but its spawn names the model in its UUID.
        if (this.models.isBedrock(viewer)) {
            if (type == PacketType.Play.Server.SPAWN_ENTITY) {
                this.nameModel(event);
            }

            return;
        }

        if (type == PacketType.Play.Server.SPAWN_ENTITY) {
            return;
        }

        if (type == PacketType.Play.Server.ENTITY_METADATA) {
            this.metadata(event, viewer);
        } else if (type == PacketType.Play.Server.ENTITY_EQUIPMENT) {
            this.equipment(event, viewer);
        } else {
            this.passengers(event, viewer);
        }
    }

    /**
     * Put the model an entity carries into the UUID of its spawn packet, for Geyser to read.
     *
     * @param event the packet
     */
    private void nameModel(final PacketSendEvent event) {
        WrapperPlayServerSpawnEntity packet = new WrapperPlayServerSpawnEntity(event);
        OptionalInt model = this.models.bedrockModel(packet.getEntityId());
        if (model.isEmpty()) {
            return;
        }

        packet.setUUID(Optional.of(BedrockIds.uuid(model.getAsInt(), packet.getEntityId())));
        event.markForReEncode(true);
    }

    /**
     * Hide the body, fire, outline and name in an entity data packet.
     *
     * @param event the packet
     * @param viewer who it goes to
     */
    private void metadata(final PacketSendEvent event, final UUID viewer) {
        WrapperPlayServerEntityMetadata packet = new WrapperPlayServerEntityMetadata(event);
        Set<EntityPart> hidden = this.models.hiddenFor(packet.getEntityId(), viewer);
        if (hidden.isEmpty()) {
            return;
        }

        List<EntityData<?>> data = packet.getEntityMetadata();
        boolean changed = false;
        for (int i = 0; i < data.size(); i++) {
            EntityData<?> entry = data.get(i);
            if (entry.getIndex() == DisplayData.FLAGS && entry.getValue() instanceof Byte flags) {
                data.set(i, new EntityData<>(DisplayData.FLAGS, EntityDataTypes.BYTE, HiddenParts.flags(flags, hidden)));
                changed = true;
            } else if (hidden.contains(EntityPart.NAME) && entry.getIndex() == DisplayData.CUSTOM_NAME) {
                data.set(i, new EntityData<>(DisplayData.CUSTOM_NAME, EntityDataTypes.OPTIONAL_ADV_COMPONENT, Optional.empty()));
                changed = true;
            } else if (hidden.contains(EntityPart.NAME) && entry.getIndex() == DisplayData.CUSTOM_NAME_VISIBLE) {
                data.set(i, new EntityData<>(DisplayData.CUSTOM_NAME_VISIBLE, EntityDataTypes.BOOLEAN, false));
                changed = true;
            }
        }

        if (changed) {
            packet.setEntityMetadata(data);
            event.markForReEncode(true);
        }
    }

    /**
     * Empty every slot in an equipment packet.
     *
     * @param event the packet
     * @param viewer who it goes to
     */
    private void equipment(final PacketSendEvent event, final UUID viewer) {
        WrapperPlayServerEntityEquipment packet = new WrapperPlayServerEntityEquipment(event);
        if (!this.models.hiddenFor(packet.getEntityId(), viewer).contains(EntityPart.EQUIPMENT)) {
            return;
        }

        for (Equipment slot : packet.getEquipment()) {
            slot.setItem(ItemStack.EMPTY);
        }

        event.markForReEncode(true);
    }

    /**
     * Add the model's displays to the riders of the entity carrying it.
     *
     * @param event the packet
     * @param viewer who it goes to
     */
    private void passengers(final PacketSendEvent event, final UUID viewer) {
        WrapperPlayServerSetPassengers packet = new WrapperPlayServerSetPassengers(event);
        int[] displays = this.models.displaysFor(packet.getEntityId(), viewer);
        if (displays.length == 0) {
            return;
        }

        int[] riders = packet.getPassengers();
        int[] all = new int[riders.length + displays.length];
        System.arraycopy(riders, 0, all, 0, riders.length);
        System.arraycopy(displays, 0, all, riders.length, displays.length);
        packet.setPassengers(all);
        event.markForReEncode(true);
    }
}
