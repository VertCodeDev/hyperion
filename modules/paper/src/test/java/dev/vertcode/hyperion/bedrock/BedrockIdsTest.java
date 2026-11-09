package dev.vertcode.hyperion.bedrock;

import java.util.UUID;

import net.kyori.adventure.key.Key;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BedrockIdsTest {

    @Test
    void namesModelsTheWayBedrockAccepts() {
        assertEquals("bosses_dragon", BedrockIds.slug(Key.key("hyperion", "bosses/dragon")));
        assertEquals("myplugin_pets_cat_v2", BedrockIds.slug(Key.key("myplugin", "pets/cat.v2")));
        assertEquals("hyperion:bosses_dragon", BedrockIds.identifier(Key.key("hyperion", "bosses/dragon")));
    }

    @Test
    void carriesTheModelInTheUuidAndReadsItBack() {
        Key key = Key.key("hyperion", "bosses/dragon");
        UUID uuid = BedrockIds.uuid(BedrockIds.id(key), 4242);

        assertEquals(BedrockIds.MARKER, uuid.getMostSignificantBits());
        assertEquals(BedrockIds.id(key), BedrockIds.model(uuid).orElseThrow());
        assertEquals(4242, (int) uuid.getLeastSignificantBits());
        assertTrue(BedrockIds.model(UUID.randomUUID()).isEmpty());
    }
}
