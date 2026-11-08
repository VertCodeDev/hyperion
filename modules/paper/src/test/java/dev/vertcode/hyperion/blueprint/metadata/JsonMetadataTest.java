package dev.vertcode.hyperion.blueprint.metadata;

import java.util.List;
import java.util.Set;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import dev.vertcode.hyperion.api.model.metadata.Metadata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonMetadataTest {

    private static final String JSON = """
            {"name": "jaw", "tag": "head", "tags": ["a", 1, "b"], "speed": 2, "on": true, "nested": {"x": "y"}}
            """;

    @Test
    void readsEachKindOfValue() {
        Metadata metadata = JsonMetadata.of(parse(), Set.of("name"));

        assertEquals(Set.of("tag", "tags", "speed", "on", "nested"), metadata.fields());
        assertEquals("head", metadata.string("tag").orElseThrow());
        assertEquals(2.0, metadata.number("speed").orElseThrow());
        assertEquals(true, metadata.bool("on").orElseThrow());
        assertEquals(List.of("head"), metadata.strings("tag"));
        assertEquals(List.of("a", "b"), metadata.strings("tags"));
        assertEquals("y", metadata.section("nested").orElseThrow().string("x").orElseThrow());
    }

    @Test
    void returnsEmptyForTheWrongKind() {
        Metadata metadata = JsonMetadata.of(parse(), Set.of());

        assertTrue(metadata.string("speed").isEmpty());
        assertTrue(metadata.number("tag").isEmpty());
        assertTrue(metadata.bool("tag").isEmpty());
        assertTrue(metadata.section("tag").isEmpty());
        assertTrue(metadata.strings("missing").isEmpty());
    }

    @Test
    void keepsAPrivateCopy() {
        JsonObject source = parse();
        Metadata metadata = JsonMetadata.of(source, Set.of());
        source.addProperty("tag", "changed");

        assertEquals("head", metadata.string("tag").orElseThrow());
    }

    @Test
    void sharesTheEmptyMetadata() {
        assertSame(JsonMetadata.EMPTY, JsonMetadata.of(parse(), parse().keySet()));
    }

    private static JsonObject parse() {
        return JsonParser.parseString(JSON).getAsJsonObject();
    }
}
