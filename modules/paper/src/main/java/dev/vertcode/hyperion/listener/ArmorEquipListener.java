package dev.vertcode.hyperion.listener;

import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockDispenseArmorEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.PluginManager;
import org.jspecify.annotations.Nullable;

import dev.vertcode.hyperion.api.armor.ArmorSlot;
import dev.vertcode.hyperion.api.event.ArmorChangeCause;
import dev.vertcode.hyperion.api.event.ArmorEquipAttemptEvent;
import dev.vertcode.hyperion.api.event.ArmorEquipMethod;
import dev.vertcode.hyperion.armor.ArmorTracker;
import dev.vertcode.hyperion.armor.HyperionArmorPiece;
import dev.vertcode.hyperion.armor.HyperionArmorRegistry;

/**
 * Ask {@link ArmorEquipAttemptEvent} before a player puts on a piece of Hyperion armor, and
 * undo what they did when a plugin cancels it.
 *
 * <p>On 1.21 to 1.21.1 a helmet is a plain item, which vanilla does not let into the head slot,
 * so this also puts helmets on: by right-click, by placing, by shift-click and by hotbar swap.
 */
public final class ArmorEquipListener implements Listener {

    private static final int HEAD = 39;
    private static final int CHEST = 38;
    private static final int LEGS = 37;
    private static final int FEET = 36;
    private static final int FIRST_ARMOR_RAW_SLOT = 5;
    private static final int LAST_ARMOR_RAW_SLOT = 8;
    private static final int OFFHAND = 40;

    private final HyperionArmorRegistry registry;
    private final ArmorTracker tracker;
    private final PluginManager events;
    private final boolean placeHelmets;

    /**
     * Create the listener.
     *
     * @param registry finds the piece an item is
     * @param tracker told why armor is about to change
     * @param events where the attempt event is fired
     * @param placeHelmets whether helmets are plain items Hyperion puts on itself, before 1.21.2
     */
    public ArmorEquipListener(
            final HyperionArmorRegistry registry,
            final ArmorTracker tracker,
            final PluginManager events,
            final boolean placeHelmets
    ) {
        this.registry = registry;
        this.tracker = tracker;
        this.events = events;
        this.placeHelmets = placeHelmets;
    }

    /**
     * Ask before a right-click puts a piece on.
     *
     * @param event the click
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(final PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        ItemStack item = event.getItem();
        HyperionArmorPiece piece = this.registry.piece(item);
        if (item == null || piece == null || event.useItemInHand() == Event.Result.DENY) {
            return;
        }

        Player player = event.getPlayer();
        if (action == Action.RIGHT_CLICK_BLOCK && !player.isSneaking() && usesClick(event.getClickedBlock())) {
            return;
        }

        if (!this.allowed(player, piece, item, ArmorEquipMethod.RIGHT_CLICK)) {
            event.setUseItemInHand(Event.Result.DENY);
            return;
        }

        this.tracker.expect(player.getUniqueId(), piece.slot(), ArmorChangeCause.PLAYER);
        if (this.placeHelmets && piece.slot() == ArmorSlot.HEAD) {
            event.setCancelled(true);
            EquipmentSlot hand = event.getHand() == null ? EquipmentSlot.HAND : event.getHand();
            EntityEquipment equipment = player.getEquipment();
            ItemStack worn = equipment.getItem(EquipmentSlot.HEAD);
            equipment.setItem(EquipmentSlot.HEAD, one(item));
            equipment.setItem(hand, rest(item, worn));
            player.playSound(player, Sound.ITEM_ARMOR_EQUIP_GENERIC, 1f, 1f);
        }
    }

    /**
     * Ask before a click in the inventory puts a piece on: placing it in its slot, a hotbar or
     * offhand swap onto the slot, or a shift-click.
     *
     * @param event the click
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onClick(final InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)
                || !(event.getClickedInventory() instanceof PlayerInventory inventory)) {
            return;
        }

        if (event.getSlotType() == InventoryType.SlotType.ARMOR) {
            this.clickArmorSlot(event, player, inventory);
            return;
        }

        if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY
                && event.getView().getType() == InventoryType.CRAFTING) {
            this.shiftClick(event, player);
        }
    }

    /**
     * Ask before a drag across an armor slot puts a piece on.
     *
     * @param event the drag
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDrag(final InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player) || event.getView().getType() != InventoryType.CRAFTING) {
            return;
        }

        ItemStack item = event.getOldCursor();
        HyperionArmorPiece piece = this.registry.piece(item);
        if (piece == null) {
            return;
        }

        for (int raw : event.getRawSlots()) {
            if (raw >= FIRST_ARMOR_RAW_SLOT && raw <= LAST_ARMOR_RAW_SLOT) {
                if (!this.allowed(player, piece, item, ArmorEquipMethod.DRAG)) {
                    event.setCancelled(true);
                    return;
                }

                this.tracker.expect(player.getUniqueId(), piece.slot(), ArmorChangeCause.PLAYER);
                return;
            }
        }
    }

    /**
     * Ask before a dispenser puts a piece on a player.
     *
     * @param event the dispense
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDispense(final BlockDispenseArmorEvent event) {
        if (!(event.getTargetEntity() instanceof Player player)) {
            return;
        }

        ItemStack item = event.getItem();
        HyperionArmorPiece piece = this.registry.piece(item);
        if (piece == null) {
            return;
        }

        if (!this.allowed(player, piece, item, ArmorEquipMethod.DISPENSER)) {
            event.setCancelled(true);
            return;
        }

        this.tracker.expect(player.getUniqueId(), piece.slot(), ArmorChangeCause.DISPENSER);
    }

    /**
     * Handle a click on an armor slot: whatever changes there, the player did it.
     *
     * @param event the click
     * @param player the player
     * @param inventory their inventory
     */
    private void clickArmorSlot(final InventoryClickEvent event, final Player player, final PlayerInventory inventory) {
        ArmorSlot slot = armorSlot(event.getSlot());
        if (slot == null) {
            return;
        }

        this.tracker.expect(player.getUniqueId(), slot, ArmorChangeCause.PLAYER);
        boolean swap = event.getClick() == ClickType.NUMBER_KEY || event.getClick() == ClickType.SWAP_OFFHAND;
        ItemStack incoming;
        if (event.getClick() == ClickType.NUMBER_KEY) {
            incoming = inventory.getItem(event.getHotbarButton());
        } else if (event.getClick() == ClickType.SWAP_OFFHAND) {
            incoming = inventory.getItem(OFFHAND);
        } else {
            incoming = event.getCursor();
        }

        HyperionArmorPiece piece = this.registry.piece(incoming);
        if (incoming == null || piece == null || piece.slot() != slot) {
            return;
        }

        if (!this.allowed(player, piece, incoming, swap ? ArmorEquipMethod.HOTBAR_SWAP : ArmorEquipMethod.INVENTORY_CLICK)) {
            event.setCancelled(true);
            return;
        }

        if (!this.placeHelmets || slot != ArmorSlot.HEAD) {
            return;
        }

        // Vanilla keeps a plain item out of the head slot, so the swap is done here.
        event.setCancelled(true);
        ItemStack worn = inventory.getItem(HEAD);
        inventory.setItem(HEAD, one(incoming));
        if (event.getClick() == ClickType.NUMBER_KEY) {
            inventory.setItem(event.getHotbarButton(), rest(incoming, worn));
        } else if (event.getClick() == ClickType.SWAP_OFFHAND) {
            inventory.setItem(OFFHAND, rest(incoming, worn));
        } else {
            event.getView().setCursor(rest(incoming, worn));
        }
    }

