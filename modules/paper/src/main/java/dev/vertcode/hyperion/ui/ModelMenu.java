package dev.vertcode.hyperion.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jspecify.annotations.Nullable;

import dev.vertcode.hyperion.animation.BakedAnimation;
import dev.vertcode.hyperion.api.animation.AnimationHandle;
import dev.vertcode.hyperion.api.animation.LoopMode;
import dev.vertcode.hyperion.api.model.EntityAnchor;
import dev.vertcode.hyperion.api.model.Model;
import dev.vertcode.hyperion.api.model.RemovalReason;
import dev.vertcode.hyperion.blueprint.Blueprint;
import dev.vertcode.hyperion.config.HyperionMessages;
import dev.vertcode.hyperion.menu.MenuButton;
import dev.vertcode.hyperion.menu.MenuClick;
import dev.vertcode.hyperion.menu.MenuPage;
import dev.vertcode.hyperion.model.ModelManager;

import static dev.vertcode.hyperion.config.HyperionMessages.value;

/**
 * The menu of one live model: its animations to play once, loop or stop, and controls to freeze
 * its mob, switch its idle and walk animations, stop everything, scale it, make it glow and
 * remove it.
 *
 * <p>Every action goes through the api, which is safe from the player's thread; freezing the mob
 * hops to the mob's own scheduler.
 */
public final class ModelMenu {

    private static final int FADE = 3;
    private static final float SCALE_STEP = 1.25f;
    private static final float MIN_SCALE = 0.1f;
    private static final float MAX_SCALE = 16f;
    private static final String DECIMALS = "%.2f";

    private final Plugin plugin;
    private final ModelManager models;
    private final Model model;

    private ModelMenu(final Plugin plugin, final ModelManager models, final Model model) {
        this.plugin = plugin;
        this.models = models;
        this.model = model;
    }

    /**
     * Build the menu of a model.
     *
     * @param plugin the plugin, for scheduling on the mob
     * @param models the live models, for removing this one
     * @param model the model
     * @return the menu
     */
    public static MenuPage of(final Plugin plugin, final ModelManager models, final Model model) {
        return new ModelMenu(plugin, models, model).page();
    }

    private MenuPage page() {
        List<MenuButton> animations = new ArrayList<>();
        for (String name : this.model.blueprint().animations()) {
            animations.add(new MenuButton(() -> this.animationIcon(name), (player, click) -> this.animate(name, click)));
        }

        List<MenuButton> controls = List.of(
                new MenuButton(this::freezeIcon, (player, click) -> this.freeze()),
                new MenuButton(this::autoIcon, (player, click) -> this.toggleAuto()),
                new MenuButton(this::stopIcon, (player, click) -> this.stopAll()),
                new MenuButton(
                        () -> this.scaleIcon(Material.SLIME_BALL, HyperionMessages.MENU__SMALLER),
                        (player, click) -> this.rescale(1f / SCALE_STEP)
                ),
                new MenuButton(
                        () -> this.scaleIcon(Material.MAGMA_CREAM, HyperionMessages.MENU__BIGGER),
                        (player, click) -> this.rescale(SCALE_STEP)
                ),
                new MenuButton(this::glowIcon, (player, click) -> this.toggleGlow()),
                new MenuButton(this::removeIcon, (player, click) -> this.remove())
        );

        return new MenuPage(
                HyperionMessages.MENU__TITLE.component(value("model", this.model.blueprint().key().asString())),
                animations,
                controls,
                Icons.of(Material.GRAY_STAINED_GLASS_PANE, Component.empty()),
                Icons.of(Material.ARROW, HyperionMessages.MENU__PREVIOUS.component()),
                Icons.of(Material.ARROW, HyperionMessages.MENU__NEXT.component())
        );
    }

    // Animations

