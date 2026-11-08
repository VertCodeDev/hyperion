package dev.vertcode.hyperion.blueprint;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

import org.bukkit.plugin.Plugin;

import dev.vertcode.hyperion.api.model.source.FormatRegistry;
import dev.vertcode.hyperion.api.model.source.ModelFormat;
import dev.vertcode.hyperion.api.model.source.ModelSource;
import dev.vertcode.hyperion.blueprint.bbmodel.BbmodelFormat;

/**
 * The formats Hyperion can read: the built-in {@code .bbmodel} format and any a plugin adds.
 *
 * <p>A plugin's format is asked before the built-in one, so a plugin can take over a file type.
 * Formats are read far more often than they change, so the list is copied on write and any
 * thread may read it without a lock.
 */
public final class HyperionFormatRegistry implements FormatRegistry {

    private final List<FormatEntry> entries = new CopyOnWriteArrayList<>();

    /**
     * Create the registry with the built-in format.
     */
    public HyperionFormatRegistry() {
        this.entries.add(new FormatEntry(null, new BbmodelFormat()));
    }

    @Override
    public void register(final Plugin owner, final ModelFormat format) {
        this.entries.addFirst(new FormatEntry(owner, format));
    }

    @Override
    public void unregisterAll(final Plugin owner) {
        this.entries.removeIf(entry -> owner.equals(entry.owner()));
    }

    @Override
    public Optional<ModelFormat> find(final ModelSource source) {
        for (FormatEntry entry : this.entries) {
            if (entry.format().supports(source)) {
                return Optional.of(entry.format());
            }
        }

        return Optional.empty();
    }

    @Override
    public Collection<ModelFormat> all() {
        List<ModelFormat> formats = new ArrayList<>(this.entries.size());
        for (FormatEntry entry : this.entries) {
            formats.add(entry.format());
        }

        return Collections.unmodifiableList(formats);
    }
}
