package dev.vertcode.hyperion.effect;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * The players effects hide from their viewers, for an effect with {@code hide-players}: nobody
 * stands in the middle of a crate while it opens.
 *
 * <p>The hides are made in Hyperion's name, so a vanish plugin's own are never undone. They are
 * counted for each viewer and player, so two effects over one crowd give a player back only when
 * the second ends. Safe from any thread: each hide and show runs on the viewer's own thread.
 */
public final class HiddenPlayers {

    private final Plugin plugin;
    private final Map<Pair, Integer> counts = new ConcurrentHashMap<>();

    /**
     * Create the record of hidden players.
     *
     * @param plugin Hyperion, which the hides are made in the name of
     */
    HiddenPlayers(final Plugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Hide a crowd from some viewers.
     *
     * @param viewers who the crowd is hidden from
     * @param crowd the players to hide; a viewer in it stays visible to themselves
     * @return what was hidden, to hand to {@link #show} later
     */
    List<Pair> hide(final Collection<Player> viewers, final Collection<Player> crowd) {
        List<Pair> pairs = new ArrayList<>();
        for (Player viewer : viewers) {
            for (Player hidden : crowd) {
                if (hidden.equals(viewer)) {
                    continue;
                }

                Pair pair = new Pair(viewer.getUniqueId(), hidden.getUniqueId());
                pairs.add(pair);
                if (this.counts.merge(pair, 1, Integer::sum) == 1) {
                    viewer.getScheduler().run(this.plugin, task -> viewer.hideEntity(this.plugin, hidden), null);
                }
            }
        }

        return pairs;
    }

    /**
     * Show players again to the viewers they were hidden from, unless another effect still hides
     * them.
     *
     * @param pairs what {@link #hide} returned
     */
    void show(final List<Pair> pairs) {
        for (Pair pair : pairs) {
            if (this.counts.computeIfPresent(pair, (key, count) -> count > 1 ? count - 1 : null) != null) {
                continue;
            }

            Player viewer = Bukkit.getPlayer(pair.viewer());
            Player hidden = Bukkit.getPlayer(pair.hidden());
            if (viewer != null && hidden != null) {
                viewer.getScheduler().run(this.plugin, task -> viewer.showEntity(this.plugin, hidden), null);
            }
        }
    }

    /**
     * Give back a player who leaves. The server remembers a hide by the player's id, so one left
     * in place would still hide them when they come back.
     *
     * @param player the player who leaves
     */
    public void left(final Player player) {
        UUID id = player.getUniqueId();
        for (Pair pair : List.copyOf(this.counts.keySet())) {
            if (!pair.viewer().equals(id) && !pair.hidden().equals(id)) {
                continue;
            }

            this.counts.remove(pair);
            Player viewer = Bukkit.getPlayer(pair.viewer());
            if (viewer != null && pair.hidden().equals(id)) {
                viewer.getScheduler().run(this.plugin, task -> viewer.showEntity(this.plugin, player), null);
            }
        }
    }

    /**
     * Show every hidden player again, as Hyperion disables. Nothing can be scheduled then, so it
     * runs where it is called.
     */
    void showAll() {
        for (Pair pair : List.copyOf(this.counts.keySet())) {
            this.counts.remove(pair);
            Player viewer = Bukkit.getPlayer(pair.viewer());
            Player hidden = Bukkit.getPlayer(pair.hidden());
            if (viewer != null && hidden != null) {
                viewer.showEntity(this.plugin, hidden);
            }
        }
    }

    /**
     * One player hidden from one viewer.
     *
     * @param viewer who does not see them
     * @param hidden who is hidden
     */
    record Pair(UUID viewer, UUID hidden) {
    }
}
