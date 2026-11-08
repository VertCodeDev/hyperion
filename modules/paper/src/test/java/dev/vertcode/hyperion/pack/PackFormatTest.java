package dev.vertcode.hyperion.pack;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import dev.vertcode.hyperion.api.version.MinecraftVersion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PackFormatTest {

    @Test
    void readsTheFormatFromVersionJsonBefore1219() {
        String json = "{\"id\": \"1.21.4\", \"pack_version\": {\"resource\": 46, \"data\": 61}}";

        assertEquals(new PackFormat(46, 0), PackFormat.fromVersionJson(JsonParser.parseString(json)).orElseThrow());
    }

    @Test
    void readsTheMajorAndMinorFormatFrom1219() {
        String json = "{\"pack_version\": {\"resource_major\": 69, \"resource_minor\": 0, \"data_major\": 88}}";

        assertEquals(new PackFormat(69, 0), PackFormat.fromVersionJson(JsonParser.parseString(json)).orElseThrow());
    }

    @Test
    void saysNothingForAFileWithoutIt() {
        assertTrue(PackFormat.fromVersionJson(JsonParser.parseString("{\"id\": \"1.21\"}")).isEmpty());
    }

    @Test
    void fallsBackToTheTable() {
        assertEquals(34, PackFormat.known(MinecraftVersion.parse("1.21.1")).major());
        assertEquals(46, PackFormat.known(MinecraftVersion.parse("1.21.4")).major());
        assertEquals(75, PackFormat.known(MinecraftVersion.parse("1.21.11")).major());
        assertEquals(84, PackFormat.known(MinecraftVersion.parse("26.3")).major());
    }
}
