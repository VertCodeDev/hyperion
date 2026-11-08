package dev.vertcode.hyperion.blueprint;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import net.kyori.adventure.key.Key;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.BoundingBox;
import org.jspecify.annotations.Nullable;

import dev.vertcode.hyperion.animation.BakedAnimation;
import dev.vertcode.hyperion.api.model.EntityAnchor;
import dev.vertcode.hyperion.api.model.LocationAnchor;
import dev.vertcode.hyperion.api.model.Model;
import dev.vertcode.hyperion.api.model.ModelBlueprint;
import dev.vertcode.hyperion.api.model.SpawnOptions;
import dev.vertcode.hyperion.api.model.bone.BoneBlueprint;
import dev.vertcode.hyperion.api.model.locator.LocatorBlueprint;
import dev.vertcode.hyperion.api.model.metadata.Metadata;
import dev.vertcode.hyperion.api.model.source.ModelSource;
import dev.vertcode.hyperion.blueprint.data.Animation;
import dev.vertcode.hyperion.blueprint.data.ModelData;
import dev.vertcode.hyperion.blueprint.data.Texture;

/**
 * A loaded model: the bone tree, locators, textures and animations of one Blockbench file.
 *
 * <p>Immutable and shared by every model spawned from it, so any thread may read it. A reload
 * builds a new blueprint with a higher revision rather than changing this one. The api accessors
 * return the api types; {@link #blueprintBones()} and {@link #blueprintLocators()} give the
 * plugin's own types, with the cubes the resource pack is built from.
 */
public final class Blueprint implements ModelBlueprint {

    private final Key key;
    private final @Nullable Plugin owner;
    private final int revision;
    private final ModelSource source;
    private final ModelData data;
    private final List<BlueprintBone> bones;
    private final List<BoneBlueprint> apiBones;
    private final List<BoneBlueprint> rootBones;
    private final Map<String, BlueprintBone> bonesByName;
    private final List<BlueprintLocator> locators;
    private final List<LocatorBlueprint> apiLocators;
    private final Map<String, BlueprintLocator> locatorsByName;
    private final Map<String, Animation> animations;
    private final Map<String, BakedAnimation> baked;
    private final BoundingBox boundingBox;
    private final LoadReport report;
    private final ModelSpawner spawner;

    /**
     * Create a blueprint. Only {@link BlueprintFactory} calls this.
     *
     * @param key the key it is registered under
     * @param owner the plugin that registered it, or null for Hyperion's own folder
     * @param revision how many times the key has been loaded
     * @param source where it was loaded from
     * @param data the file as read
     * @param bones every bone, parents first
     * @param locators every locator
     * @param animations every animation, by unique name
     * @param baked every animation baked for playing, by the same names
     * @param boundingBox the box around every cube at rest, in blocks
     * @param report the problems found while loading
     * @param spawner what {@link #spawn} and {@link #attach} hand the work to
     */
    Blueprint(
            final Key key,
            final @Nullable Plugin owner,
            final int revision,
            final ModelSource source,
            final ModelData data,
            final Map<String, BlueprintBone> bones,
            final Map<String, BlueprintLocator> locators,
            final Map<String, Animation> animations,
            final Map<String, BakedAnimation> baked,
            final BoundingBox boundingBox,
            final LoadReport report,
            final ModelSpawner spawner
    ) {
        this.key = key;
        this.owner = owner;
        this.revision = revision;
        this.source = source;
        this.data = data;
        this.bones = List.copyOf(bones.values());
        this.apiBones = List.copyOf(this.bones);
        this.rootBones = this.bones.stream()
                .filter(bone -> bone.parent().isEmpty())
                .<BoneBlueprint>map(bone -> bone)
                .toList();
        this.bonesByName = Map.copyOf(bones);
        this.locators = List.copyOf(locators.values());
        this.apiLocators = List.copyOf(this.locators);
        this.locatorsByName = Map.copyOf(locators);
        this.animations = Collections.unmodifiableMap(new LinkedHashMap<>(animations));
        this.baked = Map.copyOf(baked);
        this.boundingBox = boundingBox.clone();
        this.report = report;
        this.spawner = spawner;
    }