    private ItemStack animationIcon(final String name) {
        AnimationHandle playing = this.model.animator().playing(name).orElse(null);
        float length = this.model.blueprint() instanceof Blueprint blueprint
                ? blueprint.baked(name).map(BakedAnimation::seconds).orElse(0f)
                : 0f;

        return Icons.of(
                playing == null ? Material.GRAY_DYE : Material.LIME_DYE,
                HyperionMessages.MENU__ANIMATION.component(value("animation", name)),
                playing == null
                        ? HyperionMessages.MENU__ANIMATION_IDLE.component()
                        : HyperionMessages.MENU__ANIMATION_PLAYING.component(),
                HyperionMessages.MENU__ANIMATION_LENGTH.component(value("length", format(length))),
                HyperionMessages.MENU__HINT_ONCE.component(),
                HyperionMessages.MENU__HINT_LOOP.component(),
                HyperionMessages.MENU__HINT_STOP.component()
        );
    }

    private boolean animate(final String name, final MenuClick click) {
        switch (click) {
            case LEFT -> this.model.animator().play(name, options -> options.loop(LoopMode.ONCE).fadeIn(FADE).fadeOut(FADE));
            case RIGHT -> this.model.animator().play(name, options -> options.loop(LoopMode.LOOP).fadeIn(FADE).fadeOut(FADE));
            case SHIFT_LEFT, SHIFT_RIGHT -> this.model.animator().stop(name);
            case OTHER -> {
                // Only the mouse buttons do something.
            }
        }

        return true;
    }

    private ItemStack stopIcon() {
        return Icons.of(Material.BARRIER, HyperionMessages.MENU__STOP_ALL.component());
    }

    private boolean stopAll() {
        this.model.animator().stopAll();

        return true;
    }

    private ItemStack autoIcon() {
        boolean on = this.model.autoAnimations();

        return Icons.of(
                Material.CLOCK,
                (on ? HyperionMessages.MENU__AUTO_ON : HyperionMessages.MENU__AUTO_OFF).component(),
                HyperionMessages.MENU__AUTO_HINT.component()
        );
    }

    private boolean toggleAuto() {
        this.model.autoAnimations(!this.model.autoAnimations());

        return true;
    }

    // The mob

    private ItemStack freezeIcon() {
        if (!(this.carrier() instanceof Mob mob)) {
            return Icons.of(Material.PACKED_ICE, HyperionMessages.MENU__CANNOT_FREEZE.component());
        }

        boolean frozen = !mob.hasAI();

        return Icons.of(
                frozen ? Material.BLUE_ICE : Material.ICE,
                (frozen ? HyperionMessages.MENU__FROZEN : HyperionMessages.MENU__MOVING).component(),
                HyperionMessages.MENU__FREEZE_HINT.component()
        );
    }

    private boolean freeze() {
        if (this.carrier() instanceof Mob mob) {
            // The mob may belong to another region than the player clicking.
            mob.getScheduler().run(this.plugin, task -> mob.setAI(!mob.hasAI()), null);
        }

        return true;
    }

    private @Nullable Entity carrier() {
        return this.model.anchor() instanceof EntityAnchor(Entity entity) ? entity : null;
    }

    // The model

    private ItemStack scaleIcon(final Material material, final HyperionMessages label) {
        return Icons.of(
                material,
                label.component(),
                HyperionMessages.MENU__SCALE.component(value("scale", format(this.model.scale())))
        );
    }

    private boolean rescale(final float factor) {
        float next = Math.clamp(this.model.scale() * factor, MIN_SCALE, MAX_SCALE);
        this.model.scale(next);

        return true;
    }

    private ItemStack glowIcon() {
        boolean on = this.model.glowing();

        return Icons.of(
                Material.GLOWSTONE_DUST,
                (on ? HyperionMessages.MENU__GLOW_ON : HyperionMessages.MENU__GLOW_OFF).component(),
                HyperionMessages.MENU__GLOW_HINT.component()
        );
    }

    private boolean toggleGlow() {
        this.model.glowing(!this.model.glowing());

        return true;
    }

    private ItemStack removeIcon() {
        return Icons.of(
                Material.LAVA_BUCKET,
                HyperionMessages.MENU__REMOVE.component(),
                HyperionMessages.MENU__REMOVE_HINT.component()
        );
    }

    private boolean remove() {
        this.models.remove(this.model, RemovalReason.REMOVED);

        return false;
    }

    /**
     * Write a number with two decimals.
     *
     * @param number the number
     * @return the text
     */
    private static String format(final float number) {
        return String.format(Locale.ROOT, DECIMALS, number);
    }
}
