package dev.vertcode.hyperion.ui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;

import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jspecify.annotations.Nullable;

import dev.vertcode.hyperion.animation.BakedAnimation;
import dev.vertcode.hyperion.api.animation.LoopMode;
import dev.vertcode.hyperion.api.armor.ArmorPiece;
import dev.vertcode.hyperion.api.armor.ArmorSlot;
import dev.vertcode.hyperion.api.effect.Effect;
import dev.vertcode.hyperion.api.effect.EffectAnchor;
import dev.vertcode.hyperion.api.model.Model;
import dev.vertcode.hyperion.armor.HyperionArmorRegistry;
import dev.vertcode.hyperion.armor.HyperionArmorSet;
import dev.vertcode.hyperion.blueprint.Blueprint;
import dev.vertcode.hyperion.blueprint.BlueprintRegistry;
import dev.vertcode.hyperion.command.HyperionPermissions;
import dev.vertcode.hyperion.config.HyperionMessages;
import dev.vertcode.hyperion.effect.HyperionEffects;
import dev.vertcode.hyperion.menu.MenuButton;
import dev.vertcode.hyperion.menu.MenuClick;
import dev.vertcode.hyperion.menu.MenuPage;
import dev.vertcode.hyperion.model.ModelManager;

import static dev.vertcode.hyperion.config.HyperionMessages.value;

/**
 * The gallery of {@code /hy gallery}: every model and effect in a menu, to preview, spawn and
 * play.
 *
 * <p>Three pages: the models, one model's animations and own effects, and every effect. Clicks
 * run on the clicking player's thread, which owns where they stand; spawning needs
 * {@code hyperion.command.spawn} and playing effects {@code hyperion.command.effect} on top of the
 * gallery's own permission.
 */
public final class Gallery {

    private static final int FADE = 3;
    private static final double EFFECT_RADIUS = 16d;
    private static final String DECIMALS = "%.2f";

    private final Plugin plugin;
    private final BlueprintRegistry registry;
    private final ModelManager models;
    private final HyperionEffects effects;
    private final Previews previews;
    private final Menus menus;
    private final HyperionArmorRegistry armor;

    /**
     * Create the gallery.
     *
     * @param plugin the plugin, for scheduling
     * @param registry the loaded models
     * @param models the live models, which spawn the mobs models ride
     * @param effects the effects to list and play
     * @param previews the players' private previews
     * @param menus opens the pages
     * @param armor the armor sets to list and hand out
     */
    public Gallery(
            final Plugin plugin,
            final BlueprintRegistry registry,
            final ModelManager models,
            final HyperionEffects effects,
            final Previews previews,
            final Menus menus,
            final HyperionArmorRegistry armor
    ) {
        this.plugin = plugin;
        this.registry = registry;
        this.models = models;
        this.effects = effects;
        this.previews = previews;
        this.menus = menus;
        this.armor = armor;
    }

    /**
     * Return the players' previews.
     *
     * @return the previews
     */
    public Previews previews() {
        return this.previews;
    }

    // Opening

    /**
     * Open the models page, or tell the player nothing matches. Call on the player's thread.
     *
     * @param player the player
     * @param filter the text a model's key must contain, or null for every model
     */
    public void openModels(final Player player, final @Nullable String filter) {
        List<Blueprint> found = matching(this.registry.all(), blueprint -> blueprint.key().asString(), filter);
        if (found.isEmpty()) {
            HyperionMessages empty = filter == null ? HyperionMessages.GALLERY__EMPTY : HyperionMessages.GALLERY__NO_MATCH;
            empty.send(player, value("filter", String.valueOf(filter)));
            return;
        }

        this.menus.open(player, this.modelsPage(found));
    }

    /**
     * Keep the items whose key contains the filter, ignoring case, sorted by key.
     *
     * @param items the items
     * @param key reads an item's key
     * @param filter the text a key must contain, or null for every item
     * @param <T> the item type
     * @return the items that match
     */
    static <T> List<T> matching(final Collection<T> items, final Function<T, String> key, final @Nullable String filter) {
        String needle = filter == null ? "" : filter.trim().toLowerCase(Locale.ROOT);
        List<T> found = new ArrayList<>();
        for (T item : items) {
            if (key.apply(item).toLowerCase(Locale.ROOT).contains(needle)) {
                found.add(item);
            }
        }

        found.sort(Comparator.comparing(key));

        return found;
    }