    /**
     * Handle a shift-click that moves a piece into its empty slot.
     *
     * @param event the click
     * @param player the player
     */
    private void shiftClick(final InventoryClickEvent event, final Player player) {
        ItemStack item = event.getCurrentItem();
        HyperionArmorPiece piece = this.registry.piece(item);
        if (item == null || piece == null) {
            return;
        }

        EntityEquipment equipment = player.getEquipment();
        ItemStack worn = equipment.getItem(piece.slot().equipmentSlot());
        if (!worn.isEmpty()) {
            return;
        }

        if (!this.allowed(player, piece, item, ArmorEquipMethod.SHIFT_CLICK)) {
            event.setCancelled(true);
            return;
        }

        this.tracker.expect(player.getUniqueId(), piece.slot(), ArmorChangeCause.PLAYER);
        if (this.placeHelmets && piece.slot() == ArmorSlot.HEAD) {
            event.setCancelled(true);
            equipment.setItem(EquipmentSlot.HEAD, one(item));
            event.setCurrentItem(rest(item, null));
        }
    }

    /**
     * Fire the attempt event.
     *
     * @param player the player
     * @param piece the piece
     * @param item the item
     * @param method how they try it
     * @return true when no plugin cancelled it
     */
    private boolean allowed(final Player player, final HyperionArmorPiece piece, final ItemStack item, final ArmorEquipMethod method) {
        ArmorEquipAttemptEvent attempt = new ArmorEquipAttemptEvent(player, piece, item, method);
        this.events.callEvent(attempt);

        return !attempt.isCancelled();
    }

    /**
     * Return whether a block takes a right-click for itself, as a chest or door does, so the item
     * in hand is not used.
     *
     * @param block the block, or null
     * @return true when the block uses the click
     */
    // Newer Paper deprecates this as not exact for every block; no replacement exists on 1.21.
    @SuppressWarnings("deprecation")
    private static boolean usesClick(final @Nullable Block block) {
        return block != null && block.getType().isInteractable();
    }

    /**
     * Return the armor slot at a player inventory index.
     *
     * @param index the index, 36 to 39
     * @return the slot, or null for any other index
     */
    private static @Nullable ArmorSlot armorSlot(final int index) {
        return switch (index) {
            case HEAD -> ArmorSlot.HEAD;
            case CHEST -> ArmorSlot.CHEST;
            case LEGS -> ArmorSlot.LEGS;
            case FEET -> ArmorSlot.FEET;
            default -> null;
        };
    }

    /**
     * Return one of a stack.
     *
     * @param item the stack
     * @return a copy with an amount of one
     */
    private static ItemStack one(final ItemStack item) {
        return item.asOne();
    }

    /**
     * Return what is left in the hand after one of a stack went on: the rest of the stack, or
     * what was worn before when the stack was one.
     *
     * @param item the stack
     * @param worn what was worn before, or null
     * @return the item for the hand or cursor
     */
    private static ItemStack rest(final ItemStack item, final @Nullable ItemStack worn) {
        if (item.getAmount() > 1) {
            return item.asQuantity(item.getAmount() - 1);
        }

        return worn == null ? ItemStack.empty() : worn;
    }
}
