package dev.vertcode.hyperion.effect.part;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jspecify.annotations.Nullable;

/**
 * The players a camera holds: kept from walking and jumping, kept safe when asked, and given back
 * when the camera lets go or something else takes them away.
 *
 * <p>The hold is a temporary attribute modifier, which the server never saves, so a crash or a
 * logout mid-shot cannot leave anyone frozen. Safe from any thread: attribute changes run on the
 * player's own thread.
 */
public final class CameraLock {

    private static final List<String> MOVEMENT = List.of("movement_speed", "generic.movement_speed");
    private static final List<String> JUMP = List.of("jump_strength", "generic.jump_strength");

    private final Plugin plugin;
    private final NamespacedKey key;
    private final Map<UUID, Hold> holds = new ConcurrentHashMap<>();

    /**
     * Create the lock.
     *
     * @param plugin Hyperion, for scheduling and the modifier's key
     */
    public CameraLock(final Plugin plugin) {
        this.plugin = plugin;
        this.key = new NamespacedKey(plugin, "camera");
    }

    /**
     * Hold a player for a camera.
     *
     * @param player the player
     * @param safe whether they take no damage while held
     * @param release gives them back, from whatever takes them away
     */
    void lock(final Player player, final boolean safe, final Runnable release) {
        this.holds.put(player.getUniqueId(), new Hold(safe, release));
        player.getScheduler().run(this.plugin, task -> {
            freeze(player, MOVEMENT, this.key);
            freeze(player, JUMP, this.key);
        }, null);
    }

    /**
     * Let a player go: they walk and jump again.
     *
     * @param player the player
     */
    void unlock(final Player player) {
        this.holds.remove(player.getUniqueId());
        player.getScheduler().run(this.plugin, task -> {
            thaw(player, MOVEMENT, this.key);
            thaw(player, JUMP, this.key);
        }, null);
    }

    /**
     * Give a player back from their camera, as when they die or change worlds.
     *
     * @param player the player
     */
    public void release(final Player player) {
        Hold hold = this.holds.get(player.getUniqueId());
        if (hold != null) {
            hold.release().run();
        }
    }

    /**
     * Forget a player who left. Their client dropped the camera and the modifiers are not saved.
     *
     * @param player the player's id
     */
    public void forget(final UUID player) {
        this.holds.remove(player);
    }

    /**
     * Give every held player back, as Hyperion disables.
     */
    public void releaseAll() {
        for (Hold hold : List.copyOf(this.holds.values())) {
            hold.release().run();
        }
    }

    /**
     * Return whether a player is held and safe from damage.
     *
     * @param player the player's id
     * @return true while a safe camera holds them
     */
    public boolean safe(final UUID player) {
        Hold hold = this.holds.get(player);

        return hold != null && hold.safe();
    }

    /**
     * Return whether a camera holds a player.
     *
     * @param player the player's id
     * @return true while held
     */
    public boolean held(final UUID player) {
        return this.holds.containsKey(player);
    }

    /**
     * Bring an attribute to nothing with a modifier the server does not save.
     *
     * @param player the player
     * @param names the attribute's keys, newest first
     * @param key the modifier's key
     */
    private static void freeze(final Player player, final List<String> names, final NamespacedKey key) {
        AttributeInstance instance = instance(player, names);
        if (instance != null && instance.getModifier(key) == null) {
            instance.addTransientModifier(new AttributeModifier(key, -1d, AttributeModifier.Operation.MULTIPLY_SCALAR_1));
        }
    }

    /**
     * Take the modifier off again.
     *
     * @param player the player
     * @param names the attribute's keys, newest first
     * @param key the modifier's key
     */
    private static void thaw(final Player player, final List<String> names, final NamespacedKey key) {
        AttributeInstance instance = instance(player, names);
        if (instance != null) {
            instance.removeModifier(key);
        }
    }

    /**
     * Find an attribute by key. Its key lost the {@code generic.} part in 1.21.2, so both are
     * tried.
     *
     * @param player the player
     * @param names the keys to try
     * @return the player's attribute, or null when the server has none by those keys
     */
    private static @Nullable AttributeInstance instance(final Player player, final List<String> names) {
        for (String name : names) {
            Attribute attribute = Registry.ATTRIBUTE.get(NamespacedKey.minecraft(name));
            if (attribute != null) {
                return player.getAttribute(attribute);
            }
        }

        return null;
    }

    /**
     * Whether a held player is safe, and how to give them back.
     *
     * @param safe whether they take no damage
     * @param release gives them back
     */
    private record Hold(boolean safe, Runnable release) {
    }
}
