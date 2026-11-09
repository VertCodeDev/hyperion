package dev.vertcode.hyperion.debug;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.player.User;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;

import dev.vertcode.hyperion.render.MarkerData;
import dev.vertcode.hyperion.render.PacketMarker;

/**
 * The floating names one model or effect shows its debuggers: a text display per name, only for
 * the players whose views want it.
 *
 * <p>A draw names every label it wants with {@link #show}, then {@link #finish} takes down the
 * rest. Only the thread that owns the model or effect touches it.
 */
public final class DebugLabels {

    private final int interpolation;
    private final Map<String, Label> labels = new HashMap<>();
    private final Set<String> used = new HashSet<>();

    /**
     * Create an empty set of labels.
     *
     * @param interpolation over how many ticks a label's moves are smoothed, the redraw interval
     */
    DebugLabels(final int interpolation) {
        this.interpolation = interpolation;
    }

    /**
     * Show a label this draw, to these players only.
     *
     * @param key what the label belongs to, the same every draw
     * @param at where it floats
     * @param text what it says
     * @param users who sees it
     */
    void show(final String key, final Location at, final Component text, final List<User> users) {
        this.used.add(key);
        Label label = this.labels.get(key);
        if (label == null) {
            label = new Label(new PacketMarker(EntityTypes.TEXT_DISPLAY), text);
            this.labels.put(key, label);
        } else if (!label.text.equals(text)) {
            label.marker.update(MarkerData.text(text, true));
            label.text = text;
        }

        Set<UUID> wanted = new HashSet<>();
        for (User user : users) {
            if (user.getUUID() != null) {
                wanted.add(user.getUUID());
            }
        }

        // A player whose view went off still has the label: a marker only forgets, so take it down.
        for (UUID shown : label.marker.viewers()) {
            if (!wanted.contains(shown)) {
                label.marker.hide(shown);
            }
        }

        Component spawned = label.text;
        label.marker.place(at, users, () -> MarkerData.nametag(spawned, true, this.interpolation));
    }

    /**
     * Take down every label this draw did not show.
     *
     * @return true when some label is still up
     */
    boolean finish() {
        Iterator<Map.Entry<String, Label>> entries = this.labels.entrySet().iterator();
        while (entries.hasNext()) {
            Map.Entry<String, Label> entry = entries.next();
            if (!this.used.contains(entry.getKey())) {
                entry.getValue().marker.hideAll();
                entries.remove();
            }
        }

        this.used.clear();

        return !this.labels.isEmpty();
    }

    /**
     * Take down every label.
     */
    public void clear() {
        for (Label label : this.labels.values()) {
            label.marker.hideAll();
        }

        this.labels.clear();
        this.used.clear();
    }

    /**
     * One label: its text display and what it says now.
     */
    private static final class Label {

        private final PacketMarker marker;
        private Component text;

        private Label(final PacketMarker marker, final Component text) {
            this.marker = marker;
            this.text = text;
        }
    }
}
