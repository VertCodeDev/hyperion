package dev.vertcode.hyperion.api.effect;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import net.kyori.adventure.key.Key;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Unmodifiable;

/**
 * Every effect Hyperion knows, and the way to play one.
 *
 * <p>Effects come from {@code plugins/Hyperion/effects} as {@code hyperion:<name>}, from folders
 * plugins register, from code, and from models: a model's own effects are
 * {@code <model key>/<name>}, such as {@code hyperion:dragon/fire_breath}.
 *
 * <pre>{@code
 * Hyperion.effects().play(Key.key("hyperion", "fire_breath"), EffectAnchor.locator(model, "mouth"),
 *         options -> options.viewers(EffectViewers.nearby(32)).parameter("color", "#3399FF"));
 * }</pre>
 *
 * <p>Every method is safe from any thread. A play starts on the thread that owns its anchor, on
 * that thread's next tick; effects on a model play inside the model's own frame.
 */
@ApiStatus.NonExtendable
public interface EffectRegistry {

    /**
     * Add an effect built in code. It goes when the plugin disables.
     *
     * @param owner the plugin it belongs to
     * @param effect the effect
     * @throws IllegalArgumentException if another plugin already uses the key
     */
    void register(Plugin owner, Effect effect);

    /**
     * Load every {@code .yml} file in a folder as effects in a namespace, now and on every
     * {@code /hy reload}. Each top-level section of a file is one effect.
     *
     * @param owner the plugin the folder belongs to
     * @param namespace the namespace of its effects' keys
     * @param folder the folder
     * @return how many effects loaded
     */
    int registerFolder(Plugin owner, String namespace, Path folder);

    /**
     * Add a part type effects can use. It goes when the plugin disables.
     *
     * @param owner the plugin it belongs to
     * @param type the part type
     * @throws IllegalArgumentException if another type already uses the key
     */
    void registerPartType(Plugin owner, EffectPartType type);

    /**
     * Remove everything a plugin added. Hyperion calls this when the plugin disables.
     *
     * @param owner the plugin
     */
    void unregisterAll(Plugin owner);

    /**
     * Find an effect, a model's own effects included.
     *
     * @param key the effect's key
     * @return the effect, or empty when none has the key
     */
    Optional<Effect> find(Key key);

    /**
     * Return every loaded and registered effect, without the effects inside models.
     *
     * @return the effects
     */
    @Unmodifiable Collection<Effect> all();

    /**
     * Play an effect as made.
     *
     * @param key the effect's key
     * @param anchor what it is attached to
     * @return the handle
     * @throws IllegalArgumentException if no effect has the key
     */
    EffectHandle play(Key key, EffectAnchor anchor);

    /**
     * Play an effect with options.
     *
     * @param key the effect's key
     * @param anchor what it is attached to
     * @param options sets this play's options
     * @return the handle
     * @throws IllegalArgumentException if no effect has the key
     */
    EffectHandle play(Key key, EffectAnchor anchor, Consumer<EffectOptions> options);

    /**
     * Play an effect that need not be registered.
     *
     * @param effect the effect
     * @param anchor what it is attached to
     * @param options sets this play's options
     * @return the handle
     */
    EffectHandle play(Effect effect, EffectAnchor anchor, Consumer<EffectOptions> options);

    /**
     * Return every effect that plays now.
     *
     * @return the handles
     */
    @Unmodifiable List<EffectHandle> playing();
}
