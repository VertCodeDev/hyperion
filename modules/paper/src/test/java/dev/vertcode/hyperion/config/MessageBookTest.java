package dev.vertcode.hyperion.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static dev.vertcode.hyperion.config.HyperionMessages.value;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MessageBookTest {

    private YamlConfiguration bundled;
    private Logger logger;

    @BeforeEach
    void read() throws IOException {
        try (InputStream stream = Objects.requireNonNull(getClass().getResourceAsStream("/" + MessageBook.FILE));
             Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            this.bundled = YamlConfiguration.loadConfiguration(reader);
        }

        this.logger = Logger.getAnonymousLogger();
        this.logger.setLevel(Level.OFF);
    }

    @Test
    void theBundledFileHasEveryMessage() {
        for (HyperionMessages message : HyperionMessages.values()) {
            assertTrue(this.bundled.isString(message.path()), "messages.yml lacks " + message.path());
        }
    }

    @Test
    void theBundledFileHasNoLongDashes() throws IOException {
        try (InputStream stream = Objects.requireNonNull(getClass().getResourceAsStream("/" + MessageBook.FILE))) {
            String text = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            assertFalse(text.contains("\u2014") || text.contains("\u2013"));
        }
    }

    @Test
    void fillsInValuesAndPaletteColors() {
        MessageBook book = MessageBook.of(this.bundled, this.logger);
        Component heading = book.render(HyperionMessages.LIST__HEADING.path(), value("count", 3));

        assertEquals("Models (3)", plain(heading));
        assertEquals(TextColor.color(0x9B6BFF), colorOf(heading, "Models"));
    }

    @Test
    void putsThePrefixInFront() {
        MessageBook book = MessageBook.of(this.bundled, this.logger);
        Component message = book.render(HyperionMessages.MODEL_NOT_LOADED.path(), value("model", "hyperion:dragon"));

        assertEquals("Hyperion » No model is loaded as hyperion:dragon.", plain(message));
    }

    @Test
    void showsTagsInsideValuesAsText() {
        MessageBook book = MessageBook.of(this.bundled, this.logger);
        Component message = book.render(HyperionMessages.LIST__NO_MATCH.path(), value("filter", "<red>boss"));

        assertTrue(plain(message).contains("'<red>boss'"));
    }

    @Test
    void fallsBackOnAnUnreadableColor() {
        this.bundled.set("palette.error", "not a color");
        this.bundled.set("palette.success", "aqua");
        MessageBook book = MessageBook.of(this.bundled, this.logger);

        assertEquals(NamedTextColor.RED, book.palette().error());
        assertEquals(NamedTextColor.AQUA, book.palette().success());
    }

    @Test
    void rendersAMissingMessageAsItsKey() {
        this.bundled.set(HyperionMessages.RELOAD__STARTED.path(), null);
        MessageBook book = MessageBook.of(this.bundled, this.logger);

        assertEquals("reload.started", plain(book.render(HyperionMessages.RELOAD__STARTED.path())));
    }

    private static @Nullable TextColor colorOf(final Component component, final String text) {
        if (component instanceof TextComponent textComponent && textComponent.content().contains(text)) {
            return component.color();
        }

        for (Component child : component.children()) {
            TextColor color = colorOf(child, text);
            if (color != null) {
                return color;
            }
        }

        return null;
    }

    private static String plain(final Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }
}