    /**
     * Open the effects page. Call on the player's thread.
     *
     * @param player the player
     */
    public void openEffects(final Player player) {
        this.menus.open(player, this.effectsPage());
    }

    /**
     * Open the armor page. Call on the player's thread.
     *
     * @param player the player
     */
    public void openArmor(final Player player) {
        this.menus.open(player, this.armorPage());
    }

    /**
     * Open another page from a click, once the click is done.
     *
     * @param player who clicked
     * @param page the page
     * @return true, to keep the menu open until the new one takes its place
     */
    private boolean switchTo(final Player player, final MenuPage page) {
        player.getScheduler().run(this.plugin, task -> this.menus.open(player, page), null);

        return true;
    }

    // Models

    private MenuPage modelsPage(final List<Blueprint> blueprints) {
        List<MenuButton> content = new ArrayList<>(blueprints.size());
        for (Blueprint blueprint : blueprints) {
            content.add(new MenuButton(
                    () -> this.modelIcon(blueprint),
                    (player, click) -> this.modelClick(player, blueprint, click)
            ));
        }

        List<MenuButton> controls = List.of(
                new MenuButton(
                        () -> Icons.of(
                                Material.NETHER_STAR,
                                HyperionMessages.GALLERY__EFFECTS.component(),
                                HyperionMessages.GALLERY__EFFECTS_HINT.component()
                        ),
                        (player, click) -> this.switchTo(player, this.effectsPage())
                ),
                new MenuButton(
                        () -> Icons.of(
                                Material.DIAMOND_CHESTPLATE,
                                HyperionMessages.GALLERY__ARMOR.component(),
                                HyperionMessages.GALLERY__ARMOR_HINT.component()
                        ),
                        (player, click) -> this.switchTo(player, this.armorPage())
                ),
                this.removePreviewButton()
        );

        return this.page(HyperionMessages.GALLERY__TITLE.component(value("count", blueprints.size())), content, controls);
    }

    private ItemStack modelIcon(final Blueprint blueprint) {
        return Icons.of(
                Material.ARMOR_STAND,
                HyperionMessages.GALLERY__MODEL.component(value("model", blueprint.key().asString())),
                HyperionMessages.GALLERY__MODEL_COUNTS.component(
                        value("bones", blueprint.bones().size()),
                        value("animations", blueprint.animations().size()),
                        value("locators", blueprint.locators().size()),
                        value("effects", this.effects.ofModel(blueprint.key()).size())
                ),
                HyperionMessages.GALLERY__HINT_MOB.component(),
                HyperionMessages.GALLERY__HINT_STATIC.component(),
                HyperionMessages.GALLERY__HINT_PREVIEW.component(),
                HyperionMessages.GALLERY__HINT_DETAILS.component()
        );
    }

    private boolean modelClick(final Player player, final Blueprint blueprint, final MenuClick click) {
        return switch (click) {
            case LEFT -> this.spawnOnMob(player, blueprint);
            case RIGHT -> this.spawnStill(player, blueprint);
            case SHIFT_LEFT -> this.preview(player, blueprint) == null;
            case SHIFT_RIGHT -> this.switchTo(player, this.modelPage(player, blueprint));
            case OTHER -> true;
        };
    }

    // One model

    private MenuPage modelPage(final Player viewer, final Blueprint blueprint) {
        List<MenuButton> content = new ArrayList<>();
        List<String> animations = new ArrayList<>(blueprint.animations());
        animations.sort(String::compareTo);
        for (String name : animations) {
            content.add(new MenuButton(
                    () -> this.animationIcon(viewer, blueprint, name),
                    (player, click) -> this.animate(player, blueprint, name, click)
            ));
        }

        for (Effect effect : this.effects.ofModel(blueprint.key())) {
            content.add(new MenuButton(
                    () -> this.effectIcon(effect, HyperionMessages.GALLERY__HINT_EFFECT_PREVIEW.component()),
                    (player, click) -> this.playOnPreview(player, blueprint, effect)
            ));
        }

        List<MenuButton> controls = List.of(
                new MenuButton(
                        () -> Icons.of(Material.OAK_DOOR, HyperionMessages.GALLERY__BACK.component()),
                        (player, click) -> {
                            this.openModelsLater(player);
                            return true;
                        }
                ),
                new MenuButton(
                        () -> Icons.of(Material.PIG_SPAWN_EGG, HyperionMessages.GALLERY__SPAWN_MOB.component()),
                        (player, click) -> this.spawnOnMob(player, blueprint)
                ),
                new MenuButton(
                        () -> Icons.of(Material.ARMOR_STAND, HyperionMessages.GALLERY__SPAWN_STATIC.component()),
                        (player, click) -> this.spawnStill(player, blueprint)
                ),
                new MenuButton(
                        () -> Icons.of(
                                Material.ENDER_EYE,
                                HyperionMessages.GALLERY__PREVIEW.component(),
                                HyperionMessages.GALLERY__PREVIEW_HINT.component(value("seconds", this.previews.seconds()))
                        ),
                        (player, click) -> this.preview(player, blueprint) == null
                ),
                this.removePreviewButton()
        );

        Component title = HyperionMessages.GALLERY__TITLE_MODEL.component(value("model", blueprint.key().asString()));

        return this.page(title, content, controls);
    }

