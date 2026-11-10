package dev.vertcode.hyperion.armor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.jspecify.annotations.Nullable;

import dev.vertcode.hyperion.api.armor.ArmorSlot;
import dev.vertcode.hyperion.api.event.ArmorChangeCause;
import dev.vertcode.hyperion.api.event.ArmorEquipEvent;
import dev.vertcode.hyperion.api.event.ArmorSetBreakEvent;
import dev.vertcode.hyperion.api.event.ArmorSetCompleteEvent;
import dev.vertcode.hyperion.api.event.ArmorUnequipEvent;

/**
 * Keep what each player wears and fire the armor events when it changes.
 *
 * <p>Every change is worked out by comparing the player's armor slots with what they wore
 * before, so armor changed by a click, a dispenser, a command or another plugin all fire the
 * same events, each once. A listener that knows why a slot is about to change says so first
 * with {@link #expect}; otherwise the cause is {@link ArmorChangeCause#OTHER}.
 *
 * <p>{@link #update}, {@link #join} and {@link #quit} read the player's inventory, so they run on
 * the player's own thread. The snapshots are safe to read from any thread.
 */
public final class ArmorTracker {

    private static final long EXPECT_MILLIS = 1_000L;

    private final HyperionArmorRegistry registry;
    private final PluginManager events;
    private final Map<UUID, HyperionWornArmor> worn = new ConcurrentHashMap<>();
    private final Map<UUID, Map<ArmorSlot, Expected>> expected = new ConcurrentHashMap<>();

    /**
     * Create the tracker.
     *
     * @param registry finds the piece an item is
     * @param events where the events are fired
     */
    public ArmorTracker(final HyperionArmorRegistry registry, final PluginManager events) {
        this.registry = registry;
        this.events = events;
    }

    /**
     * Return what a player wears.
     *
     * @param player the player's id
     * @return the snapshot, empty for a player Hyperion has not seen
     */
    public HyperionWornArmor worn(final UUID player) {
        return this.worn.getOrDefault(player, HyperionWornArmor.empty(player));
    }

    /**
     * Note why a slot is about to change, for the event that follows.
     *
     * @param player the player's id
     * @param slot the slot
     * @param cause why it changes
     */
    public void expect(final UUID player, final ArmorSlot slot, final ArmorChangeCause cause) {
        this.expected.computeIfAbsent(player, key -> new ConcurrentHashMap<>())
                .put(slot, new Expected(cause, System.currentTimeMillis()));
    }

    /**
     * Start tracking a player who joined: what they wear counts as put on.
     *
     * @param player the player
     */
    public void join(final Player player) {
        this.worn.remove(player.getUniqueId());
        this.update(player, ArmorChangeCause.JOIN);
    }

    /**
     * Stop tracking a player who left: what they wore counts as taken off.
     *
     * @param player the player
     */
    public void quit(final Player player) {
        UUID id = player.getUniqueId();
        HyperionWornArmor before = this.worn.remove(id);
        this.expected.remove(id);
        if (before != null) {
            this.fire(player, before, HyperionWornArmor.empty(id), slot -> ArmorChangeCause.QUIT);
        }
    }

    /**
     * Compare a player's armor with what they wore and fire the events for what changed.
     *
     * @param player the player
     * @param fallback the cause of a change no listener expected
     */
    public void update(final Player player, final ArmorChangeCause fallback) {
        UUID id = player.getUniqueId();
        HyperionWornArmor before = this.worn.get(id);
        if (before == null) {
            // A change before the join event is part of joining, which reads every slot itself.
            if (fallback != ArmorChangeCause.JOIN) {
                return;
            }

            before = HyperionWornArmor.empty(id);
        }

        HyperionWornArmor now = this.read(player);
        this.worn.put(id, now);
        this.fire(player, before, now, slot -> this.cause(id, slot, fallback));
    }

    /**
     * Compare every online player's armor again after a reload, each on their own thread, so
     * pieces of a set that is gone count as taken off. A player Hyperion has not tracked yet, one
     * who was online before it enabled, counts as joining.
     *
     * @param plugin the plugin to schedule with
     * @param players the online players
     */
    public void recheck(final Plugin plugin, final Collection<? extends Player> players) {
        for (Player player : players) {
            player.getScheduler().run(plugin, task -> {
                boolean tracked = this.worn.containsKey(player.getUniqueId());
                this.update(player, tracked ? ArmorChangeCause.OTHER : ArmorChangeCause.JOIN);
            }, null);
        }
    }

