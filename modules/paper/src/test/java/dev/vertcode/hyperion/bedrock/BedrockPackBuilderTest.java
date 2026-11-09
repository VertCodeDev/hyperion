package dev.vertcode.hyperion.bedrock;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import dev.vertcode.hyperion.blueprint.Blueprint;
import dev.vertcode.hyperion.blueprint.Fixtures;
import dev.vertcode.hyperion.blueprint.LoadReport;
import dev.vertcode.hyperion.blueprint.data.Cube;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BedrockPackBuilderTest {

    @Test
    void writesEveryFileAModelNeedsAndListsIt() throws IOException {
        Blueprint blueprint = Fixtures.blueprint("groups_layout", new LoadReport("groups_layout.bbmodel"));
        BedrockPack pack = BedrockPackBuilder.build(List.of(blueprint));
        Map<String, byte[]> files = unzip(pack.zip());
        String slug = BedrockIds.slug(blueprint.key());

        assertTrue(files.containsKey("manifest.json"));
        assertTrue(files.containsKey("entity/" + slug + ".entity.json"));
        assertTrue(files.containsKey("models/entity/" + slug + ".geo.json"));
        assertTrue(files.containsKey("animations/" + slug + ".animation.json"));
        assertTrue(files.containsKey("animation_controllers/" + slug + ".animation_controllers.json"));
        assertTrue(files.containsKey("textures/hyperion/" + slug + ".png"));

        JsonObject manifest = JsonParser.parseString(pack.manifest()).getAsJsonObject();
        JsonObject model = manifest.getAsJsonArray("models").get(0).getAsJsonObject();
        assertEquals(BedrockIds.id(blueprint.key()), model.get("id").getAsInt());
        assertEquals(blueprint.animations().size(), model.getAsJsonArray("animations").size());
    }

    @Test
    void mirrorsXAsBlockbenchExportsToBedrock() throws IOException {
        Blueprint blueprint = Fixtures.blueprint("groups_layout", new LoadReport("groups_layout.bbmodel"));
        Map<String, byte[]> files = unzip(BedrockPackBuilder.build(List.of(blueprint)).zip());
        String slug = BedrockIds.slug(blueprint.key());
        JsonObject geometry = json(files, "models/entity/" + slug + ".geo.json")
                .getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
        JsonObject bone = geometry.getAsJsonArray("bones").get(0).getAsJsonObject();
        JsonObject cube = bone.getAsJsonArray("cubes").get(0).getAsJsonObject();
        JsonArray origin = cube.getAsJsonArray("origin");
        Cube first = blueprint.blueprintBones().getFirst().cubes().getFirst();

        assertEquals(-first.to().x(), origin.get(0).getAsFloat(), 1.0e-3f);
        assertEquals(first.from().y(), origin.get(1).getAsFloat(), 1.0e-3f);
    }

    @Test
    void writesTheSamePackForTheSameModels() throws IOException {
        Blueprint blueprint = Fixtures.blueprint("groups_layout", new LoadReport("groups_layout.bbmodel"));

        assertArrayEquals(BedrockPackBuilder.build(List.of(blueprint)).zip(), BedrockPackBuilder.build(List.of(blueprint)).zip());
    }

    private static JsonObject json(final Map<String, byte[]> files, final String path) {
        return JsonParser.parseString(new String(Objects.requireNonNull(files.get(path)), StandardCharsets.UTF_8)).getAsJsonObject();
    }

    private static Map<String, byte[]> unzip(final byte[] zip) throws IOException {
        Map<String, byte[]> files = new HashMap<>();
        try (ZipInputStream stream = new ZipInputStream(new ByteArrayInputStream(zip))) {
            ZipEntry entry;
            while ((entry = stream.getNextEntry()) != null) {
                files.put(entry.getName(), stream.readAllBytes());
            }
        }

        return files;
    }
}
