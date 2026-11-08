package dev.vertcode.hyperion.blueprint;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.kyori.adventure.key.Key;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.BoundingBox;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

import dev.vertcode.hyperion.api.model.source.ModelSource;
import dev.vertcode.hyperion.blueprint.data.Animation;
import dev.vertcode.hyperion.blueprint.data.Cube;
import dev.vertcode.hyperion.blueprint.data.Group;
import dev.vertcode.hyperion.blueprint.data.Locator;
import dev.vertcode.hyperion.blueprint.data.ModelData;
import dev.vertcode.hyperion.blueprint.metadata.JsonMetadata;

/**
 * Turn {@link ModelData} into a {@link Blueprint}: the bone tree in blocks, unique names and the
 * bounding box.
 *
 * <p>Blockbench works in pixels, 16 to a block, with Y up, and turns everything in ZYX order
 * (see {@link BlockbenchRotation}). Bones and locators are converted to blocks here; cubes stay
 * in pixels for the resource pack. A {@code java_block} project spans 0 to 16 on X and Z, so it
 * is moved by 8 pixels to put its center on the model's origin like every other format.
 */
final class BlueprintFactory {

    private static final float PIXELS_PER_BLOCK = 16f;
    private static final String JAVA_BLOCK = "java_block";
    private static final String ROOT_BONE = "root";

    private static final String RENAMED = "the name is already taken, so it is renamed to '%s'";
    private static final String LOOSE_CUBES = "%d cube(s) sit outside every group; they are put in a bone named '%s'";

    private final ModelData data;
    private final LoadReport report;
    private final Vector3fc shift;
    private final Map<String, BlueprintBone> bones = new LinkedHashMap<>();
    private final Map<String, Vector3fc> pivotsInPixels = new LinkedHashMap<>();
    private final Map<String, BlueprintLocator> locators = new LinkedHashMap<>();
    private final Vector3f min = new Vector3f(Float.POSITIVE_INFINITY);
    private final Vector3f max = new Vector3f(Float.NEGATIVE_INFINITY);

    private BlueprintFactory(final ModelData data, final LoadReport report) {
        this.data = data;
        this.report = report;
        this.shift = JAVA_BLOCK.equals(data.format()) ? new Vector3f(-8f, 0f, -8f) : new Vector3f();
    }

    /**
     * Build a blueprint from a file that loaded without errors.
     *
     * @param key the key it is registered under
     * @param owner the plugin that registered it, or null for Hyperion's own folder
     * @param revision how many times the key has been loaded
     * @param source where it was loaded from
     * @param data the file as read
     * @param report the report to add naming warnings to
     * @return the blueprint
     */
    static Blueprint build(
            final Key key,
            final @Nullable Plugin owner,
            final int revision,
            final ModelSource source,
            final ModelData data,
            final LoadReport report
    ) {
        BlueprintFactory factory = new BlueprintFactory(data, report);
        factory.walk(data.roots(), null, new Matrix4f());

        return new Blueprint(
                key,
                owner,
                revision,
                source,
                data,
                factory.bones,
                factory.locators,
                factory.animations(),
                factory.boundingBox(),
                report
        );
    }

    /**
     * Build the bones and locators under one level of the outliner.
     *
     * @param ids the ids at this level, in order
     * @param parent the bone they hang from, or null at the top
     * @param parentWorld the parent's rest transform in pixels, for the bounding box
     */
    private void walk(final List<String> ids, final @Nullable BlueprintBone parent, final Matrix4fc parentWorld) {
        List<Cube> looseCubes = new ArrayList<>();
        for (String id : ids) {
            Group group = this.data.groups().get(id);
            if (group != null) {
                this.bone(group, parent, parentWorld);
                continue;
            }

            Locator locator = this.data.locators().get(id);
            if (locator != null) {
                this.locator(locator, parent);
                continue;
            }

            Cube cube = this.data.cubes().get(id);
            if (cube != null && parent == null) {
                looseCubes.add(this.shifted(cube));
            }
        }

        if (looseCubes.isEmpty()) {
            return;
        }

        String name = this.unique(ROOT_BONE, this.bones, "bone");
        this.report.warn("outliner", String.format(LOOSE_CUBES, looseCubes.size(), name));
        this.bones.put(name, new BlueprintBone(
                name,
                null,
                new Vector3f(),
                new Quaternionf(),
                looseCubes,
                true,
                JsonMetadata.EMPTY
        ));
        this.pivotsInPixels.put(name, new Vector3f());
        this.include(looseCubes, parentWorld);
    }