    private ItemStack animationIcon(final Player viewer, final Blueprint blueprint, final String name) {
        Model preview = this.previewOf(viewer, blueprint).orElse(null);
        boolean playing = preview != null && preview.animator().playing(name).isPresent();
        float length = blueprint.baked(name).map(BakedAnimation::seconds).orElse(0f);

        return Icons.of(
                playing ? Material.LIME_DYE : Material.GRAY_DYE,
                HyperionMessages.GALLERY__ANIMATION.component(value("animation", name)),
                HyperionMessages.GALLERY__ANIMATION_LENGTH.component(
                        value("length", String.format(Locale.ROOT, DECIMALS, length))
                ),
                playing ? HyperionMessages.GALLERY__ANIMATION_PLAYING.component() : Component.empty(),
                HyperionMessages.GALLERY__HINT_ONCE.component(),
                HyperionMessages.GALLERY__HINT_LOOP.component(),
                HyperionMessages.GALLERY__HINT_STOP.component()
        );
    }

    private boolean animate(final Player player, final Blueprint blueprint, final String name, final MenuClick click) {
        if (click == MenuClick.OTHER) {
            return true;
        }

        if (click == MenuClick.SHIFT_LEFT || click == MenuClick.SHIFT_RIGHT) {
            this.previewOf(player, blueprint).ifPresent(preview -> preview.animator().stop(name));
            return true;
        }

        Model preview = this.previewOf(player, blueprint).orElseGet(() -> this.preview(player, blueprint));
        if (preview == null) {
            return false;
        }

        LoopMode loop = click == MenuClick.RIGHT ? LoopMode.LOOP : LoopMode.ONCE;
        preview.animator().play(name, options -> options.loop(loop).fadeIn(FADE).fadeOut(FADE));

        return true;
    }

    private boolean playOnPreview(final Player player, final Blueprint blueprint, final Effect effect) {
        if (!this.allowed(player, HyperionPermissions.EFFECT)) {
            return true;
        }

        Model preview = this.previewOf(player, blueprint).orElseGet(() -> this.preview(player, blueprint));
        if (preview != null) {
            this.effects.play(effect, EffectAnchor.on(preview), options -> {
                // The preview's viewer, the player alone, sees it.
            });
        }

        return true;
    }

    // Effects

    private MenuPage effectsPage() {
        List<Effect> all = matching(this.effects.all(), effect -> effect.key().asString(), null);
        List<MenuButton> content = new ArrayList<>(all.size());
        for (Effect effect : all) {
            content.add(new MenuButton(
                    () -> this.effectIcon(
                            effect,
                            HyperionMessages.GALLERY__HINT_EFFECT_MODEL.component(),
                            HyperionMessages.GALLERY__HINT_EFFECT_HERE.component(),
                            HyperionMessages.GALLERY__HINT_EFFECT_CAMERA.component()
                    ),
                    (player, click) -> this.playEffect(player, effect, click)
            ));
        }

        List<MenuButton> controls = List.of(new MenuButton(
                () -> Icons.of(Material.OAK_DOOR, HyperionMessages.GALLERY__BACK.component()),
                (player, click) -> {
                    this.openModelsLater(player);
                    return true;
                }
        ));

        return this.page(HyperionMessages.GALLERY__TITLE_EFFECTS.component(value("count", all.size())), content, controls);
    }

