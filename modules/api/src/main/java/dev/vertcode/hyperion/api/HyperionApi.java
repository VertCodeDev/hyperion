package dev.vertcode.hyperion.api;

import java.util.function.Function;

import org.bukkit.World;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;

import dev.vertcode.hyperion.api.armor.ArmorRegistry;
import dev.vertcode.hyperion.api.effect.EffectRegistry;
import dev.vertcode.hyperion.api.player.PlayerAnimations;
import dev.vertcode.hyperion.api.model.ModelRegistry;
import dev.vertcode.hyperion.api.model.ModelTracker;
import dev.vertcode.hyperion.api.model.behavior.BehaviorRegistry;
import dev.vertcode.hyperion.api.model.source.FormatRegistry;
import dev.vertcode.hyperion.api.skin.SkinService;

/**
 * Everything Hyperion offers another plugin, behind one interface.
 *
 * <p>Reach it through {@link Hyperion#api()} or Bukkit's {@code ServicesManager}. Every method is
 * safe from any thread. Being an interface, it can be mocked in your own tests.
 */
@ApiStatus.NonExtendable
public interface HyperionApi {

    /**
     * Return the registry of model blueprints.
     *
     * @return the model registry
     */
    ModelRegistry models();

    /**
     * Return the tracker that finds live models.
     *
     * @return the model tracker
     */
    ModelTracker tracker();

    /**
     * Return the registry of bone behaviors.
     *
     * @return the behavior registry
     */
    BehaviorRegistry behaviors();

    /**
     * Return the registry of model file formats.
     *
     * @return the format registry
     */
    FormatRegistry formats();

    /**
     * Return the effects and the way to play them.
     *
     * @return the effect registry
     */
    EffectRegistry effects();

    /**
     * Return the armor sets and what each player wears.
     *
     * @return the armor registry
     */
    ArmorRegistry armor();

    /**
     * Return the player skins models can wear.
     *
     * @return the skin service
     */
    SkinService skins();

    /**
     * Return the way to play animations on players, in their own skin.
     *
     * @return the player animations
     */
    PlayerAnimations playerAnimations();

    /**
     * Count worlds that are copies of one another as one world. A model that stands in one copy is
     * then shown, at the same spot, to players in every other copy too, as if they stood in its
     * own world. Worlds with the same group name are copies of one world.
     *
     * @param groupOf the group name of a world, or {@code null} to make every world its own again
     */
    void worldGroups(@Nullable Function<World, String> groupOf);
}
