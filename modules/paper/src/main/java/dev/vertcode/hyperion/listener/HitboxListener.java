package dev.vertcode.hyperion.listener;

import java.util.Optional;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.InteractionHand;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientAttack;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity.InteractAction;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;

import dev.vertcode.hyperion.api.event.InteractionType;
import dev.vertcode.hyperion.api.event.ModelInteractEvent;
import dev.vertcode.hyperion.api.model.EntityAnchor;
import dev.vertcode.hyperion.api.model.Model;
import dev.vertcode.hyperion.model.HitboxTarget;
import dev.vertcode.hyperion.model.ModelInteractions;
import dev.vertcode.hyperion.model.ModelManager;

/**
 * Turn hits and clicks on a model's hitboxes into {@link ModelInteractEvent}, and pass them on to
 * the entity carrying the model as if the player touched the entity itself.
 *
 * <p>A hitbox is an interaction only its viewers know about, so the server would ignore what
 * touches it. This listener takes those packets off the network thread, fires the event on the
 * thread that owns the model, and when nobody cancels it hands the server the same packet aimed
 * at the entity, which then runs its own reach checks, damage and click handling.
 *
 * <p>From 26.1 a hit is its own packet and a click sends one packet; before, both are interact
 * packets and a click sends two, of which only the second counts.
 */
public final class HitboxListener implements PacketListener {

    private final ModelManager models;
    private final ModelInteractions index;
    private final boolean splitPackets;

    /**
     * Create the listener.
     *
     * @param models the live models
     */
    public HitboxListener(final ModelManager models) {
        this.models = models;
        this.index = models.interactions();
        this.splitPackets = PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_26_1);
    }

    @Override
    public void onPacketReceive(final PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.ATTACK) {
            WrapperPlayClientAttack packet = new WrapperPlayClientAttack(event);
            HitboxTarget target = this.index.hitbox(packet.getEntityId());
            if (target != null) {
                event.setCancelled(true);
                this.touch(event, target, InteractionType.ATTACK, InteractionHand.MAIN_HAND, Vector3d.zero(), false);
            }

            return;
        }

        if (event.getPacketType() != PacketType.Play.Client.INTERACT_ENTITY) {
            return;
        }

        WrapperPlayClientInteractEntity packet = new WrapperPlayClientInteractEntity(event);
        HitboxTarget target = this.index.hitbox(packet.getEntityId());
        if (target == null) {
            return;
        }

        event.setCancelled(true);
        InteractAction action = packet.getAction();
        boolean sneaking = packet.isSneaking().orElse(false);
        if (action == InteractAction.ATTACK) {
            this.touch(event, target, InteractionType.ATTACK, InteractionHand.MAIN_HAND, Vector3d.zero(), sneaking);
            return;
        }

        boolean counts = this.splitPackets ? action == InteractAction.INTERACT_AT : action == InteractAction.INTERACT;
        if (counts) {
            this.touch(event, target, InteractionType.INTERACT, packet.getHand(), packet.getLocation(), sneaking);
        }
    }

    /**
     * Handle a hit or click on the thread that owns the model.
     *
     * @param event the packet
     * @param target the hitbox
     * @param type a hit or a click
     * @param hand the hand used
     * @param location where on the hitbox a click landed, for 26.1 and newer
     * @param sneaking whether the player sneaked
     */
    private void touch(
            final PacketReceiveEvent event,
            final HitboxTarget target,
            final InteractionType type,
            final InteractionHand hand,
            final Vector3d location,
            final boolean sneaking
    ) {
        Player player = event.getPlayer();
        User user = event.getUser();
        if (player == null) {
            return;
        }

        this.models.runOnOwner(target.model(), () -> {
            Model model = target.model();
            if (model.isRemoved() || !player.isOnline()) {
                return;
            }

            EquipmentSlot slot = hand == InteractionHand.OFF_HAND ? EquipmentSlot.OFF_HAND : EquipmentSlot.HAND;
            ModelInteractEvent touched = new ModelInteractEvent(model, player, target.bone(), type, slot);
            Bukkit.getPluginManager().callEvent(touched);
            if (touched.isCancelled() || !(model.anchor() instanceof EntityAnchor(Entity carrier))) {
                return;
            }

            if (type == InteractionType.ATTACK && !target.forwardsDamage()) {
                return;
            }

            user.receivePacketSilently(this.aimed(carrier.getEntityId(), type, hand, location, sneaking));
        });
    }

    /**
     * Build the packet the client would have sent had it touched the entity itself.
     *
     * @param entityId the entity carrying the model
     * @param type a hit or a click
     * @param hand the hand used
     * @param location where on the hitbox a click landed, for 26.1 and newer
     * @param sneaking whether the player sneaked
     * @return the packet
     */
    private PacketWrapper<?> aimed(
            final int entityId,
            final InteractionType type,
            final InteractionHand hand,
            final Vector3d location,
            final boolean sneaking
    ) {
        if (type == InteractionType.ATTACK) {
            return this.splitPackets
                    ? new WrapperPlayClientAttack(entityId)
                    : new WrapperPlayClientInteractEntity(entityId, InteractAction.ATTACK, hand, Optional.empty(), Optional.of(sneaking));
        }

        return this.splitPackets
                ? new WrapperPlayClientInteractEntity(entityId, hand, location, sneaking)
                : new WrapperPlayClientInteractEntity(entityId, InteractAction.INTERACT, hand, Optional.empty(), Optional.of(sneaking));
    }
}
