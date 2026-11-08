package dev.vertcode.hyperion.blueprint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

import dev.vertcode.hyperion.api.model.bone.BoneBlueprint;
import dev.vertcode.hyperion.api.model.metadata.Metadata;
import dev.vertcode.hyperion.blueprint.data.Cube;

/**
 * A bone of a loaded blueprint, with the cubes it renders.
 *
 * <p>Built by {@link BlueprintFactory} parents first: a child is added to its parent while the
 * blueprint is being built, and nothing changes once the factory returns.
 */
public final class BlueprintBone implements BoneBlueprint {

    private final String name;
    private final @Nullable BlueprintBone parent;
    private final List<BoneBlueprint> children = new ArrayList<>();
    private final Vector3fc pivot;
    private final Quaternionfc rotation;
    private final List<Cube> cubes;
    private final boolean visibleByDefault;
    private final Metadata metadata;

    /**
     * Create a bone and hang it from its parent.
     *
     * @param name the unique name
     * @param parent the parent, or null for a root bone
     * @param pivot the pivot in blocks, copied
     * @param rotation the rest rotation, copied
     * @param cubes the cubes in Blockbench pixels
     * @param visibleByDefault whether the bone was visible when the model was saved
     * @param metadata the custom data on the group
     */
    BlueprintBone(
            final String name,
            final @Nullable BlueprintBone parent,
            final Vector3fc pivot,
            final Quaternionfc rotation,
            final List<Cube> cubes,
            final boolean visibleByDefault,
            final Metadata metadata
    ) {
        this.name = name;
        this.parent = parent;
        this.pivot = new Vector3f(pivot);
        this.rotation = new Quaternionf(rotation);
        this.cubes = List.copyOf(cubes);
        this.visibleByDefault = visibleByDefault;
        this.metadata = metadata;
        if (parent != null) {
            parent.children.add(this);
        }
    }

    @Override
    public String name() {
        return this.name;
    }

    @Override
    public Optional<BoneBlueprint> parent() {
        return Optional.ofNullable(this.parent);
    }

    @Override
    public List<BoneBlueprint> children() {
        return Collections.unmodifiableList(this.children);
    }

    @Override
    public Vector3fc pivot() {
        return this.pivot;
    }

    @Override
    public Quaternionfc rotation() {
        return this.rotation;
    }

    @Override
    public boolean hasGeometry() {
        return !this.cubes.isEmpty();
    }

    @Override
    public boolean visibleByDefault() {
        return this.visibleByDefault;
    }

    @Override
    public Metadata metadata() {
        return this.metadata;
    }

    /**
     * Return the cubes this bone renders, for the resource pack.
     *
     * @return the cubes in Blockbench pixels, relative to the model's origin
     */
    public List<Cube> cubes() {
        return this.cubes;
    }

    /**
     * Return how deep this bone sits in the tree.
     *
     * @return 0 for a root bone
     */
    public int depth() {
        return this.parent == null ? 0 : this.parent.depth() + 1;
    }

    @Override
    public String toString() {
        return "BlueprintBone[" + this.name + "]";
    }
}
