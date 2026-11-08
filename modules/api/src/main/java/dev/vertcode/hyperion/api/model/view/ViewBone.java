package dev.vertcode.hyperion.api.model.view;

import org.jetbrains.annotations.ApiStatus;

import dev.vertcode.hyperion.api.model.bone.BoneAppearance;

/**
 * One bone as one player sees it. Values left unset fall back to the shared bone.
 */
@ApiStatus.Experimental
@ApiStatus.NonExtendable
public interface ViewBone extends BoneAppearance<ViewBone> {

    /**
     * Return the bone's name.
     *
     * @return the name
     */
    String name();
}
