package dev.vertcode.hyperion.pack.armor;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.imageio.ImageIO;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import dev.vertcode.hyperion.api.armor.ArmorSlot;
import dev.vertcode.hyperion.api.version.MinecraftVersion;
import dev.vertcode.hyperion.armor.ArmorFixtures;
import dev.vertcode.hyperion.armor.ArmorPackInfo;
import dev.vertcode.hyperion.armor.HyperionArmorSet;
import dev.vertcode.hyperion.config.ArmorSettings;
import dev.vertcode.hyperion.config.PackHosting;
import dev.vertcode.hyperion.config.PackSettings;
import dev.vertcode.hyperion.pack.PackBuilder;
import dev.vertcode.hyperion.pack.PackFormat;
import dev.vertcode.hyperion.pack.ResourcePack;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArmorPackTest {

    private static final PackSettings SETTINGS = new PackSettings(
            PackHosting.SELF_HOST, 8163, "", "", true, false, true, "paper", 10_000
    );
    private static final ArmorSettings FLAT = new ArmorSettings(false, 32, 16);
    private static final ArmorSettings SHADER = new ArmorSettings(true, 32, 16);
    private static final Logger LOGGER = Logger.getLogger("ArmorPackTest");

    @TempDir
    Path cache;

    @Test
    void picksTheHelmetsModelByWhereItIsDrawnFrom1214() throws IOException {
        Map<String, byte[]> files = this.build("1.21.4", FLAT).files();
        JsonObject helmet = json(files, "assets/hyperion/items/armor/frost/helmet.json").getAsJsonObject("model");

        assertEquals("minecraft:select", helmet.get("type").getAsString());
        assertEquals("minecraft:display_context", helmet.get("property").getAsString());
        assertEquals("head", helmet.getAsJsonArray("cases").get(0).getAsJsonObject().get("when").getAsString());
        assertEquals("hyperion:armor/frost/helmet", helmet.getAsJsonArray("cases").get(0).getAsJsonObject()
                .getAsJsonObject("model").get("model").getAsString());
        assertEquals("hyperion:armor/frost/helmet_icon", helmet.getAsJsonObject("fallback").get("model").getAsString());

        JsonObject chestplate = json(files, "assets/hyperion/items/armor/frost/chestplate.json").getAsJsonObject("model");
        assertEquals("minecraft:model", chestplate.get("type").getAsString());
        assertEquals("hyperion:armor/frost/chestplate_icon", chestplate.get("model").getAsString());
        assertTrue(json(files, "assets/hyperion/models/armor/frost/chestplate_icon.json").has("elements"));
    }

    @Test
    void writesAnEquipmentAssetWithTheFlatLookFrom1214() throws IOException {
        Built built = this.build("1.21.4", FLAT);
        JsonObject layers = json(built.files(), "assets/hyperion/equipment/frost.json").getAsJsonObject("layers");
        JsonArray outer = layers.getAsJsonArray("humanoid");

        assertEquals(1, outer.size());
        assertEquals("hyperion:frost", outer.get(0).getAsJsonObject().get("texture").getAsString());
        assertFalse(layers.has("humanoid_leggings"));
        assertTrue(built.files().containsKey("assets/hyperion/textures/entity/equipment/humanoid/frost.png"));
        assertFalse(json(built.files(), "pack.mcmeta").has("overlays"));
    }

    @Test
    void keepsTrimLooksWhenAddingCustomModelDataBefore1214() throws IOException {
        Built built = this.build("1.21.2", FLAT);
        JsonArray overrides = json(built.files(), "assets/minecraft/models/item/netherite_chestplate.json").getAsJsonArray("overrides");
        int number = built.info().customModelData(built.set().key(), ArmorSlot.CHEST).orElseThrow();

        assertEquals(11, overrides.size());
        assertEquals("minecraft:item/netherite_chestplate_netherite_darker_trim",
                overrides.get(2).getAsJsonObject().get("model").getAsString());
        assertEquals(number, overrides.get(10).getAsJsonObject().getAsJsonObject("predicate").get("custom_model_data").getAsInt());
        assertTrue(built.files().containsKey("assets/hyperion/models/equipment/frost.json"));
    }

    @Test
    void addsTheShadersLayersAndOverlayWhenTheShaderIsOn() throws IOException {
        this.fakeClient("26.3");
        Built built = this.build("26.3", SHADER);
        Map<String, byte[]> files = built.files();
        JsonArray outer = json(files, "assets/hyperion/equipment/frost.json").getAsJsonObject("layers").getAsJsonArray("humanoid");
        String vertex = new String(files.get(ArmorPackResult.OVERLAY + "/assets/minecraft/shaders/core/entity.vsh"), StandardCharsets.UTF_8);
        JsonObject overlay = json(files, "pack.mcmeta").getAsJsonObject("overlays").getAsJsonArray("entries").get(0).getAsJsonObject();
        BufferedImage flat = image(files, "assets/hyperion/textures/entity/equipment/humanoid/frost.png");

        assertEquals(7, outer.size());
        assertEquals("hyperion:frost_3d_1", outer.get(1).getAsJsonObject().get("texture").getAsString());
        assertTrue(vertex.contains("hy_armor(hy_Position, hy_Normal, hy_UV0, hy_Color);"));
        assertTrue(files.containsKey(ArmorPackResult.OVERLAY + "/assets/minecraft/shaders/core/entity.fsh"));
        assertEquals(ArmorPackResult.OVERLAY, overlay.get("directory").getAsString());
        assertTrue(overlay.has("min_format"));
        assertEquals(ArmorImages.argb(72, 89, 80, 3), flat.getRGB(0, 0));
    }

    @Test
    void drawsSetsAsCellsOfTheLeatherTextureOn1211() throws IOException {
        this.fakeClient("1.21.1");
        Built built = this.build("1.21.1", FLAT);
        Map<String, byte[]> files = built.files();
        String atlas = ArmorPackResult.OVERLAY + "/assets/minecraft/textures/models/armor/leather_layer_1.png";
        JsonArray base = json(files, "assets/minecraft/models/item/paper.json").getAsJsonArray("overrides");
        JsonArray leather = json(files, "assets/minecraft/models/item/leather_chestplate.json").getAsJsonArray("overrides");

        assertEquals(128, image(files, atlas).getWidth());
        assertTrue(files.containsKey(ArmorPackResult.OVERLAY + "/assets/minecraft/shaders/core/rendertype_armor_cutout_no_cull.vsh"));
        assertEquals(ArmorPackInfo.color(1), built.info().setColor(built.set().key()).orElseThrow());
        assertEquals("hyperion:armor/frost/helmet", base.get(0).getAsJsonObject().get("model").getAsString());
        assertEquals(11, leather.size());
        assertTrue(json(files, "pack.mcmeta").getAsJsonObject("overlays").getAsJsonArray("entries").get(0)
                .getAsJsonObject().has("formats"));
    }

    /**
     * Build a pack with the test set.
     *
     * @param version the version
     * @param settings the armor settings
     * @return the files, the armor numbers and the set
     * @throws IOException if the zip cannot be read
     */
    private Built build(final String version, final ArmorSettings settings) throws IOException {
        MinecraftVersion parsed = MinecraftVersion.parse(version);
        HyperionArmorSet set = ArmorFixtures.frostSet(parsed);
        ArmorBuild armor = new ArmorBuild(List.of(set), settings, version, this.cache, LOGGER);
        ResourcePack pack = PackBuilder.build(List.of(), SETTINGS, parsed, PackFormat.known(parsed), armor);

        return new Built(unzip(pack.zip()), pack.armor(), set);
    }

    /**
     * Put a client jar in the cache with the files Hyperion reads from it.
     *
     * @param version the version it is for
     * @throws IOException if it cannot be written
     */
    private void fakeClient(final String version) throws IOException {
        Path jar = this.cache.resolve("minecraft-" + version + ".jar");
        try (OutputStream stream = Files.newOutputStream(jar); ZipOutputStream zip = new ZipOutputStream(stream)) {
            entry(zip, "assets/minecraft/shaders/core/entity.vsh", ShaderSamples.VERTEX.getBytes(StandardCharsets.UTF_8));
            entry(zip, "assets/minecraft/shaders/core/entity.fsh", ShaderSamples.FRAGMENT.getBytes(StandardCharsets.UTF_8));
            entry(zip, "assets/minecraft/shaders/core/rendertype_armor_cutout_no_cull.vsh",
                    ShaderSamples.VERTEX.getBytes(StandardCharsets.UTF_8));
            byte[] leather = ArmorImages.png(ArmorImages.blank(64, 32));
            entry(zip, "assets/minecraft/textures/models/armor/leather_layer_1.png", leather);
            entry(zip, "assets/minecraft/textures/models/armor/leather_layer_2.png", leather);
        }
    }

    /**
     * Add a file to a zip.
     *
     * @param zip the zip
     * @param path the file's path
     * @param bytes its contents
     * @throws IOException if it cannot be written
     */
    private static void entry(final ZipOutputStream zip, final String path, final byte[] bytes) throws IOException {
        zip.putNextEntry(new ZipEntry(path));
        zip.write(bytes);
        zip.closeEntry();
    }

    /**
     * Read a zip's files.
     *
     * @param zip the zip
     * @return the files by path
     * @throws IOException if it cannot be read
     */
    private static Map<String, byte[]> unzip(final byte[] zip) throws IOException {
        Map<String, byte[]> files = new TreeMap<>();
        try (ZipInputStream stream = new ZipInputStream(new ByteArrayInputStream(zip))) {
            for (ZipEntry entry = stream.getNextEntry(); entry != null; entry = stream.getNextEntry()) {
                files.put(entry.getName(), stream.readAllBytes());
            }
        }

        return files;
    }

    /**
     * Read a JSON file from a pack.
     *
     * @param files the pack's files
     * @param path the file's path
     * @return the parsed object
     */
    private static JsonObject json(final Map<String, byte[]> files, final String path) {
        byte[] bytes = files.get(path);
        assertTrue(bytes != null, "the pack has no " + path);

        return JsonParser.parseString(new String(bytes, StandardCharsets.UTF_8)).getAsJsonObject();
    }

    /**
     * Read an image from a pack.
     *
     * @param files the pack's files
     * @param path the image's path
     * @return the image
     * @throws IOException if it cannot be read
     */
    private static BufferedImage image(final Map<String, byte[]> files, final String path) throws IOException {
        byte[] bytes = files.get(path);
        assertTrue(bytes != null, "the pack has no " + path);

        return ImageIO.read(new ByteArrayInputStream(bytes));
    }

    /**
     * What a test build gave.
     *
     * @param files the pack's files
     * @param info the armor numbers
     * @param set the test set
     */
    private record Built(Map<String, byte[]> files, ArmorPackInfo info, HyperionArmorSet set) {
    }
}
