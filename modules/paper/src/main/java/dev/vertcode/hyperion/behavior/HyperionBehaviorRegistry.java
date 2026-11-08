package dev.vertcode.hyperion.behavior;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import net.kyori.adventure.key.Key;
import org.bukkit.plugin.Plugin;

import dev.vertcode.hyperion.api.model.behavior.BehaviorRegistry;
import dev.vertcode.hyperion.api.model.behavior.BehaviorType;

/**
 * Every behavior type plugins registered, by key.
 *
 * <p>Safe from any thread. Models read {@link #all()} when they spawn and when a reload adds a
 * bone, so a type registered later only reaches models spawned after it.
 */
public final class HyperionBehaviorRegistry implements BehaviorRegistry {

    private static final String TAKEN = "The behavior key %s is already registered by %s";

    private final Map<Key, RegisteredBehavior> types = new ConcurrentHashMap<>();

    @Override
    public void register(final Plugin owner, final BehaviorType<?> type) {
        RegisteredBehavior registered = new RegisteredBehavior(owner, type);
        RegisteredBehavior existing = this.types.putIfAbsent(type.key(), registered);
        if (existing != null) {
            String message = String.format(TAKEN, type.key().asString(), existing.owner().getName());
            throw new IllegalArgumentException(message);
        }
    }

    @Override
    public void unregisterAll(final Plugin owner) {
        this.types.values().removeIf(registered -> registered.owner().equals(owner));
    }

    @Override
    public Optional<BehaviorType<?>> find(final Key key) {
        RegisteredBehavior registered = this.types.get(key);

        return registered == null ? Optional.empty() : Optional.of(registered.type());
    }

    @Override
    public Collection<BehaviorType<?>> all() {
        List<BehaviorType<?>> all = new ArrayList<>(this.types.size());
        for (RegisteredBehavior registered : this.types.values()) {
            all.add(registered.type());
        }

        return List.copyOf(all);
    }
}
