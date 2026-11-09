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
 * <p>Values: {@code text} in MiniMessage, {@code background} as {@code #AARRGGBB},
 * {@code shadow}, {@code see-through}, {@code line-width}, {@code opacity} (0 to 255) and
 * {@code alignment} ({@code left}, {@code center}, {@code right}).
 */
final class TextActor extends DisplayActor {

    private static final int DEFAULT_LINE_WIDTH = 200;
    private static final int DEFAULT_BACKGROUND = 0x40000000;

    /**
     * Create a text prop.
     *
     * @param context the effect it plays in
     */
    TextActor(final EffectContext context) {
        super(context, EntityTypes.TEXT_DISPLAY);
    }

    @Override
    protected void content(final List<EntityData<?>> into, final EffectFrame frame) {
        String alignment = frame.string("alignment", "center").toLowerCase(Locale.ROOT);
        PropData.TextStyle style = new PropData.TextStyle(
                Math.round(frame.number("line-width", DEFAULT_LINE_WIDTH)),
                frame.string("background").map(TextActor::argb).orElse(DEFAULT_BACKGROUND),
                (byte) Math.clamp(Math.round(frame.number("opacity", 255f)), 0, 255),
                frame.bool("shadow", false),
                frame.bool("see-through", false),
                alignment.equals("left") ? -1 : alignment.equals("right") ? 1 : 0
        );
        PropData.text(into, MiniMessage.miniMessage().deserialize(text(frame)), style);
    }

    @Override
    protected String contentKey(final EffectFrame frame) {
        return text(frame) + '\u0000' + frame.string("background", "") + '\u0000' + frame.number("opacity", 255f);
    }

    /**
     * Return the text.
     *
     * @param frame the part's values
     * @return the text
     */
    private static String text(final EffectFrame frame) {
        return frame.string("text", "");
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
