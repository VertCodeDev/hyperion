package dev.vertcode.hyperion.model;

import java.util.Set;

import org.bukkit.Color;
import org.bukkit.entity.Display;
import org.jspecify.annotations.Nullable;

import dev.vertcode.hyperion.api.model.EntityPart;
import dev.vertcode.hyperion.api.model.ModelRotator;
import dev.vertcode.hyperion.api.model.ModelScaler;
import dev.vertcode.hyperion.api.model.SpawnOptions;
import dev.vertcode.hyperion.api.model.bone.BoneTransform;
import dev.vertcode.hyperion.api.model.hitbox.HitboxMode;
import dev.vertcode.hyperion.api.model.view.ViewerPolicy;
import dev.vertcode.hyperion.blueprint.Blueprint;

/**
 * The settings a caller gives a model before it spawns, starting from {@code config.yml}.
 *
 * <p>Used on the spawning thread only, then read once into the new model.
 */
final class ModelOptions implements SpawnOptions {

    private static final String SCALE_NOT_POSITIVE = "A model scale must be above 0, got %s";
    private static final String DISTANCE_NOT_POSITIVE = "A view distance must be above 0, got %d";
    private static final String NEGATIVE_TICKS = "Interpolation ticks must be 0 or more, got %d";
    private static final String NO_ANIMATION = "The model %s has no animation named '%s'";

    private final Blueprint blueprint;
    BoneTransform transform = BoneTransform.IDENTITY;
    ModelScaler scaler = ModelScaler.fixed(1f);
    ModelRotator rotator = ModelRotator.anchor();
    ViewerPolicy policy = ViewerPolicy.everyone();
    int viewDistance;
    int interpolation;
    Set<EntityPart> hiddenParts;
    @Nullable Color tint;
    boolean glowing;
    @Nullable Color glowColor;
    Display.@Nullable Brightness brightness;
    boolean glint;
    Display.@Nullable Billboard billboard;
    @Nullable String animation;
    @Nullable Boolean autoAnimations;
    @Nullable HitboxMode hitboxMode;

    /**
     * Start from the defaults.
     *
     * @param blueprint the blueprint being spawned, to check animation names against
     * @param viewDistance the view distance from {@code config.yml}
     * @param interpolation the interpolation from {@code config.yml}
     * @param hiddenParts the parts hidden by default: every part for a model on an entity
     */
    ModelOptions(
            final Blueprint blueprint,
            final int viewDistance,
            final int interpolation,
            final Set<EntityPart> hiddenParts
    ) {
        this.blueprint = blueprint;
        this.viewDistance = viewDistance;
        this.interpolation = interpolation;
        this.hiddenParts = hiddenParts;
    }

    @Override
    public SpawnOptions scale(final float scale) {
        if (!(scale > 0f)) {
            throw new IllegalArgumentException(String.format(SCALE_NOT_POSITIVE, scale));
        }

        this.scaler = ModelScaler.fixed(scale);

        return this;
    }

    @Override
    public SpawnOptions transform(final BoneTransform transform) {
        this.transform = transform;

        return this;
    }

    @Override
    public SpawnOptions viewDistance(final int blocks) {
        if (blocks <= 0) {
            throw new IllegalArgumentException(String.format(DISTANCE_NOT_POSITIVE, blocks));
        }

        this.viewDistance = blocks;

        return this;
    }

    @Override
    public SpawnOptions viewers(final ViewerPolicy policy) {
        this.policy = policy;

        return this;
    }

    @Override
    public SpawnOptions scaler(final ModelScaler scaler) {
        this.scaler = scaler;

        return this;
    }

    @Override
    public SpawnOptions rotator(final ModelRotator rotator) {
        this.rotator = rotator;

        return this;
    }

    @Override
    public SpawnOptions interpolation(final int ticks) {
        if (ticks < 0) {
            throw new IllegalArgumentException(String.format(NEGATIVE_TICKS, ticks));
        }

        this.interpolation = ticks;

        return this;
    }

    @Override
    public SpawnOptions hiddenParts(final Set<EntityPart> parts) {
        this.hiddenParts = Set.copyOf(parts);

        return this;
    }

    @Override
    public SpawnOptions hideBaseEntity(final boolean hidden) {
        this.hiddenParts = hidden ? EntityPart.ALL : EntityPart.NONE;

        return this;
    }

    @Override
    public SpawnOptions tint(final @Nullable Color tint) {
        this.tint = tint;

        return this;
    }

    @Override
    public SpawnOptions glowing(final boolean glowing) {
        this.glowing = glowing;

        return this;
    }

    @Override
    public SpawnOptions glowColor(final @Nullable Color color) {
        this.glowColor = color;

        return this;
    }

    @Override
    public SpawnOptions brightness(final Display.@Nullable Brightness brightness) {
        this.brightness = brightness;

        return this;
    }

    @Override
    public SpawnOptions glint(final boolean glint) {
        this.glint = glint;

        return this;
    }

    @Override
    public SpawnOptions billboard(final Display.@Nullable Billboard billboard) {
        this.billboard = billboard;

        return this;
    }

    @Override
    public SpawnOptions animation(final String name) {
        if (!this.blueprint.animations().contains(name)) {
            String message = String.format(NO_ANIMATION, this.blueprint.key().asString(), name);
            throw new IllegalArgumentException(message);
        }

        this.animation = name;

        return this;
    }

    @Override
    public SpawnOptions autoAnimations(final boolean enabled) {
        this.autoAnimations = enabled;

        return this;
    }

    @Override
    public SpawnOptions hitboxMode(final HitboxMode mode) {
        this.hitboxMode = mode;

        return this;
    }
}
