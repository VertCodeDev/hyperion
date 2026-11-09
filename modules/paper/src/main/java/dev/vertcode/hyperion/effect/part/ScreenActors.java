package dev.vertcode.hyperion.effect.part;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.joml.Vector3f;

import dev.vertcode.hyperion.api.effect.EffectActor;
import dev.vertcode.hyperion.api.effect.EffectContext;
import dev.vertcode.hyperion.api.effect.EffectFrame;

/**
 * The parts that act on each viewer's screen at a keyframe: titles, the action bar, chat,
 * console commands and the sky's time.
 *
 * <p>Every text is MiniMessage, with the effect's {@code %parameters%} already filled in, then
 * {@code %player%} for each viewer.
 */
final class ScreenActors {

    private static final long MILLIS_PER_TICK = 50L;
    private static final int FADE_IN = 10;
    private static final int STAY = 70;
    private static final int FADE_OUT = 20;

    private ScreenActors() {
    }

    /**
     * Fill in a viewer's name and read MiniMessage.
     *
     * @param text the text
     * @param viewer the viewer
     * @return the component
     */
    static Component render(final String text, final Player viewer) {
        return MiniMessage.miniMessage().deserialize(fill(text, viewer));
    }

    /**
     * Fill in a viewer's name.
     *
     * @param text the text
     * @param viewer the viewer
     * @return the filled text
     */
    static String fill(final String text, final Player viewer) {
        return text.replace("%player%", viewer.getName());
    }

    /**
     * A title and subtitle. Values: {@code title}, {@code subtitle}, and {@code fade} as
     * {@code [in, stay, out]} in ticks.
     */
    static final class TitleActor implements EffectActor {

        private final EffectContext context;

        TitleActor(final EffectContext context) {
            this.context = context;
        }

        @Override
        public void frame(final int tick, final EffectFrame frame) {
            String title = frame.string("title", "");
            String subtitle = frame.string("subtitle", "");
            Vector3f fade = frame.vector("fade");
            Title.Times times = Title.Times.times(
                    ticks(fade == null ? FADE_IN : fade.x),
                    ticks(fade == null ? STAY : fade.y),
                    ticks(fade == null ? FADE_OUT : fade.z)
            );
            for (Player viewer : this.context.viewers()) {
                viewer.showTitle(Title.title(render(title, viewer), render(subtitle, viewer), times));
            }
        }

        private static Duration ticks(final float ticks) {
            return Duration.ofMillis(Math.max(0L, Math.round(ticks) * MILLIS_PER_TICK));
        }
    }

    /**
     * A line above the hotbar. Values: {@code text}.
     */
    static final class ActionbarActor implements EffectActor {

        private final EffectContext context;

        ActionbarActor(final EffectContext context) {
            this.context = context;
        }

        @Override
        public void frame(final int tick, final EffectFrame frame) {
            String text = frame.string("text", "");
            for (Player viewer : this.context.viewers()) {
                viewer.sendActionBar(render(text, viewer));
            }
        }
    }

    /**
     * A chat message. Values: {@code text}.
     */
    static final class MessageActor implements EffectActor {

        private final EffectContext context;

        MessageActor(final EffectContext context) {
            this.context = context;
        }

        @Override
        public void frame(final int tick, final EffectFrame frame) {
            String text = frame.string("text", "");
            for (Player viewer : this.context.viewers()) {
                viewer.sendMessage(render(text, viewer));
            }
        }
    }

    /**
     * A console command, run on the global region: once per viewer when it holds a placeholder,
     * such as {@code %player%}, else once. Values: {@code command}, without the slash.
     */
    static final class CommandActor implements EffectActor {

        private final EffectContext context;
        private final PartServices services;

        CommandActor(final EffectContext context, final PartServices services) {
            this.context = context;
            this.services = services;
        }

        @Override
        public void frame(final int tick, final EffectFrame frame) {
            String command = frame.string("command", "").trim();
            if (command.startsWith("/")) {
                command = command.substring(1);
            }

            if (command.isEmpty()) {
                return;
            }

            if (command.indexOf('%') < 0) {
                this.run(command);
                return;
            }

            for (Player viewer : this.context.viewers()) {
                this.run(fill(command, viewer));
            }
        }

        private void run(final String command) {
            Bukkit.getGlobalRegionScheduler().execute(
                    this.services.plugin(),
                    () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command)
            );
        }
    }

    /**
     * The sky's time for each viewer only, put back when the effect ends. Values: {@code time},
     * 0 to 24000, or {@code reset}.
     */
    static final class TimeActor implements EffectActor {

        private final EffectContext context;
        private final Set<UUID> changed = new HashSet<>();

        TimeActor(final EffectContext context) {
            this.context = context;
        }

        @Override
        public void frame(final int tick, final EffectFrame frame) {
            boolean reset = frame.string("time", "reset").equals("reset");
            long time = Math.round(frame.number("time", 0f));
            for (Player viewer : this.context.viewers()) {
                if (reset) {
                    viewer.resetPlayerTime();
                    this.changed.remove(viewer.getUniqueId());
                } else {
                    viewer.setPlayerTime(time, false);
                    this.changed.add(viewer.getUniqueId());
                }
            }
        }

        @Override
        public void stop() {
            for (UUID id : this.changed) {
                Player viewer = Bukkit.getPlayer(id);
                if (viewer != null) {
                    viewer.resetPlayerTime();
                }
            }

            this.changed.clear();
        }
    }
}
