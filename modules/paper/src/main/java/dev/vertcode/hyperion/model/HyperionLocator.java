package dev.vertcode.hyperion.model;

import org.bukkit.Location;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Quaternionfc;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

import dev.vertcode.hyperion.api.model.Model;
import dev.vertcode.hyperion.api.model.locator.Locator;
import dev.vertcode.hyperion.api.model.locator.LocatorBlueprint;
import dev.vertcode.hyperion.blueprint.BlueprintLocator;
import dev.vertcode.hyperion.render.DisplayTransform;

/**
 * A locator on a live model, placed from its bone's last frame. Safe to read from any thread.
 */
final class HyperionLocator implements Locator {

    private final HyperionModel model;
    private final BlueprintLocator blueprint;
    private final @Nullable HyperionBone bone;

    /**
     * Create a locator.
     *
     * @param model the model it belongs to
     * @param blueprint the blueprint's locator
     * @param bone the bone it hangs from, or null at the top of the model
     */
    HyperionLocator(final HyperionModel model, final BlueprintLocator blueprint, final @Nullable HyperionBone bone) {
        this.model = model;
        this.blueprint = blueprint;
        this.bone = bone;
    }

    @Override
    public LocatorBlueprint blueprint() {
        return this.blueprint;
    }

    @Override
    public Model model() {
        return this.model;
    }

    @Override
    public String name() {
        return this.blueprint.name();
    }

    @Override
    public Location location() {
        Vector3f position = this.matrix().transformPosition(new Vector3f());

        return this.model.location().add(position.x, position.y, position.z);
    }

    @Override
    public Quaternionfc rotation() {
        return DisplayTransform.of(this.matrix()).rotation();
    }

    /**
     * Work out the locator's matrix from its bone's, or the model's root without a bone.
     *
     * @return the matrix
     */
    private Matrix4f matrix() {
        if (this.bone == null) {
            Matrix4fc root = this.model.rootWorld();

            return new Matrix4f(root).translate(this.blueprint.position()).rotate(this.blueprint.rotation());
        }

        return new Matrix4f(this.bone.world())
                .translate(this.bone.blueprintBone().pivot())
                .translate(this.blueprint.position())
                .rotate(this.blueprint.rotation());
    }
}
