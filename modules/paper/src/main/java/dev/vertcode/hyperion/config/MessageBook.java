package dev.vertcode.hyperion.config;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;

/**
 * Every message from {@code messages.yml}, ready to render.
 *
 * <p>Immutable once read, so any thread may render from it; a reload builds a new book. The
 * palette colors and the prefix are tags every message can use: {@code <primary>},
 * {@code <prefix>} and so on.
 */
public final class MessageBook {

    /**
     * The file name, in the data folder and in the jar.
     */
    public static final String FILE = "messages.yml";

    private static final String BAD_COLOR = "messages.yml: '%s' is not a color for palette.%s, using %s";

    private final Map<String, String> templates;
    private final MessagePalette palette;
    private final TagResolver tags;

    private MessageBook(final Map<String, String> templates, final MessagePalette palette, final TagResolver tags) {
        this.templates = Map.copyOf(templates);
        this.palette = palette;
        this.tags = tags;
    }

    /**
     * Read {@code messages.yml} from the data folder, writing it from the jar when missing and
     * adding any key a newer jar has. Keys the server changed are never touched.
     *
     * @param plugin the plugin whose data folder and jar to read
     * @return the messages
     */
    public static MessageBook load(final Plugin plugin) {
        return of(BundledYaml.load(plugin, FILE), plugin.getLogger());
    }

    /**
     * Build the messages from a loaded file. A message the file lacks renders as its key, so a
     * missing line is easy to spot.
     *
     * @param config the contents of {@code messages.yml}
     * @param logger where an unreadable palette color is reported
     * @return the messages
     */
    public static MessageBook of(final ConfigurationSection config, final Logger logger) {
        MessagePalette palette = new MessagePalette(
                color(config, "primary", TextColor.color(0x9B6BFF), logger),
                color(config, "highlight", TextColor.color(0xC9B2FF), logger),
                color(config, "text", NamedTextColor.WHITE, logger),
                color(config, "muted", NamedTextColor.GRAY, logger),
                color(config, "dim", NamedTextColor.DARK_GRAY, logger),
                color(config, "success", NamedTextColor.GREEN, logger),
                color(config, "warning", NamedTextColor.YELLOW, logger),
                color(config, "error", NamedTextColor.RED, logger)
        );
        TagResolver colors = TagResolver.resolver(
                Placeholder.styling("primary", palette.primary()),
                Placeholder.styling("highlight", palette.highlight()),
                Placeholder.styling("text", palette.text()),
                Placeholder.styling("muted", palette.muted()),
                Placeholder.styling("dim", palette.dim()),
                Placeholder.styling("success", palette.success()),
                Placeholder.styling("warning", palette.warning()),
                Placeholder.styling("error", palette.error())
        );
        Component prefix = MiniMessage.miniMessage().deserialize(config.getString("prefix", ""), colors);
        TagResolver tags = TagResolver.resolver(colors, Placeholder.component("prefix", prefix));

        Map<String, String> templates = new HashMap<>();
        for (HyperionMessages message : HyperionMessages.values()) {
            templates.put(message.path(), config.getString(message.path(), message.path()));
        }

        return new MessageBook(templates, palette, tags);
    }

    /**
     * Render a message.
     *
     * @param path the message's key, such as {@code reload.done}
     * @param placeholders the values the message fills in
     * @return the message
     */
    public Component render(final String path, final TagResolver... placeholders) {
        String template = this.templates.getOrDefault(path, path);

        TagResolver resolver = TagResolver.resolver(this.tags, TagResolver.resolver(placeholders));

        return MiniMessage.miniMessage().deserialize(template, resolver);
    }

    /**
     * Return the palette, for output Hyperion does not write itself, such as the help menu.
     *
     * @return the colors
     */
    public MessagePalette palette() {
        return this.palette;
    }

    /**
     * Read one palette color: a hex code such as {@code #9B6BFF} or a name such as
     * {@code gray}.
     *
     * @param config the contents of {@code messages.yml}
     * @param name the palette entry
     * @param fallback the color when the entry is missing or unreadable
     * @param logger where an unreadable color is reported
     * @return the color
     */
    private static TextColor color(
            final ConfigurationSection config,
            final String name,
            final TextColor fallback,
            final Logger logger
    ) {
        String value = config.getString("palette." + name);
        if (value == null) {
            return fallback;
        }

        TextColor hex = TextColor.fromHexString(value.trim());
        if (hex != null) {
            return hex;
        }

        NamedTextColor named = NamedTextColor.NAMES.value(value.trim().toLowerCase(Locale.ROOT));
        if (named != null) {
            return named;
        }

        logger.warning(String.format(BAD_COLOR, value, name, fallback.asHexString()));

        return fallback;
    }
}
