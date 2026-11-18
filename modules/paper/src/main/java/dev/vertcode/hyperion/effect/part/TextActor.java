package dev.vertcode.hyperion.effect.part;

import java.util.List;
import java.util.Locale;

import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import net.kyori.adventure.text.minimessage.MiniMessage;

import dev.vertcode.hyperion.api.effect.EffectContext;
import dev.vertcode.hyperion.api.effect.EffectFrame;
import dev.vertcode.hyperion.render.PropData;

/**
 * A text prop.
 *
 * <p>Values: {@code text} in MiniMessage, with the server's PlaceholderAPI placeholders, {@code background} as {@code #AARRGGBB},
 * {@code shadow}, {@code see-through}, {@code line-width}, {@code opacity} (0 to 255) and
 * {@code alignment} ({@code left}, {@code center}, {@code right}).
 *
 * <p>The background can fade and breathe: {@code alpha} (0 to 1) scales how solid it is and
 * eases between keyframes like any number, and {@code pulse} is a period in ticks over which it
 * dims to {@code pulse-floor} (0.3 of itself unless set) and back. Both change the background at
 * most every {@link #STEP} ticks.
 */
final class TextActor extends DisplayActor {

    private static final int DEFAULT_LINE_WIDTH = 200;
    private static final int DEFAULT_BACKGROUND = 0x40000000;
    private static final float DEFAULT_PULSE_FLOOR = 0.3f;
    private static final int ALPHA_SHIFT = 24;
    private static final int RGB = 0xFFFFFF;

    private int background = DEFAULT_BACKGROUND;
    private boolean shaded;

    /**
     * Create a text prop.
     *
     * @param context the effect it plays in
     */
    TextActor(final EffectContext context) {
        super(context, EntityTypes.TEXT_DISPLAY);
    }

    @Override
    public void frame(final int tick, final EffectFrame frame) {
        // A fade or pulse changes the background every tick; sending it every other is enough.
        if (!this.shaded || tick % STEP == 0) {
            this.background = background(tick, frame);
            this.shaded = true;
        }

        super.frame(tick, frame);
    }

    @Override
    protected void content(final List<EntityData<?>> into, final EffectFrame frame) {
        String alignment = frame.string("alignment", "center").toLowerCase(Locale.ROOT);
        PropData.TextStyle style = new PropData.TextStyle(
                Math.round(frame.number("line-width", DEFAULT_LINE_WIDTH)),
                this.background,
                (byte) Math.clamp(Math.round(frame.number("opacity", 255f)), 0, 255),
                frame.bool("shadow", false),
                frame.bool("see-through", false),
                alignment.equals("left") ? -1 : alignment.equals("right") ? 1 : 0
        );
        PropData.text(into, MiniMessage.miniMessage().deserialize(text(frame)), style);
    }

    @Override
    protected String contentKey(final EffectFrame frame) {
        return text(frame) + '\u0000' + this.background + '\u0000' + frame.number("opacity", 255f);
    }

    /**
     * Work out the background at a tick: its color, made less solid by {@code alpha} and by where
     * the {@code pulse} is.
     *
     * @param tick the effect's tick
     * @param frame the part's values
     * @return the background as ARGB
     */
    static int background(final int tick, final EffectFrame frame) {
        int color = frame.string("background").map(TextActor::argb).orElse(DEFAULT_BACKGROUND);
        float solid = Math.clamp(frame.number("alpha", 1f), 0f, 1f) * pulse(tick, frame);
        if (solid >= 1f) {
            return color;
        }

        return Math.round((color >>> ALPHA_SHIFT) * solid) << ALPHA_SHIFT | (color & RGB);
    }

    /**
     * Work out how solid a {@code pulse} leaves the background at a tick.
     *
     * @param tick the effect's tick
     * @param frame the part's values
     * @return 1 at the top of the pulse and for a text that does not pulse, down to its floor
     */
    private static float pulse(final int tick, final EffectFrame frame) {
        int period = Math.round(frame.number("pulse", 0f));
        if (period <= 0) {
            return 1f;
        }

        float floor = Math.clamp(frame.number("pulse-floor", DEFAULT_PULSE_FLOOR), 0f, 1f);
        double phase = (tick % period) / (double) period;
        double wave = 0.5d + 0.5d * Math.cos(phase * 2d * Math.PI);

        return (float) (floor + (1f - floor) * wave);
    }

    /**
     * Return the text, with the server's PlaceholderAPI placeholders filled in. Every viewer sees
     * the same prop, so placeholders of one player stay as written.
     *
     * @param frame the part's values
     * @return the text
     */
    private static String text(final EffectFrame frame) {
        return Placeholders.apply(null, frame.string("text", ""));
    }

    /**
     * Read a color with its alpha, {@code #AARRGGBB}, or {@code #RRGGBB} for an opaque one.
     *
     * @param text the color
     * @return the color as ARGB, the vanilla name background when it cannot be read
     */
    static int argb(final String text) {
        String hex = text.trim().startsWith("#") ? text.trim().substring(1) : text.trim();
        try {
            long value = Long.parseLong(hex, 16);
            return hex.length() <= 6 ? (int) (0xFF000000L | value) : (int) value;
        } catch (NumberFormatException exception) {
            return DEFAULT_BACKGROUND;
        }
    }
}
