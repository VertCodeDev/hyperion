package dev.vertcode.hyperion.pack;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
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
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import dev.vertcode.hyperion.api.version.MinecraftVersion;
import dev.vertcode.hyperion.blueprint.Blueprint;
import dev.vertcode.hyperion.blueprint.Fixtures;
import dev.vertcode.hyperion.blueprint.IssueSeverity;
import dev.vertcode.hyperion.blueprint.LoadReport;
import dev.vertcode.hyperion.blueprint.data.Cube;
import dev.vertcode.hyperion.blueprint.data.CubeFace;
import dev.vertcode.hyperion.blueprint.data.FaceSide;
import dev.vertcode.hyperion.config.PackHosting;
import dev.vertcode.hyperion.config.PackSettings;
import dev.vertcode.hyperion.skin.SkinMap;
import dev.vertcode.hyperion.skin.SkinPixel;
import dev.vertcode.hyperion.skin.SkinVariant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkinPackTest {

    private static final Key PLAYER = Key.key("hyperion", "player");
    private static final MinecraftVersion MODERN = MinecraftVersion.parse("1.21.4");
    private static final MinecraftVersion LEGACY = MinecraftVersion.parse("1.21.1");
    private static final PackSettings SETTINGS = new PackSettings(
            PackHosting.SELF_HOST, 8163, "", "", true, false, true, "paper", 10_000
    );
    private static final int HEAD_PIXELS = 6 * 8 * 8;
    private static final float EPSILON = 1.0e-4f;

    @Test
    void loadsTheTemplateAsAPlayerModelWithoutWarnings() throws IOException {
        LoadReport report = new LoadReport("player-template.bbmodel");
        Blueprint blueprint = Fixtures.shipped("player-template.bbmodel", "player", report);

        assertTrue(blueprint.skinned());
        assertEquals(0, report.count(IssueSeverity.WARNING));
        assertTrue(blueprint.animations().containsAll(List.of("idle", "walk", "wave", "sit", "hands_up")));
        assertNotNull(blueprint.fileSkin());
    }

    @Test
    void drawsEverySkinPixelOfTheHeadAsItsOwnFace() throws IOException {
        ResourcePack pack = build(MODERN);
        SkinMap skin = Objects.requireNonNull(pack.parts(PLAYER, "head").getFirst().skin());

        // The head itself is always drawn; the hat over it can be see-through.
        assertEquals(HEAD_PIXELS, skin.solid().size());
        assertEquals(HEAD_PIXELS, skin.optional().size());

        Map<String, byte[]> files = files(pack);
        JsonArray elements = json(files, "assets/hyperion/models/player/head.json").getAsJsonArray("elements");
        assertEquals(HEAD_PIXELS, elements.size());

        JsonObject definition = json(files, "assets/hyperion/items/player/head.json").getAsJsonObject("model");
        JsonArray models = definition.getAsJsonArray("models");
        assertEquals("minecraft:composite", definition.get("type").getAsString());
        assertEquals(1 + HEAD_PIXELS, models.size());
        assertEquals(1 + HEAD_PIXELS, models.get(0).getAsJsonObject().getAsJsonArray("tints").size());

        JsonObject first = models.get(1).getAsJsonObject();
        assertEquals("minecraft:condition", first.get("type").getAsString());
        assertEquals(0, first.get("index").getAsInt());
        JsonObject tint = first.getAsJsonObject("on_true").getAsJsonArray("tints").get(0).getAsJsonObject();
        assertEquals(HEAD_PIXELS, tint.get("index").getAsInt());
    }

    @Test
    void leavesTheSkinTextureOutOfThePack() throws IOException {
        Map<String, byte[]> files = files(build(MODERN));

        assertTrue(files.containsKey("assets/hyperion/textures/item/skin_pixel.png"));
        assertFalse(files.containsKey("assets/hyperion/textures/item/player/0.png"));
    }

    @Test
    void drawsBothArmShapesOnlyForTheirSkins() throws IOException {
        SkinMap arm = Objects.requireNonNull(build(MODERN).parts(PLAYER, "right_arm").getFirst().skin());

        assertTrue(arm.solid().isEmpty());
        assertTrue(arm.optional().stream().anyMatch(pixel -> pixel.variant() == SkinVariant.SLIM));
        assertTrue(arm.optional().stream().anyMatch(pixel -> pixel.variant() == SkinVariant.WIDE));
    }

    @Test
    void showsTheFilesOwnSkinBefore1214() throws IOException {
        ResourcePack pack = build(LEGACY);
        Map<String, byte[]> files = files(pack);

        assertNull(pack.parts(PLAYER, "head").getFirst().skin());
        assertTrue(files.containsKey("assets/hyperion/textures/item/player/0.png"));
        assertFalse(files.containsKey("assets/hyperion/textures/item/skin_pixel.png"));
    }

    @Test
    void putsThePixelsOfAFaceWhereItsTextureWould() {
        Cube head = new Cube(
                "head", new Vector3f(-4, 24, -4), new Vector3f(4, 32, 4), new Vector3f(), new Vector3f(), 0f, Map.of(), true
        );
        List<SkinQuad> quads = new ArrayList<>();
        SkinFaces.cut(head, FaceSide.NORTH, new CubeFace(8, 8, 16, 16, 0, 0), 64, 64, SkinVariant.ANY, quads);

        assertEquals(64, quads.size());

        // The first pixel of the face's UV box is its top corner on the side of the higher X.
        SkinQuad first = quads.getFirst();
        assertEquals(new SkinPixel(8, 8, SkinVariant.ANY), first.pixel());
        assertTrue(new Vector3f(3, 31, -4).equals(new Vector3f(first.from()), EPSILON));
        assertTrue(new Vector3f(4, 32, -4).equals(new Vector3f(first.to()), EPSILON));
        assertFalse(first.optional());
    }

    @Test
    void followsAMirroredUvBox() {
        Cube top = new Cube(
                "head", new Vector3f(-4, 24, -4), new Vector3f(4, 32, 4), new Vector3f(), new Vector3f(), 0f, Map.of(), true
        );
        List<SkinQuad> quads = new ArrayList<>();
        SkinFaces.cut(top, FaceSide.UP, new CubeFace(16, 8, 8, 0, 0, 0), 64, 64, SkinVariant.ANY, quads);

        // The box runs backwards both ways, so its first pixel in the skin, (8, 0), is drawn last.
        SkinQuad corner = quads.stream().filter(quad -> quad.pixel().x() == 8 && quad.pixel().y() == 0).findFirst().orElseThrow();
        assertTrue(new Vector3f(3, 32, 3).equals(new Vector3f(corner.from()), EPSILON));
        assertTrue(new Vector3f(4, 32, 4).equals(new Vector3f(corner.to()), EPSILON));
    }

    private static ResourcePack build(final MinecraftVersion version) throws IOException {
        Blueprint blueprint = Fixtures.shipped("player-template.bbmodel", "player", new LoadReport("player-template.bbmodel"));

        return PackBuilder.build(List.of(blueprint), SETTINGS, version, PackFormat.known(version));
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
}
