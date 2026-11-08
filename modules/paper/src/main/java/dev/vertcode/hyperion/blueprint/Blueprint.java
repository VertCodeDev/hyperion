package dev.vertcode.hyperion.blueprint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import net.kyori.adventure.key.Key;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.BoundingBox;
import org.jspecify.annotations.Nullable;

import dev.vertcode.hyperion.api.model.bone.BoneBlueprint;
import dev.vertcode.hyperion.api.model.metadata.Metadata;
import dev.vertcode.hyperion.api.model.source.ModelSource;
import dev.vertcode.hyperion.blueprint.data.Animation;
import dev.vertcode.hyperion.blueprint.data.ModelData;
import dev.vertcode.hyperion.blueprint.data.Texture;

/**
 * A loaded model: the bone tree, locators, textures and animations of one Blockbench file.
 *
 * <p>Immutable and shared by every model spawned from it, so any thread may read it. A reload
 * builds a new blueprint with a higher revision rather than changing this one. The accessors
 * match {@code ModelBlueprint} in the api, which this class will implement once models can be
 * spawned.
 */
public final class Blueprint {

    private final Key key;
    private final @Nullable Plugin owner;
    private final int revision;
    private final ModelSource source;
    private final ModelData data;
    private final List<BlueprintBone> bones;
    private final Map<String, BlueprintBone> bonesByName;
    private final List<BlueprintLocator> locators;
    private final Map<String, BlueprintLocator> locatorsByName;
    private final Map<String, Animation> animations;
    private final BoundingBox boundingBox;
    private final LoadReport report;

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
     * @param boundingBox the box around every cube at rest, in blocks
     * @param report the problems found while loading
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
            final BoundingBox boundingBox,
            final LoadReport report
    ) {
        this.key = key;
        this.owner = owner;
        this.revision = revision;
        this.source = source;
        this.data = data;
        this.bones = List.copyOf(bones.values());
        this.bonesByName = Map.copyOf(bones);
        this.locators = List.copyOf(locators.values());
        this.locatorsByName = Map.copyOf(locators);
        this.animations = Collections.unmodifiableMap(new LinkedHashMap<>(animations));
        this.boundingBox = boundingBox.clone();
        this.report = report;
    }

    /**
     * Return the key this blueprint is registered under.
     *
     * @return the key
     */
    public Key key() {
        return this.key;
    }

    /**
     * Return the plugin that registered this blueprint.
     *
     * @return the owner, or null for a model from Hyperion's own {@code models/} folder
     */
    public @Nullable Plugin owner() {
        return this.owner;
    }

    /**
     * Return how many times this key has been loaded, starting at 1.
     *
     * @return the revision
     */
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

    /**
     * Return every bone, parents before their children.
     *
     * @return the bones
     */
    public List<BlueprintBone> bones() {
        return this.bones;
    }

    /**
     * Return the bones at the top of the tree.
     *
     * @return the root bones
     */
    public List<BlueprintBone> rootBones() {
        List<BlueprintBone> roots = new ArrayList<>();
        for (BlueprintBone bone : this.bones) {
            if (bone.parent().isEmpty()) {
                roots.add(bone);
            }
        }

        return Collections.unmodifiableList(roots);
    }

    /**
     * Find a bone by its name.
     *
     * @param name the bone's name
     * @return the bone, or empty if there is none by that name
     */
    public Optional<BoneBlueprint> bone(final String name) {
        return Optional.ofNullable(this.bonesByName.get(name));
    }

    /**
     * Return every locator.
     *
     * @return the locators
     */
    public List<BlueprintLocator> locators() {
        return this.locators;
    }

    /**
     * Find a locator by its name.
     *
     * @param name the locator's name
     * @return the locator, or empty if there is none by that name
     */
    public Optional<BlueprintLocator> locator(final String name) {
        return Optional.ofNullable(this.locatorsByName.get(name));
    }

    /**
     * Return the names of the animations, in the order of the file.
     *
     * @return the animation names
     */
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
    public Metadata metadata() {
        return this.data.metadata();
    }

    /**
     * Return the box around every cube at rest.
     *
     * @return a copy of the box, in blocks, relative to the model's origin
     */
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
    public String toString() {
        return "Blueprint[" + this.key.asString() + ", revision " + this.revision + "]";
    }
}