    /**
     * Return the key this blueprint is registered under.
     *
     * @return the key
     */
    @Override
    public Key key() {
        return this.key;
    }

    /**
     * Return the plugin that registered this blueprint.
     *
     * @return the owner, or null for a model from Hyperion's own {@code models/} folder
     */
    @Override
    public @Nullable Plugin owner() {
        return this.owner;
    }

    /**
     * Return how many times this key has been loaded, starting at 1.
     *
     * @return the revision
     */
    @Override
    public int revision() {
        return this.revision;
    }

    /**
     * Return where this blueprint was loaded from, so it can be loaded again.
     *
     * @return the source
     */
    public ModelSource source() {
        return this.source;
    }

    /**
     * Return the file as read, for the resource pack and animation playback.
     *
     * @return the model data
     */
    public ModelData data() {
        return this.data;
    }

    @Override
    public List<BoneBlueprint> bones() {
        return this.apiBones;
    }

    /**
     * Return every bone with its cubes, parents before their children.
     *
     * @return the bones
     */
    public List<BlueprintBone> blueprintBones() {
        return this.bones;
    }

    @Override
    public List<BoneBlueprint> rootBones() {
        return this.rootBones;
    }

    /**
     * Find a bone by its name.
     *
     * @param name the bone's name
     * @return the bone, or empty if there is none by that name
     */
    @Override
    public Optional<BoneBlueprint> bone(final String name) {
        return Optional.ofNullable(this.bonesByName.get(name));
    }

    @Override
    public List<LocatorBlueprint> locators() {
        return this.apiLocators;
    }

    /**
     * Return every locator with its position relative to its bone.
     *
     * @return the locators
     */
    public List<BlueprintLocator> blueprintLocators() {
        return this.locators;
    }

    @Override
    public Optional<LocatorBlueprint> locator(final String name) {
        return Optional.ofNullable(this.locatorsByName.get(name));
    }

    /**
     * Return the names of the animations, in the order of the file.
     *
     * @return the animation names
     */
    @Override
    public Set<String> animations() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(this.animations.keySet()));
    }

    /**
     * Find an animation by its name.
     *
     * @param name the animation's name
     * @return the animation, or empty if there is none by that name
     */
    public Optional<Animation> animation(final String name) {
        return Optional.ofNullable(this.animations.get(name));
    }

    /**
     * Find an animation baked for playing, by its name.
     *
     * @param name the animation's name
     * @return the baked animation, or empty if there is none by that name
     */
    public Optional<BakedAnimation> baked(final String name) {
        return Optional.ofNullable(this.baked.get(name));
    }

    /**
     * Return the textures, for the resource pack.
     *
     * @return the textures; a face's texture index points into this list
     */
    public List<Texture> textures() {
        return this.data.textures();
    }

    /**
     * Return the custom data stored on the Blockbench project.
     *
     * @return the project's metadata, empty when there is none
     */
    @Override
    public Metadata metadata() {
        return this.data.metadata();
    }

    /**
     * Return the box around every cube at rest.
     *
     * @return a copy of the box, in blocks, relative to the model's origin
     */
    @Override
    public BoundingBox boundingBox() {
        return this.boundingBox.clone();
    }

    /**
     * Return the problems found while loading.
     *
     * @return the load report, with warnings only, since a file with errors never becomes a
     *     blueprint
     */
    public LoadReport report() {
        return this.report;
    }

    @Override
    public Model spawn(final Location location) {
        return this.spawn(location, Blueprint::defaults);
    }

    @Override
    public Model spawn(final Location location, final Consumer<SpawnOptions> options) {
        return this.spawner.spawn(this, new LocationAnchor(location), options);
    }

    @Override
    public Model attach(final Entity entity) {
        return this.attach(entity, Blueprint::defaults);
    }

    @Override
    public Model attach(final Entity entity, final Consumer<SpawnOptions> options) {
        return this.spawner.spawn(this, new EntityAnchor(entity), options);
    }

    /**
     * Leave every spawn option at its default.
     *
     * @param options the options, untouched
     */
    private static void defaults(final SpawnOptions options) {
        // Nothing to change.
    }

    @Override
    public String toString() {
        return "Blueprint[" + this.key.asString() + ", revision " + this.revision + "]";
    }
}
