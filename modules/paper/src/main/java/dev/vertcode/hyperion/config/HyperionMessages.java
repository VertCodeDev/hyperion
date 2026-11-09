package dev.vertcode.hyperion.config;

import java.util.Locale;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.jspecify.annotations.Nullable;

/**
 * Every message in {@code messages.yml}, one constant per line of the file.
 *
 * <p>A constant's key follows its name: {@code __} is a dot and {@code _} a dash, so
 * {@code RELOAD__MODEL_DONE} is {@code reload.model-done}. Values are passed by name:
 *
 * <pre>{@code
 * HyperionMessages.RELOAD__DONE.send(sender, value("loaded", 40), value("time", 312));
 * }</pre>
 *
 * <p>The messages are installed on enable and replaced on {@code /hy reload}; any thread may
 * render them.
 */
public enum HyperionMessages {

    MODEL_NOT_LOADED,

    LIST__HEADING,
    LIST__ENTRY,
    LIST__ENTRY_HOVER,
    LIST__FOOTER,
    LIST__PREVIOUS,
    LIST__PREVIOUS_NONE,
    LIST__NEXT,
    LIST__NEXT_NONE,
    LIST__PAGE_HOVER,
    LIST__NO_PAGE,
    LIST__EMPTY,
    LIST__NO_MATCH,

    INFO__HEADING,
    INFO__SOURCE,
    INFO__SOURCE_PLUGIN,
    INFO__FORMAT,
    INFO__SIZE,
    INFO__TEXTURES,
    INFO__BONES,
    INFO__BONE,
    INFO__BONE_WITHOUT_CUBES,
    INFO__BONE_TAGS,
    INFO__BONE_HIDDEN,
    INFO__MORE_BONES,
    INFO__ANIMATIONS,
    INFO__ANIMATION,
    INFO__LOCATORS,
    INFO__LOCATOR,
    INFO__LOCATOR_ON_BONE,
    INFO__WARNINGS,
    INFO__WARNINGS_HOVER,

    RELOAD__STARTED,
    RELOAD__DONE,
    RELOAD__CHANGED,
    RELOAD__REMOVED,
    RELOAD__FILE_FAILED,
    RELOAD__MODEL_DONE,
    RELOAD__FAILED,

    VALIDATE__HEADING,
    VALIDATE__ENTRY_ERRORS,
    VALIDATE__ENTRY_WARNINGS,
    VALIDATE__ENTRY_HOVER,
    VALIDATE__ALL_CLEAN,
    VALIDATE__MODEL_CLEAN,
    VALIDATE__MODEL_HEADING,
    VALIDATE__WARNING,
    VALIDATE__ERROR,
    VALIDATE__MORE,
    VALIDATE__EFFECTS_HEADING,
    VALIDATE__EFFECT_FILE,

    PACK__PROMPT,
    PACK__KICK,
    PACK__BUILDING,
    PACK__BUILT,
    PACK__BUILD_FAILED,
    PACK__NOT_BUILT,
    PACK__SENDING_OFF,
    PACK__SENT,
    PACK__EXPORTED,
    PACK__EXPORT_FAILED,
    PACK__STATUS_HEADING,
    PACK__STATUS_HOSTING,
    PACK__STATUS_URL,
    PACK__STATUS_PACK,
    PACK__STATUS_PLAYERS,

    SPAWN__SPAWNED,
    SPAWN__ATTACHED,
    SPAWN__NO_TARGET,
    SPAWN__FAILED,
    SPAWN__PACK_NOT_BUILT,
    SPAWN__SPAWNED_ON_MOB,
    SPAWN__BAD_ENTITY,

    REMOVE__REMOVED,
    REMOVE__NONE,
    REMOVE__GONE,

    NEAR__HEADING,
    NEAR__ENTRY,
    NEAR__ENTRY_HOVER,
    NEAR__ON_ENTITY,
    NEAR__STANDING,
    NEAR__NONE,

    ANIMATE__PLAYING,
    ANIMATE__STOPPED,
    ANIMATE__STOPPED_ALL,
    ANIMATE__NO_MODEL,
    ANIMATE__NO_ANIMATION,
    EFFECT__PLAYING,
    EFFECT__ON_MODEL,
    EFFECT__HERE,
    EFFECT__NO_EFFECT,
    EFFECT__STOPPED,
    EFFECT__HEADING,
    EFFECT__ENTRY,
    EFFECT__ENTRY_LOOP,
    EFFECT__ENTRY_HOVER,
    EFFECT__EMPTY,

