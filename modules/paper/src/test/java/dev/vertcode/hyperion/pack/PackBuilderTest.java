package dev.vertcode.hyperion.pack;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.kyori.adventure.key.Key;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import dev.vertcode.hyperion.api.version.MinecraftVersion;
import dev.vertcode.hyperion.blueprint.Blueprint;
import dev.vertcode.hyperion.blueprint.Fixtures;
import dev.vertcode.hyperion.blueprint.LoadReport;
import dev.vertcode.hyperion.config.PackHosting;
import dev.vertcode.hyperion.config.PackSettings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PackBuilderTest {

    private static final Key ROTATIONS = Key.key("hyperion", "rotations");
    private static final MinecraftVersion MODERN = MinecraftVersion.parse("1.21.4");
    private static final MinecraftVersion LEGACY = MinecraftVersion.parse("1.21.1");
    private static final PackSettings SETTINGS = new PackSettings(
            PackHosting.SELF_HOST, 8163, "", "", true, false, true, "paper", 10_000
    );
    private static final float EPSILON = 1.0e-4f;

    @Test
    void keepsCubesAnItemModelCanHoldInTheBonesModel() throws IOException {
        ResourcePack pack = build(MODERN);
        JsonArray elements = json(pack, "assets/hyperion/models/rotations/arm.json").getAsJsonArray("elements");

        assertEquals(2, elements.size());
        assertEquals(array(7, 8, 7), elements.get(0).getAsJsonObject().get("from"));
        assertEquals(array(9, 10, 9), elements.get(0).getAsJsonObject().get("to"));

        JsonObject tilt = elements.get(1).getAsJsonObject().getAsJsonObject("rotation");
        assertEquals(22.5f, tilt.get("angle").getAsFloat());
        assertEquals("x", tilt.get("axis").getAsString());
        assertEquals(array(8, 12, 8), tilt.get("origin"));
    }

    @Test
    void givesOddlyTurnedCubesAPartOfTheirOwn() throws IOException {
        ResourcePack pack = build(MODERN);
        List<ModelPart> parts = pack.parts(ROTATIONS, "Arm");

        assertEquals(List.of("hyperion:rotations/arm", "hyperion:rotations/arm_1", "hyperion:rotations/arm_2"),
                parts.stream().map(ModelPart::model).toList());

        ModelPart odd = parts.get(1);
        assertTrue(new Vector3f(0.125f).equals(new Vector3f(odd.offset()), EPSILON));
        assertTrue(new Quaternionf().rotationY((float) Math.toRadians(30)).equals(new Quaternionf(odd.rotation()), EPSILON));

        JsonObject element = json(pack, "assets/hyperion/models/rotations/arm_1.json")
                .getAsJsonArray("elements").get(0).getAsJsonObject();
        assertEquals(array(7, 7, 7), element.get("from"));
        assertFalse(element.has("rotation"));
    }

    @Test
    void shrinksABoneTooBigForAnItemModel() throws IOException {
        ResourcePack pack = build(MODERN);
        JsonObject element = json(pack, "assets/hyperion/models/rotations/giant.json")
                .getAsJsonArray("elements").get(0).getAsJsonObject();

        assertNumbers(element.getAsJsonArray("from"), -16, 8, 5.6f);
        assertNumbers(element.getAsJsonArray("to"), 32, 12.8f, 10.4f);
        assertEquals(1f / 0.6f, pack.parts(ROTATIONS, "giant").getFirst().scale(), EPSILON);
    }

    @Test
    void movesUvsToMinecraftsRange() throws IOException {
        JsonObject faces = json(build(MODERN), "assets/hyperion/models/rotations/arm.json")
                .getAsJsonArray("elements").get(0).getAsJsonObject().getAsJsonObject("faces");

        assertEquals(array(0, 0, 8, 8), faces.getAsJsonObject("north").get("uv"));
        assertEquals(array(0, 0, 8, 4), faces.getAsJsonObject("south").get("uv"));
        assertEquals("#1", faces.getAsJsonObject("south").get("texture").getAsString());
        assertEquals(0, faces.getAsJsonObject("south").get("tintindex").getAsInt());
    }

    @Test
    void animatesOnlyTexturesWithMoreThanOneFrame() throws IOException {
        Map<String, byte[]> files = files(build(MODERN));

        assertTrue(files.containsKey("assets/hyperion/textures/item/rotations/0.png"));
        assertFalse(files.containsKey("assets/hyperion/textures/item/rotations/1.png.mcmeta"));
        JsonObject animation = json(files, "assets/hyperion/textures/item/rotations/2.png.mcmeta").getAsJsonObject("animation");
        assertEquals(3, animation.get("frametime").getAsInt());
    }

    @Test
    void writesItemDefinitionsFrom1214() throws IOException {
        Map<String, byte[]> files = files(build(MODERN));
        JsonObject model = json(files, "assets/hyperion/items/rotations/arm.json").getAsJsonObject("model");

        assertEquals("hyperion:rotations/arm", model.get("model").getAsString());
        assertEquals("minecraft:dye", model.getAsJsonArray("tints").get(0).getAsJsonObject().get("type").getAsString());
        assertFalse(files.containsKey("assets/minecraft/models/item/paper.json"));
    }

    @Test
    void writesCustomModelDataOverridesBefore1214() throws IOException {
        ResourcePack pack = build(LEGACY);
        Map<String, byte[]> files = files(pack);
        JsonArray overrides = json(files, "assets/minecraft/models/item/paper.json").getAsJsonArray("overrides");

        assertEquals(4, overrides.size());
        assertEquals(10_000, overrides.get(0).getAsJsonObject().getAsJsonObject("predicate").get("custom_model_data").getAsInt());
        assertEquals(10_000, pack.parts(ROTATIONS, "Arm").getFirst().customModelData());
        assertTrue(files.keySet().stream().noneMatch(path -> path.contains("/items/")));
    }

    @Test
    void statesTheFormatAsARangeFrom1219() throws IOException {
        JsonObject old = json(build(MODERN), "pack.mcmeta").getAsJsonObject("pack");
        MinecraftVersion version = MinecraftVersion.parse("1.21.9");
        ResourcePack pack = PackBuilder.build(List.of(blueprint()), SETTINGS, version, new PackFormat(69, 0));
        JsonObject ranged = json(pack, "pack.mcmeta").getAsJsonObject("pack");

        assertEquals(46, old.get("pack_format").getAsInt());
        assertFalse(old.has("min_format"));
        assertEquals(array(69, 0), ranged.get("min_format"));
        assertEquals(array(69, 0), ranged.get("max_format"));
    }

    @Test
    void buildsTheSameZipTwice() throws IOException {
        assertEquals(build(MODERN).sha1(), build(MODERN).sha1());
    }

    private static ResourcePack build(final MinecraftVersion version) throws IOException {
        return PackBuilder.build(List.of(blueprint()), SETTINGS, version, PackFormat.known(version));
    }

    private static Blueprint blueprint() throws IOException {
        return Fixtures.blueprint("rotations", new LoadReport("rotations.bbmodel"));
    }

    private static JsonObject json(final ResourcePack pack, final String path) throws IOException {
        return json(files(pack), path);
    }

    private static JsonObject json(final Map<String, byte[]> files, final String path) {
        byte[] bytes = Objects.requireNonNull(files.get(path), () -> "The pack has no " + path);

        return JsonParser.parseString(new String(bytes, StandardCharsets.UTF_8)).getAsJsonObject();
    }

    private static Map<String, byte[]> files(final ResourcePack pack) throws IOException {
        Map<String, byte[]> files = new TreeMap<>();
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(pack.zip()))) {
            for (ZipEntry entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
                files.put(entry.getName(), zip.readAllBytes());
            }
        }

        return files;
    }

    private static void assertNumbers(final JsonArray actual, final float... expected) {
        assertEquals(expected.length, actual.size());
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], actual.get(i).getAsFloat(), EPSILON);
        }
    }

    private static JsonArray array(final float... values) {
        JsonArray array = new JsonArray();
        for (float value : values) {
            array.add(value);
        }

        return array;
    }
}
