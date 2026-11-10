package dev.vertcode.hyperion.armor;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Base64;
import java.util.UUID;

import javax.imageio.ImageIO;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import dev.vertcode.hyperion.api.version.MinecraftVersion;
import dev.vertcode.hyperion.blueprint.LoadReport;

/**
 * Build armor {@code .bbmodel} files in code, for tests.
 */
public final class ArmorFixtures {

    private static final String[] SIDES = {"north", "east", "south", "west", "up", "down"};

    private final JsonArray textures = new JsonArray();
    private final JsonArray elements = new JsonArray();
    private final JsonArray outliner = new JsonArray();

    /**
     * Add a texture of one color.
     *
     * @param name the texture's name
     * @param width its width
     * @param height its height
     * @param argb its color
     * @return the texture's index
     */
    public int texture(final String name, final int width, final int height, final int argb) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                image.setRGB(x, y, argb);
            }
        }

        JsonObject texture = new JsonObject();
        texture.addProperty("name", name + ".png");
        texture.addProperty("uv_width", width);
        texture.addProperty("uv_height", height);
        texture.addProperty("source", "data:image/png;base64," + Base64.getEncoder().encodeToString(png(image)));
        this.textures.add(texture);

        return this.textures.size() - 1;
    }

    /**
     * Add a cube, every face textured with the whole texture.
     *
     * @param from the lowest corner
     * @param to the highest corner
     * @param texture the texture's index
     * @param inflate how far the cube grows
     * @return the cube's id
     */
    public String cube(final float[] from, final float[] to, final int texture, final float inflate) {
        JsonObject faces = new JsonObject();
        for (String side : SIDES) {
            JsonArray uv = new JsonArray();
            uv.add(0);
            uv.add(0);
            uv.add(4);
            uv.add(4);
            JsonObject face = new JsonObject();
            face.add("uv", uv);
            face.addProperty("texture", texture);
            faces.add(side, face);
        }

        String id = UUID.randomUUID().toString();
        JsonObject cube = new JsonObject();
        cube.addProperty("name", "cube");
        cube.addProperty("type", "cube");
        cube.addProperty("uuid", id);
        cube.add("from", array(from));
        cube.add("to", array(to));
        cube.add("origin", array(from));
        cube.addProperty("inflate", inflate);
        cube.add("faces", faces);
        this.elements.add(cube);

        return id;
    }

    /**
     * Add a bone at the top of the outliner.
     *
     * @param name the bone's name
     * @param origin its pivot
     * @param cubes the ids of its cubes
     */
    public void bone(final String name, final float[] origin, final String... cubes) {
        JsonArray children = new JsonArray();
        for (String cube : cubes) {
            children.add(cube);
        }

        JsonObject group = new JsonObject();
        group.addProperty("name", name);
        group.addProperty("uuid", UUID.randomUUID().toString());
        group.add("origin", array(origin));
        group.add("children", children);
        this.outliner.add(group);
    }

    /**
     * Build the file.
     *
     * @return the parsed {@code .bbmodel}
     */
    public JsonObject build() {
        JsonObject meta = new JsonObject();
        meta.addProperty("format_version", "5.0");
        meta.addProperty("model_format", "free");
        JsonObject resolution = new JsonObject();
        resolution.addProperty("width", 16);
        resolution.addProperty("height", 16);
        JsonObject model = new JsonObject();
        model.add("meta", meta);
        model.add("resolution", resolution);
        model.add("textures", this.textures);
        model.add("elements", this.elements);
        model.add("outliner", this.outliner);

        return model;
    }

    /**
     * Build the test set {@code hyperion:frost}: a helmet, a chestplate with a glowing second
     * cube, a right arm and a right boot, all at vanilla's pivots, a flat outer texture and a
     * flat helmet icon.
     *
     * @return the file
     */
    public static JsonObject frost() {
        ArmorFixtures model = new ArmorFixtures();
        int plain = model.texture("frost", 4, 4, 0xFF3366CC);
        int glow = model.texture("frost_glow", 4, 4, 0xFFFFFFFF);
        model.texture("flat_outer", 64, 32, 0xFF22AA22);
        model.texture("icon_helmet", 16, 16, 0xFF3366CC);
        model.bone("helmet", new float[] {0, 24, 0}, model.cube(new float[] {-4, 24, -4}, new float[] {4, 32, 4}, plain, 1f));
        model.bone(
                "chestplate",
                new float[] {0, 24, 0},
                model.cube(new float[] {-4, 12, -3}, new float[] {4, 24, 3}, plain, 0f),
                model.cube(new float[] {-1, 18, -4}, new float[] {1, 20, -3}, glow, 0f)
        );
        model.bone("right_arm", new float[] {5, 22, 0}, model.cube(new float[] {4, 12, -2}, new float[] {8, 24, 2}, plain, 0f));
        model.bone("right_boot", new float[] {1.9f, 12, 0}, model.cube(new float[] {0, 0, -2}, new float[] {4, 4, 2}, plain, 0f));

        return model.build();
    }

    /**
     * Load a set from a file and its settings.
     *
     * @param json the {@code .bbmodel}
     * @param yaml the settings, empty for none
     * @param items builds the set's items
     * @return the set
     */
    public static HyperionArmorSet set(final JsonObject json, final String yaml, final ArmorItems items) {
        YamlConfiguration settings = new YamlConfiguration();
        try {
            settings.loadFromString(yaml);
        } catch (InvalidConfigurationException exception) {
            throw new IllegalArgumentException(exception);
        }

        return ArmorLoader.read("frost.bbmodel", json, settings, items, new LoadReport("frost.bbmodel")).orElseThrow();
    }

    /**
     * Load the test set {@code hyperion:frost} without settings.
     *
     * @param version the version items are built for
     * @return the set
     */
    public static HyperionArmorSet frostSet(final MinecraftVersion version) {
        return set(frost(), "", new ArmorItems(version, "paper"));
    }

    /**
     * Write a point as a JSON array.
     *
     * @param values the point
     * @return the array
     */
    private static JsonArray array(final float[] values) {
        JsonArray array = new JsonArray();
        for (float value : values) {
            array.add(value);
        }

        return array;
    }

    /**
     * Write an image as PNG bytes.
     *
     * @param image the image
     * @return the bytes
     */
    private static byte[] png(final BufferedImage image) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try {
            ImageIO.write(image, "png", bytes);
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }

        return bytes.toByteArray();
    }
}
