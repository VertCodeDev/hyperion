package dev.vertcode.hyperion.behavior;

import java.lang.reflect.Proxy;

import net.kyori.adventure.key.Key;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.Test;

import dev.vertcode.hyperion.api.model.behavior.BehaviorMatcher;
import dev.vertcode.hyperion.api.model.behavior.BehaviorType;
import dev.vertcode.hyperion.api.model.behavior.BoneBehavior;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HyperionBehaviorRegistryTest {

    private static final Key GLOW = Key.key("test", "glow");

    @Test
    void findsARegisteredTypeAndRefusesItsKeyTwice() {
        HyperionBehaviorRegistry registry = new HyperionBehaviorRegistry();
        Plugin owner = plugin("Owner");
        registry.register(owner, type(GLOW));

        assertEquals(GLOW, registry.find(GLOW).orElseThrow().key());
        assertEquals(1, registry.all().size());
        assertThrows(IllegalArgumentException.class, () -> registry.register(plugin("Other"), type(GLOW)));
    }

    @Test
    void dropsAPluginsTypesWhenItDisables() {
        HyperionBehaviorRegistry registry = new HyperionBehaviorRegistry();
        Plugin owner = plugin("Owner");
        Plugin other = plugin("Other");
        registry.register(owner, type(GLOW));
        registry.register(other, type(Key.key("test", "spin")));

        registry.unregisterAll(owner);

        assertTrue(registry.find(GLOW).isEmpty());
        assertEquals(1, registry.all().size());
    }

    private static BehaviorType<BoneBehavior> type(final Key key) {
        return BehaviorType.of(key, BoneBehavior.class, BehaviorMatcher.nameEquals("head"), bone -> new BoneBehavior() {
        });
    }

    // A plugin is only ever equal to itself, which is what reference equality checks here.
    @SuppressWarnings("ReferenceEquality")
    private static Plugin plugin(final String name) {
        return (Plugin) Proxy.newProxyInstance(
                Plugin.class.getClassLoader(),
                new Class<?>[] {Plugin.class},
                (proxy, method, arguments) -> switch (method.getName()) {
                    case "getName" -> name;
                    case "toString" -> name;
                    case "equals" -> proxy == arguments[0];
                    case "hashCode" -> System.identityHashCode(proxy);
                    default -> null;
                }
        );
    }
}
