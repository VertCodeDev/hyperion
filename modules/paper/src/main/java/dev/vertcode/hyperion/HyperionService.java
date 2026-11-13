package dev.vertcode.hyperion;

import dev.vertcode.hyperion.api.HyperionApi;
import dev.vertcode.hyperion.api.armor.ArmorRegistry;
import dev.vertcode.hyperion.api.effect.EffectRegistry;
import dev.vertcode.hyperion.api.player.PlayerAnimations;
import dev.vertcode.hyperion.api.model.ModelRegistry;
import dev.vertcode.hyperion.api.model.ModelTracker;
import dev.vertcode.hyperion.api.model.behavior.BehaviorRegistry;
import dev.vertcode.hyperion.api.model.source.FormatRegistry;
import dev.vertcode.hyperion.api.skin.SkinService;

/**
 * The api Hyperion installs for other plugins on enable.
 *
 * @param models the blueprints
 * @param tracker the live models
 * @param behaviors the behavior types
 * @param formats the model file formats
 * @param effects the effects
 * @param armor the armor sets
 * @param skins the player skins
 * @param playerAnimations the player animations
 */
record HyperionService(
        ModelRegistry models,
        ModelTracker tracker,
        BehaviorRegistry behaviors,
        FormatRegistry formats,
        EffectRegistry effects,
        ArmorRegistry armor,
        SkinService skins,
        PlayerAnimations playerAnimations
) implements HyperionApi {
}
