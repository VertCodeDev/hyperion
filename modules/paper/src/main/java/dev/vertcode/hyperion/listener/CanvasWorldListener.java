package dev.vertcode.hyperion.listener;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityEvent;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.plugin.Plugin;
import org.jspecify.annotations.Nullable;

/**
 * The world change and respawn hooks on Canvas, which never fires {@code PlayerChangedWorldEvent}
 * or the respawn events and fires its own post teleport, post portal and post respawn events
 * instead. Without them a player who moves into a copy of the world, or respawns, keeps counting
 * as a viewer of every model near them while their client has dropped all of it, so those models
 * stay invisible until the player walks out of range and back.
 *
 * <p>Hyperion compiles against plain Paper, so the Canvas events are looked up by name and
 * registered only when the server has them. Each fires on the player's region once they arrived.
 */
public final class CanvasWorldListener implements Listener {

    private static final String PACKAGE = "io.canvasmc.canvas.event.";
    private static final String TELEPORT = PACKAGE + "EntityPostTeleportAsyncEvent";
    private static final String PORTAL = PACKAGE + "EntityPostPortalAsyncEvent";
    private static final String RESPAWN = PACKAGE + "PlayerPostRespawnAsyncEvent";
    private static final String BROKEN = "Could not read a Canvas %s event";

    private final Consumer<Player> changedWorld;
    private final Consumer<Player> respawned;
    private final Logger logger;

    private CanvasWorldListener(final Consumer<Player> changedWorld, final Consumer<Player> respawned, final Logger logger) {
        this.changedWorld = changedWorld;
        this.respawned = respawned;
        this.logger = logger;
    }

    /**
     * Listen to Canvas' post teleport, post portal and post respawn events, when the server has
     * them. Does nothing on Paper and Folia, where the Bukkit events do the work.
     *
     * @param plugin the plugin to register for
     * @param changedWorld told when a player arrived in another world
     * @param respawned told when a player respawned
     * @return whether the server is Canvas and the hooks are on
     */
    public static boolean register(final Plugin plugin, final Consumer<Player> changedWorld, final Consumer<Player> respawned) {
        @Nullable Class<? extends Event> teleport = find(TELEPORT);
        @Nullable Class<? extends Event> portal = find(PORTAL);
        @Nullable Class<? extends Event> respawn = find(RESPAWN);
        if (teleport == null && portal == null && respawn == null) {
            return false;
        }

        CanvasWorldListener listener = new CanvasWorldListener(changedWorld, respawned, plugin.getLogger());
        if (teleport != null) {
            @Nullable MethodHandle from = getter(teleport, "getFrom", Location.class);
            @Nullable MethodHandle to = getter(teleport, "getTo", Location.class);
            listener.on(plugin, teleport, event -> listener.teleported(event, from, to));
        }

        if (portal != null) {
            @Nullable MethodHandle from = getter(portal, "getFrom", World.class);
            @Nullable MethodHandle to = getter(portal, "getTo", World.class);
            listener.on(plugin, portal, event -> listener.portalled(event, from, to));
        }

        if (respawn != null) {
            listener.on(plugin, respawn, listener::respawn);
        }

        return true;
    }

    private void on(final Plugin plugin, final Class<? extends Event> type, final Consumer<Event> handler) {
        plugin.getServer().getPluginManager().registerEvent(type, this, EventPriority.MONITOR, (ignored, event) -> {
            if (type.isInstance(event)) {
                handler.accept(event);
            }
        }, plugin);
    }

    private void teleported(final Event event, final @Nullable MethodHandle from, final @Nullable MethodHandle to) {
        if (!(event instanceof EntityEvent moved) || !(moved.getEntity() instanceof Player player)) {
            return;
        }

        try {
            @Nullable Location was = from == null ? null : (Location) from.invoke(event);
            @Nullable Location now = to == null ? null : (Location) to.invoke(event);

            // Unknown ends count as a change: forgetting the player once too often only resends.
            if (was == null || now == null || !Objects.equals(was.getWorld(), now.getWorld())) {
                this.changedWorld.accept(player);
            }
        } catch (Throwable broken) {
            this.logger.log(Level.WARNING, String.format(BROKEN, "teleport"), broken);
        }
    }

    private void portalled(final Event event, final @Nullable MethodHandle from, final @Nullable MethodHandle to) {
        if (!(event instanceof EntityEvent moved) || !(moved.getEntity() instanceof Player player)) {
            return;
        }

        try {
            @Nullable Object was = from == null ? null : from.invoke(event);
            @Nullable Object now = to == null ? null : to.invoke(event);
            if (was == null || now == null || !was.equals(now)) {
                this.changedWorld.accept(player);
            }
        } catch (Throwable broken) {
            this.logger.log(Level.WARNING, String.format(BROKEN, "portal"), broken);
        }
    }

    private void respawn(final Event event) {
        if (event instanceof PlayerEvent respawn) {
            this.respawned.accept(respawn.getPlayer());
        }
    }

    @SuppressWarnings("unchecked")
    private static @Nullable Class<? extends Event> find(final String name) {
        try {
            Class<?> found = Class.forName(name, false, CanvasWorldListener.class.getClassLoader());

            return Event.class.isAssignableFrom(found) ? (Class<? extends Event>) found : null;
        } catch (ClassNotFoundException | LinkageError absent) {
            return null;
        }
    }

    private static @Nullable MethodHandle getter(final Class<?> type, final String name, final Class<?> returns) {
        try {
            return MethodHandles.publicLookup().findVirtual(type, name, MethodType.methodType(returns));
        } catch (NoSuchMethodException | IllegalAccessException absent) {
            return null;
        }
    }
}
