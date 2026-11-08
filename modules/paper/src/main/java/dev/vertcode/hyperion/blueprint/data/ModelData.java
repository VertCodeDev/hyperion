package dev.vertcode.hyperion.blueprint.data;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import dev.vertcode.hyperion.api.model.metadata.Metadata;

/**
 * Everything read from one Blockbench file, still in Blockbench's own shape.
 *
 * <p>Groups, cubes and locators are looked up by the id Blockbench gave them; the outliner tree
 * is {@link #roots()} plus each group's children. Every map keeps the order of the file.
 *
 * @param name the project name
 * @param format the Blockbench format, such as {@code free}, {@code bedrock} or
 *     {@code java_block}
 * @param resolutionWidth the project's UV width
 * @param resolutionHeight the project's UV height
 * @param groups every group, by id
 * @param cubes every cube, by id
 * @param locators every locator and null object, by id
 * @param roots the ids at the top of the outliner, in order
 * @param textures the textures; a face's texture index points into this list
 * @param animations the animations
 * @param metadata the project fields Blockbench itself does not use
 */
public record ModelData(
        String name,
        String format,
        int resolutionWidth,
        int resolutionHeight,
        Map<String, Group> groups,
        Map<String, Cube> cubes,
        Map<String, Locator> locators,
        List<String> roots,
        List<Texture> textures,
        List<Animation> animations,
        Metadata metadata
) {

    /**
     * Copy every collection, keeping the order of the file.
     */
    public ModelData {
        groups = Collections.unmodifiableMap(new LinkedHashMap<>(groups));
        cubes = Collections.unmodifiableMap(new LinkedHashMap<>(cubes));
        locators = Collections.unmodifiableMap(new LinkedHashMap<>(locators));
        roots = List.copyOf(roots);
        textures = List.copyOf(textures);
        animations = List.copyOf(animations);
    }
}
