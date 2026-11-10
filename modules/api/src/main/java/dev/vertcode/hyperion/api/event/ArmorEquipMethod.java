package dev.vertcode.hyperion.api.event;

/**
 * How a player tries to put on a piece of armor.
 */
public enum ArmorEquipMethod {

    /**
     * Right-clicking with the piece in hand.
     */
    RIGHT_CLICK,

    /**
     * Placing the piece in an armor slot with the cursor.
     */
    INVENTORY_CLICK,

    /**
     * Shift-clicking the piece from the inventory into its slot.
     */
    SHIFT_CLICK,

    /**
     * Dragging the piece across its slot.
     */
    DRAG,

    /**
     * Pressing a hotbar number, or the offhand key, over an armor slot.
     */
    HOTBAR_SWAP,

    /**
     * A dispenser in front of the player.
     */
    DISPENSER
}
