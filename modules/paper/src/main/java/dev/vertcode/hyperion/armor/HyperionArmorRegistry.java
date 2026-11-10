package dev.vertcode.hyperion.armor;

import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

import net.kyori.adventure.key.Key;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jspecify.annotations.Nullable;

import dev.vertcode.hyperion.api.armor.ArmorPiece;
import dev.vertcode.hyperion.api.armor.ArmorRegistry;
import dev.vertcode.hyperion.api.armor.ArmorSet;
import dev.vertcode.hyperion.api.armor.ArmorSlot;
import dev.vertcode.hyperion.api.armor.WornArmor;
import dev.vertcode.hyperion.config.ArmorSettings;
import dev.vertcode.hyperion.pack.armor.ArmorBuild;

/**
 * The loaded armor sets, and the way into what players wear.
 *
 * <p>Sets load from {@code plugins/Hyperion/armor} before every pack build. They are swapped in
 * whole, so readers on any thread see the old sets or the new ones, never a mix.
 */
public final class HyperionArmorRegistry implements ArmorRegistry {

    private static final String SEPARATOR = "|";

    private final Path folder;
    private final Path cache;
    private final String minecraftVersion;
    private final ArmorItems items;
    private final Logger logger;
    private volatile Map<Key, HyperionArmorSet> sets = Map.of();
    private volatile ArmorSettings settings;
    private volatile @Nullable ArmorTracker tracker;

    /**
     * Create the registry. Nothing loads until {@link #load()}.
     *
     * @param folder the folder sets are read from
     * @param cache the folder the client jar is kept in
     * @param minecraftVersion the server's version as Mojang names it
     * @param items builds the pieces' items
     * @param settings the armor settings
     * @param logger where problems are logged
     */
    public HyperionArmorRegistry(
            final Path folder,
            final Path cache,
            final String minecraftVersion,
            final ArmorItems items,
            final ArmorSettings settings,
            final Logger logger
    ) {
        this.folder = folder;
        this.cache = cache;
        this.minecraftVersion = minecraftVersion;
        this.items = items;
        this.settings = settings;
        this.logger = logger;
    }

    /**
     * Use the tracker for {@link #worn(Player)}.
     *
     * @param next the tracker
     */
    public void useTracker(final ArmorTracker next) {
        this.tracker = next;
    }

    /**
     * Use new armor settings from the next pack build on.
     *
     * @param next the settings
     */
    public void configure(final ArmorSettings next) {
        this.settings = next;
    }

    /**
     * Read every set from the folder again and publish them. Reads files, so call it off the
     * tick threads.
     *
     * @return the sets now loaded
     */
    public List<HyperionArmorSet> load() {
        List<HyperionArmorSet> loaded = ArmorLoader.loadFolder(this.folder, this.items, this.logger);
        Map<Key, HyperionArmorSet> next = new LinkedHashMap<>();
        for (HyperionArmorSet set : loaded) {
            next.put(set.key(), set);
        }

        this.sets = Map.copyOf(next);

        return loaded;
    }

    /**
     * Return what the pack builder needs for the sets loaded now.
     *
     * @return the build input
     */
    public ArmorBuild build() {
        return new ArmorBuild(this.sorted(), this.settings, this.minecraftVersion, this.cache, this.logger);
    }

    /**
     * Return the folder sets are read from.
     *
     * @return the folder
     */
    public Path folder() {
        return this.folder;
    }

    @Override
    public Optional<ArmorSet> find(final Key key) {
        return Optional.ofNullable(this.sets.get(key));
    }

    @Override
    public Collection<ArmorSet> all() {
        return List.copyOf(this.sorted());
    }

    /**
     * Return every loaded set as Hyperion's own type, sorted by key.
     *
     * @return the sets
     */
    public List<HyperionArmorSet> sorted() {
        return this.sets.values().stream()
                .sorted((first, second) -> first.key().asString().compareTo(second.key().asString()))
                .toList();
    }

    @Override
    public Optional<ArmorPiece> pieceOf(final @Nullable ItemStack item) {
        return Optional.ofNullable(this.piece(item));
    }

    @Override
    public boolean isArmor(final @Nullable ItemStack item) {
        return this.piece(item) != null;
    }

    /**
     * Find the piece an item is, as Hyperion's own type.
     *
     * @param item the item, or null
     * @return the piece, or null for anything that is no piece of a loaded set
     */
    public @Nullable HyperionArmorPiece piece(final @Nullable ItemStack item) {
        if (item == null || item.isEmpty() || !item.hasItemMeta()) {
            return null;
        }

        ItemMeta meta = item.getItemMeta();
        String tag = meta == null ? null : meta.getPersistentDataContainer().get(ArmorItems.TAG, PersistentDataType.STRING);
        if (tag == null) {
            return null;
        }

        int split = tag.lastIndexOf(SEPARATOR);
        if (split < 0 || !Key.parseable(tag.substring(0, split))) {
            return null;
        }

        HyperionArmorSet set = this.sets.get(Key.key(tag.substring(0, split)));
        if (set == null) {
            return null;
        }

        for (ArmorSlot slot : ArmorSlot.values()) {
            if (slot.name().equals(tag.substring(split + 1))) {
                return set.piece(slot).map(HyperionArmorPiece.class::cast).orElse(null);
            }
        }

        return null;
    }

    @Override
    public WornArmor worn(final Player player) {
        ArmorTracker current = this.tracker;

        return current == null ? HyperionWornArmor.empty(player.getUniqueId()) : current.worn(player.getUniqueId());
    }
}
