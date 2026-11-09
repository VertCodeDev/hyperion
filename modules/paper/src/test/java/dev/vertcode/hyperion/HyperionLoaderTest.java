package dev.vertcode.hyperion;

import java.util.List;
import java.util.Properties;

import org.junit.jupiter.api.Test;

import dev.vertcode.hyperion.api.version.MinecraftVersion;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HyperionLoaderTest {

    private static final Properties VERSIONS = new Properties();

    static {
        VERSIONS.setProperty("invui1", "1.49");
        VERSIONS.setProperty("invui2", "2.5.1");
        VERSIONS.setProperty("invui2For261", "2.1.1");
        VERSIONS.setProperty("invui2For262", "2.3.2");
    }

    @Test
    void picksInvUi1WithTheInventoryModuleOfEach121Version() {
        assertEquals(List.of(
                "xyz.xenondevs.invui:invui-core:1.49",
                "xyz.xenondevs.invui:inventory-access-r20:jar:remapped-mojang:1.49"
        ), HyperionLoader.invui(MinecraftVersion.parse("1.21.1"), VERSIONS));
        assertEquals("r21", HyperionLoader.inventoryAccess(MinecraftVersion.parse("1.21.3")));
        assertEquals("r22", HyperionLoader.inventoryAccess(MinecraftVersion.parse("1.21.4")));
        assertEquals("r24", HyperionLoader.inventoryAccess(MinecraftVersion.parse("1.21.8")));
        assertEquals("r25", HyperionLoader.inventoryAccess(MinecraftVersion.parse("1.21.10")));
        assertEquals("r26", HyperionLoader.inventoryAccess(MinecraftVersion.parse("1.21.11")));
    }

    @Test
    void picksTheInvUi2ReleaseOfEach26Line() {
        assertEquals(List.of("xyz.xenondevs.invui:invui:2.1.1"), HyperionLoader.invui(MinecraftVersion.parse("26.1.2"), VERSIONS));
        assertEquals(List.of("xyz.xenondevs.invui:invui:2.3.2"), HyperionLoader.invui(MinecraftVersion.parse("26.2"), VERSIONS));
        assertEquals(List.of("xyz.xenondevs.invui:invui:2.5.1"), HyperionLoader.invui(MinecraftVersion.parse("26.3"), VERSIONS));
    }
}