    /**
     * Build a bone, then everything inside it.
     *
     * @param group the group to turn into a bone
     * @param parent the bone it hangs from, or null at the top
     * @param parentWorld the parent's rest transform in pixels
     */
    private void bone(final Group group, final @Nullable BlueprintBone parent, final Matrix4fc parentWorld) {
        String name = this.unique(group.name(), this.bones, "group");
        Vector3f pivot = new Vector3f(group.origin()).add(this.shift);
        Quaternionf rotation = BlockbenchRotation.of(group.rotation());

        List<Cube> cubes = new ArrayList<>();
        for (String child : group.children()) {
            Cube cube = this.data.cubes().get(child);
            if (cube != null) {
                cubes.add(this.shifted(cube));
            }
        }

        BlueprintBone bone = new BlueprintBone(
                name,
                parent,
                new Vector3f(pivot).div(PIXELS_PER_BLOCK),
                rotation,
                cubes,
                group.visible(),
                group.metadata()
        );
        this.bones.put(name, bone);
        this.pivotsInPixels.put(name, pivot);

        // Blockbench nests a group around its own origin: children turn with it at rest.
        Matrix4f world = new Matrix4f(parentWorld)
                .translate(pivot)
                .rotate(rotation)
                .translate(-pivot.x, -pivot.y, -pivot.z);
        this.include(cubes, world);
        this.walk(group.children(), bone, world);
    }

    /**
     * Build a locator, positioned relative to the bone it hangs from.
     *
     * @param locator the locator as read
     * @param parent the bone it hangs from, or null at the top
     */
    private void locator(final Locator locator, final @Nullable BlueprintBone parent) {
        String name = this.unique(locator.name(), this.locators, "locator");
        Vector3fc origin = new Vector3f();
        Vector3fc parentPivot = parent == null ? origin : this.pivotsInPixels.getOrDefault(parent.name(), origin);
        Vector3f position = new Vector3f(locator.position())
                .add(this.shift)
                .sub(parentPivot)
                .div(PIXELS_PER_BLOCK);

        this.locators.put(name, new BlueprintLocator(
                name,
                parent,
                position,
                BlockbenchRotation.of(locator.rotation()),
                locator.metadata()
        ));
    }

    /**
     * Return the animations by name, renaming any that repeat a name.
     *
     * @return the animations in the order of the file
     */
    private Map<String, Animation> animations() {
        Map<String, Animation> animations = new LinkedHashMap<>();
        for (Animation animation : this.data.animations()) {
            animations.put(this.unique(animation.name(), animations, "animation"), animation);
        }

        return animations;
    }

    /**
     * Grow the bounding box around cubes, turned the way they rest.
     *
     * @param cubes the cubes, in pixels
     * @param world the rest transform of the bone holding them, in pixels
     */
    private void include(final List<Cube> cubes, final Matrix4fc world) {
        Vector3f corner = new Vector3f();
        for (Cube cube : cubes) {
            Vector3fc origin = cube.origin();
            Matrix4f transform = new Matrix4f(world)
                    .translate(origin)
                    .rotate(BlockbenchRotation.of(cube.rotation()))
                    .translate(-origin.x(), -origin.y(), -origin.z());
            float inflate = cube.inflate();
            for (int i = 0; i < 8; i++) {
                corner.set(
                        (i & 1) == 0 ? cube.from().x() - inflate : cube.to().x() + inflate,
                        (i & 2) == 0 ? cube.from().y() - inflate : cube.to().y() + inflate,
                        (i & 4) == 0 ? cube.from().z() - inflate : cube.to().z() + inflate
                );
                transform.transformPosition(corner);
                this.min.min(corner);
                this.max.max(corner);
            }
        }
    }

    /**
     * Return the bounding box in blocks.
     *
     * @return the box, empty at the origin for a model without cubes
     */
    private BoundingBox boundingBox() {
        if (this.min.x > this.max.x) {
            return new BoundingBox();
        }

        return new BoundingBox(
                this.min.x / PIXELS_PER_BLOCK,
                this.min.y / PIXELS_PER_BLOCK,
                this.min.z / PIXELS_PER_BLOCK,
                this.max.x / PIXELS_PER_BLOCK,
                this.max.y / PIXELS_PER_BLOCK,
                this.max.z / PIXELS_PER_BLOCK
        );
    }

    /**
     * Move a cube of a {@code java_block} project onto the model's origin. Other formats are
     * returned as they are.
     *
     * @param cube the cube as read
     * @return the cube in the model's space
     */
    private Cube shifted(final Cube cube) {
        if (this.shift.lengthSquared() == 0f) {
            return cube;
        }

        return new Cube(
                cube.name(),
                new Vector3f(cube.from()).add(this.shift),
                new Vector3f(cube.to()).add(this.shift),
                new Vector3f(cube.origin()).add(this.shift),
                cube.rotation(),
                cube.inflate(),
                cube.faces(),
                cube.visible()
        );
    }

    /**
     * Return a name not yet in use, adding {@code _2}, {@code _3} and so on to a repeat.
     *
     * @param name the name from the file
     * @param taken the names already in use
     * @param kind what is being named, for the warning
     * @return the unique name
     */
    private String unique(final String name, final Map<String, ?> taken, final String kind) {
        if (!taken.containsKey(name)) {
            return name;
        }

        int number = 2;
        while (taken.containsKey(name + "_" + number)) {
            number++;
        }

        String unique = name + "_" + number;
        this.report.warn(kind + " '" + name + "'", String.format(RENAMED, unique));

        return unique;
    }
}