    /**
     * Read a player's armor slots.
     *
     * @param player the player
     * @return the snapshot
     */
    private HyperionWornArmor read(final Player player) {
        EntityEquipment equipment = player.getEquipment();
        Map<ArmorSlot, HyperionArmorPiece> pieces = new EnumMap<>(ArmorSlot.class);
        Map<ArmorSlot, ItemStack> items = new EnumMap<>(ArmorSlot.class);
        for (ArmorSlot slot : ArmorSlot.values()) {
            ItemStack item = equipment.getItem(slot.equipmentSlot());
            HyperionArmorPiece piece = this.registry.piece(item);
            if (piece != null && piece.slot() == slot) {
                pieces.put(slot, piece);
                items.put(slot, item);
            }
        }

        return new HyperionWornArmor(player.getUniqueId(), pieces, items);
    }

    /**
     * Fire the events between two snapshots: pieces taken off, sets broken, pieces put on, sets
     * completed, in that order.
     *
     * @param player the player
     * @param before what they wore
     * @param now what they wear
     * @param causes tells why each slot changed
     */
    private void fire(
            final Player player,
            final HyperionWornArmor before,
            final HyperionWornArmor now,
            final Function<ArmorSlot, ArmorChangeCause> causes
    ) {
        List<Event> off = new ArrayList<>();
        List<Event> on = new ArrayList<>();
        Map<ArmorSlot, ArmorChangeCause> slotCauses = new EnumMap<>(ArmorSlot.class);
        for (ArmorSlot slot : ArmorSlot.values()) {
            HyperionArmorPiece old = before.at(slot);
            HyperionArmorPiece current = now.at(slot);
            if (same(old, current)) {
                continue;
            }

            ArmorChangeCause cause = causes.apply(slot);
            slotCauses.put(slot, cause);
            if (old != null) {
                off.add(new ArmorUnequipEvent(player, old, before.item(slot), cause));
            }

            if (current != null) {
                on.add(new ArmorEquipEvent(player, current, now.item(slot), cause));
            }
        }

        if (slotCauses.isEmpty()) {
            return;
        }

        ArmorChangeCause setCause = slotCauses.values().iterator().next();
        List<Event> broken = new ArrayList<>();
        List<Event> completed = new ArrayList<>();
        for (HyperionArmorSet set : sets(before, now).values()) {
            boolean wasFull = before.hasFullSet(set);
            boolean isFull = now.hasFullSet(set);
            if (wasFull && !isFull) {
                broken.add(new ArmorSetBreakEvent(player, set, setCause));
            } else if (!wasFull && isFull) {
                completed.add(new ArmorSetCompleteEvent(player, set, setCause));
            }
        }

        off.forEach(this.events::callEvent);
        broken.forEach(this.events::callEvent);
        on.forEach(this.events::callEvent);
        completed.forEach(this.events::callEvent);
    }

    /**
     * Take the expected cause of a slot's change, if it is recent.
     *
     * @param player the player's id
     * @param slot the slot
     * @param fallback the cause when none was expected
     * @return the cause
     */
    private ArmorChangeCause cause(final UUID player, final ArmorSlot slot, final ArmorChangeCause fallback) {
        Map<ArmorSlot, Expected> slots = this.expected.get(player);
        Expected expectedCause = slots == null ? null : slots.remove(slot);
        if (expectedCause == null || System.currentTimeMillis() - expectedCause.at() > EXPECT_MILLIS) {
            return fallback;
        }

        return expectedCause.cause();
    }

    /**
     * Return every set worn in either snapshot, by key; the newer set when both have it.
     *
     * @param before the older snapshot
     * @param now the newer snapshot
     * @return the sets
     */
    private static Map<String, HyperionArmorSet> sets(final HyperionWornArmor before, final HyperionWornArmor now) {
        Map<String, HyperionArmorSet> sets = new LinkedHashMap<>();
        for (HyperionWornArmor snapshot : List.of(before, now)) {
            for (ArmorSlot slot : ArmorSlot.values()) {
                HyperionArmorPiece piece = snapshot.at(slot);
                if (piece != null) {
                    sets.put(piece.set().key().asString(), piece.set());
                }
            }
        }

        return sets;
    }

    /**
     * Return whether two pieces are the same piece of the same set.
     *
     * @param first one piece, or null
     * @param second the other, or null
     * @return true when both are null or the same set and slot
     */
    private static boolean same(final @Nullable HyperionArmorPiece first, final @Nullable HyperionArmorPiece second) {
        if (first == null || second == null) {
            return first == second;
        }

        return first.slot() == second.slot() && Objects.equals(first.set().key(), second.set().key());
    }

    /**
     * A cause a listener expects for a slot's next change.
     *
     * @param cause the cause
     * @param at when it was noted
     */
    private record Expected(ArmorChangeCause cause, long at) {
    }
}