    MENU__TITLE,
    MENU__ANIMATION,
    MENU__ANIMATION_LENGTH,
    MENU__ANIMATION_PLAYING,
    MENU__ANIMATION_IDLE,
    MENU__HINT_ONCE,
    MENU__HINT_LOOP,
    MENU__HINT_STOP,
    MENU__FROZEN,
    MENU__MOVING,
    MENU__FREEZE_HINT,
    MENU__CANNOT_FREEZE,
    MENU__AUTO_ON,
    MENU__AUTO_OFF,
    MENU__AUTO_HINT,
    MENU__STOP_ALL,
    MENU__SMALLER,
    MENU__BIGGER,
    MENU__SCALE,
    MENU__GLOW_ON,
    MENU__GLOW_OFF,
    MENU__GLOW_HINT,
    MENU__REMOVE,
    MENU__REMOVE_HINT,
    MENU__PREVIOUS,
    MENU__NEXT,
    MENU__UNAVAILABLE,

    COMMAND__USAGE,
    COMMAND__NO_PERMISSION,
    COMMAND__PLAYERS_ONLY,
    COMMAND__INVALID_ARGUMENT,
    COMMAND__FAILED,

    HELP__TITLE,
    HELP__COMMAND,
    HELP__DESCRIPTION,
    HELP__NO_DESCRIPTION,
    HELP__ARGUMENTS,
    HELP__OPTIONAL,
    HELP__SHOWING_RESULTS,
    HELP__NO_RESULTS,
    HELP__AVAILABLE_COMMANDS,
    HELP__CLICK_TO_SHOW_HELP,
    HELP__PAGE_OUT_OF_RANGE,
    HELP__NEXT_PAGE,
    HELP__PREVIOUS_PAGE;

    private static final String NOT_LOADED = "Hyperion's messages are not loaded yet";

    private static volatile @Nullable MessageBook book;

    private final String path;

    HyperionMessages() {
        this.path = this.name().toLowerCase(Locale.ROOT).replace("__", ".").replace('_', '-');
    }

    /**
     * Use a new set of messages from now on.
     *
     * @param messages the messages, as loaded from {@code messages.yml}
     */
    public static void install(final MessageBook messages) {
        book = messages;
    }

    /**
     * Return the palette of the installed messages.
     *
     * @return the colors
     * @throws IllegalStateException if no messages are installed yet
     */
    public static MessagePalette palette() {
        return book().palette();
    }

    /**
     * Fill in a value as plain text: tags inside it are shown, not run.
     *
     * @param name the placeholder's name, as in {@code <count>}
     * @param value the value
     * @return the placeholder
     */
    public static TagResolver value(final String name, final Object value) {
        return Placeholder.unparsed(name, String.valueOf(value));
    }

    /**
     * Fill in a value that is already a component, such as another rendered message.
     *
     * @param name the placeholder's name
     * @param value the component
     * @return the placeholder
     */
    public static TagResolver value(final String name, final ComponentLike value) {
        return Placeholder.component(name, value);
    }

    /**
     * Return the key of this message in {@code messages.yml}.
     *
     * @return the key, such as {@code reload.done}
     */
    public String path() {
        return this.path;
    }

    /**
     * Render this message.
     *
     * @param placeholders the values it fills in
     * @return the message
     * @throws IllegalStateException if no messages are installed yet
     */
    public Component component(final TagResolver... placeholders) {
        return book().render(this.path, placeholders);
    }

    /**
     * Render this message and send it.
     *
     * @param audience who gets it
     * @param placeholders the values it fills in
     * @throws IllegalStateException if no messages are installed yet
     */
    public void send(final Audience audience, final TagResolver... placeholders) {
        audience.sendMessage(this.component(placeholders));
    }

    /**
     * Return the installed messages.
     *
     * @return the messages
     * @throws IllegalStateException if none are installed yet
     */
    private static MessageBook book() {
        MessageBook current = book;
        if (current == null) {
            throw new IllegalStateException(NOT_LOADED);
        }

        return current;
    }
}
