package dev.vertcode.hyperion.render;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.player.Equipment;
import com.github.retrooper.packetevents.protocol.player.EquipmentSlot;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityEquipment;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.jspecify.annotations.Nullable;

import dev.vertcode.hyperion.api.model.EntityPart;

/**
 * The packets that tell a viewer how the entity carrying a model looks, with some parts hidden.
 *
 * <p>The entity's own packets are changed on their way out while a model hides its parts; these
 * packets cover the moments in between: a viewer who starts seeing the model after the entity
 * was sent, and a change in which parts are hidden. Read the entity on the thread that owns it.
 */
public final class BaseEntityData {

    private static final byte SNEAKING = 0x02;
    private static final byte SPRINTING = 0x08;
    private static final byte SWIMMING = 0x10;
    private static final byte GLIDING = (byte) 0x80;
    // Bukkit's EquipmentSlot is qualified here and below: PacketEvents has one by the same name.
    private static final Map<org.bukkit.inventory.EquipmentSlot, EquipmentSlot> SLOTS = Map.of(
            org.bukkit.inventory.EquipmentSlot.HAND, EquipmentSlot.MAIN_HAND,
            org.bukkit.inventory.EquipmentSlot.OFF_HAND, EquipmentSlot.OFF_HAND,
            org.bukkit.inventory.EquipmentSlot.FEET, EquipmentSlot.BOOTS,
            org.bukkit.inventory.EquipmentSlot.LEGS, EquipmentSlot.LEGGINGS,
            org.bukkit.inventory.EquipmentSlot.CHEST, EquipmentSlot.CHEST_PLATE,
            org.bukkit.inventory.EquipmentSlot.HEAD, EquipmentSlot.HELMET
    );

    private BaseEntityData() {
    }

    /**
     * Build the packets that show an entity with some parts hidden, or whole again.
     *
     * @param entity the entity
     * @param hidden the parts to hide, empty to show it whole
     * @return the packets, to send in order
     */
    public static List<PacketWrapper<?>> packets(final Entity entity, final Set<EntityPart> hidden) {
        List<PacketWrapper<?>> packets = new ArrayList<>(2);
        List<EntityData<?>> data = new ArrayList<>(3);
        data.add(new EntityData<>(DisplayData.FLAGS, EntityDataTypes.BYTE, HiddenParts.flags(flags(entity), hidden)));
        if (!(entity instanceof Player)) {
            boolean nameHidden = hidden.contains(EntityPart.NAME);
            @Nullable Component name = nameHidden ? null : entity.customName();
            data.add(new EntityData<>(DisplayData.CUSTOM_NAME, EntityDataTypes.OPTIONAL_ADV_COMPONENT, Optional.ofNullable(name)));
            boolean nameVisible = !nameHidden && entity.isCustomNameVisible();
            data.add(new EntityData<>(DisplayData.CUSTOM_NAME_VISIBLE, EntityDataTypes.BOOLEAN, nameVisible));
        }

        packets.add(Packets.metadata(entity.getEntityId(), data));
        if (entity instanceof LivingEntity living && living.getEquipment() != null) {
            packets.add(equipment(living.getEntityId(), living.getEquipment(), hidden.contains(EntityPart.EQUIPMENT)));
        }

        return packets;
    }

    /**
     * Work out an entity's flags byte from what the server knows about it.
     *
     * @param entity the entity
     * @return the flags
     */
    private static byte flags(final Entity entity) {
        int flags = 0;
        if (entity.getFireTicks() > 0 || entity.isVisualFire()) {
            flags |= DisplayData.ON_FIRE;
        }

        if (entity.isSneaking()) {
            flags |= SNEAKING;
        }

        if (entity instanceof Player player && player.isSprinting()) {
            flags |= SPRINTING;
        }

        if (entity instanceof LivingEntity living) {
            if (living.isSwimming()) {
                flags |= SWIMMING;
            }

            if (living.isGliding()) {
                flags |= GLIDING;
            }
        }

        if (entity.isInvisible()) {
            flags |= DisplayData.INVISIBLE;
        }

        if (entity.isGlowing()) {
            flags |= DisplayData.GLOWING;
        }

        return (byte) flags;
    }

    /**
     * Build the packet with an entity's equipment, or with every slot empty.
     *
     * @param entityId the entity id
     * @param equipment what the entity wears and holds
     * @param hidden true to send every slot empty
     * @return the packet
     */
    private static WrapperPlayServerEntityEquipment equipment(
            final int entityId,
            final EntityEquipment equipment,
            final boolean hidden
    ) {
        List<Equipment> slots = new ArrayList<>(SLOTS.size());
        for (Map.Entry<org.bukkit.inventory.EquipmentSlot, EquipmentSlot> slot : SLOTS.entrySet()) {
            ItemStack item = hidden
                    ? ItemStack.EMPTY
                    : SpigotConversionUtil.fromBukkitItemStack(equipment.getItem(slot.getKey()));
            slots.add(new Equipment(slot.getValue(), item));
        }

        return new WrapperPlayServerEntityEquipment(entityId, slots);
    }
}