    private ItemStack effectIcon(final Effect effect, final Component... hints) {
        Component[] lore = new Component[hints.length + 1];
        lore[0] = HyperionMessages.GALLERY__EFFECT_COUNTS.component(
                value("parts", effect.parts().size()),
                value("duration", effect.duration()),
                value("loop", effect.loop() ? HyperionMessages.GALLERY__EFFECT_LOOP.component() : Component.empty())
        );
        System.arraycopy(hints, 0, lore, 1, hints.length);

        Component name = HyperionMessages.GALLERY__EFFECT.component(value("effect", effect.key().asString()));

        return Icons.of(Material.BLAZE_POWDER, name, lore);
    }

    private boolean playEffect(final Player player, final Effect effect, final MenuClick click) {
        if (click == MenuClick.OTHER) {
            return true;
        }

        if (!this.allowed(player, HyperionPermissions.EFFECT)) {
            return true;
        }

        Entity self = player;
        Location at = self.getLocation();
        boolean here = click == MenuClick.RIGHT;
        boolean camera = click == MenuClick.SHIFT_LEFT || click == MenuClick.SHIFT_RIGHT;
        Optional<Model> model = here ? Optional.empty() : this.models.nearest(at, EFFECT_RADIUS);
        EffectAnchor anchor = model.map(EffectAnchor::on).orElseGet(() -> EffectAnchor.at(at));
        this.effects.play(effect, anchor, options -> {
            if (camera) {
                options.camera(player);
            }
        });

        Component target = model
                .map(found -> HyperionMessages.EFFECT__ON_MODEL.component(value("model", found.blueprint().key().asString())))
                .orElseGet(HyperionMessages.EFFECT__HERE::component);
        HyperionMessages.EFFECT__PLAYING.send(player, value("effect", effect.key().asString()), value("target", target));

        return !camera;
    }

    // Spawning and previews

    private boolean spawnOnMob(final Player player, final Blueprint blueprint) {
        if (!this.allowed(player, HyperionPermissions.SPAWN)) {
            return true;
        }

        String name = this.models.settings().spawnEntity();
        NamespacedKey typeKey = NamespacedKey.fromString(name);
        EntityType type = typeKey == null ? null : Registry.ENTITY_TYPE.get(typeKey);
        if (type == null) {
            HyperionMessages.SPAWN__BAD_ENTITY.send(player, value("entity", name));
            return false;
        }

        Entity self = player;
        Entity carrier;
        try {
            carrier = this.models.spawnCarrier(self.getLocation(), type);
        } catch (IllegalArgumentException exception) {
            HyperionMessages.SPAWN__BAD_ENTITY.send(player, value("entity", name));
            return false;
        }

        try {
            blueprint.attach(carrier);
        } catch (IllegalStateException | IllegalArgumentException exception) {
            carrier.remove();
            HyperionMessages.SPAWN__FAILED.send(player, value("reason", String.valueOf(exception.getMessage())));
            return false;
        }

        HyperionMessages.SPAWN__SPAWNED_ON_MOB.send(
                player,
                value("model", blueprint.key().asString()),
                value("entity", type.getKey().getKey())
        );

        return false;
    }

    private boolean spawnStill(final Player player, final Blueprint blueprint) {
        if (!this.allowed(player, HyperionPermissions.SPAWN)) {
            return true;
        }

        Entity self = player;
        try {
            blueprint.spawn(self.getLocation());
        } catch (IllegalStateException | IllegalArgumentException exception) {
            HyperionMessages.SPAWN__FAILED.send(player, value("reason", String.valueOf(exception.getMessage())));
            return false;
        }

        HyperionMessages.SPAWN__SPAWNED.send(player, value("model", blueprint.key().asString()));

        return false;
    }

    /**
     * Show a player a preview, and tell them.
     *
     * @param player the player
     * @param blueprint the model
     * @return the preview, or null when it could not spawn, which they are told
     */
    private @Nullable Model preview(final Player player, final Blueprint blueprint) {
        try {
            Model model = this.previews.show(player, blueprint);
            HyperionMessages.GALLERY__PREVIEW_SHOWN.send(player, value("model", blueprint.key().asString()));
            return model;
        } catch (IllegalStateException | IllegalArgumentException exception) {
            HyperionMessages.GALLERY__PREVIEW_FAILED.send(player, value("reason", String.valueOf(exception.getMessage())));
            return null;
        }
    }

    /**
     * Return a player's preview when it shows this model.
     *
     * @param player the player
     * @param blueprint the model
     * @return the preview, or empty
     */
    private Optional<Model> previewOf(final Player player, final Blueprint blueprint) {
        return this.previews.current(player.getUniqueId())
                .filter(preview -> preview.blueprint().key().equals(blueprint.key()));
    }

    // Armor

    private MenuPage armorPage() {
        List<HyperionArmorSet> sets = this.armor.sorted();
        List<MenuButton> content = new ArrayList<>(sets.size());
        for (HyperionArmorSet set : sets) {
            content.add(new MenuButton(() -> this.armorIcon(set), (player, click) -> this.armorClick(player, set, click)));
        }

        List<MenuButton> controls = List.of(new MenuButton(
                () -> Icons.of(Material.OAK_DOOR, HyperionMessages.GALLERY__BACK.component()),
                (player, click) -> {
                    this.openModelsLater(player);
                    return true;
                }
        ));

        return this.page(HyperionMessages.GALLERY__TITLE_ARMOR.component(value("count", sets.size())), content, controls);
    }

    private ItemStack armorIcon(final HyperionArmorSet set) {
        Component[] lore = {
            HyperionMessages.GALLERY__ARMOR_SET.component(value("set", set.key().asString())),
            HyperionMessages.GALLERY__ARMOR_PIECES.component(value("count", set.pieces().size())),
            HyperionMessages.GALLERY__HINT_ARMOR_GIVE.component(),
            HyperionMessages.GALLERY__HINT_ARMOR_WEAR.component()
        };
        ArmorPiece shown = set.piece(ArmorSlot.CHEST).or(() -> set.pieces().stream().findFirst()).orElse(null);
        try {
            if (shown != null) {
                return Icons.withLore(shown.item(), lore);
            }
        } catch (IllegalStateException exception) {
            // The pack is not built yet, so the piece has no model to show.
        }

        return Icons.of(Material.LEATHER_CHESTPLATE, Component.text(set.key().asString()), lore);
    }

    private boolean armorClick(final Player player, final HyperionArmorSet set, final MenuClick click) {
        if ((click != MenuClick.LEFT && click != MenuClick.RIGHT) || !this.allowed(player, HyperionPermissions.ARMOR)) {
            return true;
        }

        List<ItemStack> spare = new ArrayList<>();
        try {
            for (ArmorPiece piece : set.pieces()) {
                ItemStack item = piece.item();
                if (click == MenuClick.RIGHT) {
                    EquipmentSlot slot = piece.slot().equipmentSlot();
                    ItemStack worn = player.getEquipment().getItem(slot);
                    player.getEquipment().setItem(slot, item);
                    item = worn;
                }

                if (!item.isEmpty()) {
                    spare.add(item);
                }
            }
        } catch (IllegalStateException exception) {
            HyperionMessages.ARMOR__NOT_BUILT.send(player);
            return true;
        }

        for (ItemStack left : player.getInventory().addItem(spare.toArray(ItemStack[]::new)).values()) {
            player.getWorld().dropItemNaturally(player.getEyeLocation(), left);
        }

        return true;
    }

    private MenuButton removePreviewButton() {
        return new MenuButton(
                () -> Icons.of(Material.BARRIER, HyperionMessages.GALLERY__REMOVE_PREVIEW.component()),
                (player, click) -> {
                    boolean had = this.previews.clear(player.getUniqueId());
                    (had ? HyperionMessages.GALLERY__PREVIEW_REMOVED : HyperionMessages.GALLERY__NO_PREVIEW).send(player);
                    return true;
                }
        );
    }

    // Helpers

    private void openModelsLater(final Player player) {
        player.getScheduler().run(this.plugin, task -> this.openModels(player, null), null);
    }

    private boolean allowed(final Player player, final String permission) {
        if (player.hasPermission(permission)) {
            return true;
        }

        HyperionMessages.COMMAND__NO_PERMISSION.send(player);

        return false;
    }

    private MenuPage page(final Component title, final List<MenuButton> content, final List<MenuButton> controls) {
        return new MenuPage(
                title,
                content,
                controls,
                Icons.of(Material.GRAY_STAINED_GLASS_PANE, Component.empty()),
                Icons.of(Material.ARROW, HyperionMessages.MENU__PREVIOUS.component()),
                Icons.of(Material.ARROW, HyperionMessages.MENU__NEXT.component())
        );
    }
}
