package dev.vertcode.hyperion.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringJoiner;

import net.kyori.adventure.key.Key;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.incendo.cloud.annotations.Argument;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.CommandDescription;
import org.incendo.cloud.annotations.Permission;
import org.incendo.cloud.annotations.suggestion.Suggestions;
import org.incendo.cloud.bukkit.data.SinglePlayerSelector;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.paper.util.sender.Source;
import org.jspecify.annotations.Nullable;

import dev.vertcode.hyperion.api.armor.ArmorPiece;
import dev.vertcode.hyperion.api.armor.ArmorSet;
import dev.vertcode.hyperion.api.armor.ArmorSlot;
import dev.vertcode.hyperion.armor.HyperionArmorRegistry;
import dev.vertcode.hyperion.armor.HyperionArmorSet;
import dev.vertcode.hyperion.blueprint.BlueprintKeys;
import dev.vertcode.hyperion.config.HyperionMessages;

import static dev.vertcode.hyperion.config.HyperionMessages.value;

/**
 * {@code /hyperion armor list} and {@code /hyperion armor give <player> <set> [piece]}: see the
 * armor sets and hand out their pieces.
 */
public final class ArmorCommand {

    private static final String SETS = "armor-sets";
    private static final String PIECES = "armor-pieces";

    private final Plugin plugin;
    private final HyperionArmorRegistry armor;

    /**
     * Create the command.
     *
     * @param plugin the plugin, for scheduling on the receiving player
     * @param armor the armor sets
     */
    public ArmorCommand(final Plugin plugin, final HyperionArmorRegistry armor) {
        this.plugin = plugin;
        this.armor = armor;
    }

    /**
     * List every set and its pieces.
     *
     * @param source who ran the command
     */
    @Command(HyperionCommands.ROOT + " armor list")
    @CommandDescription("List the armor sets and their pieces")
    @Permission(HyperionPermissions.ARMOR)
    public void list(final Source source) {
        CommandSender sender = source.source();
        List<HyperionArmorSet> sets = this.armor.sorted();
        if (sets.isEmpty()) {
            HyperionMessages.ARMOR__EMPTY.send(sender);
            return;
        }

        HyperionMessages.ARMOR__HEADING.send(sender, value("count", sets.size()));
        for (HyperionArmorSet set : sets) {
            HyperionMessages.ARMOR__ENTRY.send(sender, value("set", set.key().asString()), value("pieces", pieces(set)));
        }
    }

    /**
     * Give a player every piece of a set, or one piece.
     *
     * @param source who ran the command
     * @param target the player who gets the pieces
     * @param name the set, such as {@code frost} or {@code hyperion:frost}
     * @param pieceName the piece, such as {@code chestplate}, or null for every piece
     */
    @Command(HyperionCommands.ROOT + " armor give <player> <set> [piece]")
    @CommandDescription("Give a player a whole armor set, or one piece of it")
    @Permission(HyperionPermissions.ARMOR)
    public void give(
            final Source source,
            @Argument("player") final SinglePlayerSelector target,
            @Argument(value = "set", suggestions = SETS) final String name,
            @Argument(value = "piece", suggestions = PIECES) final @Nullable String pieceName
    ) {
        CommandSender sender = source.source();
        ArmorSet set = this.find(name);
        if (set == null) {
            HyperionMessages.ARMOR__UNKNOWN_SET.send(sender, value("set", name));
            return;
        }

        List<ArmorPiece> chosen = new ArrayList<>();
        if (pieceName == null) {
            chosen.addAll(set.pieces());
        } else {
            ArmorSlot slot = ArmorSlot.fromName(pieceName).orElse(null);
            ArmorPiece piece = slot == null ? null : set.piece(slot).orElse(null);
            if (piece == null) {
                HyperionMessages.ARMOR__UNKNOWN_PIECE.send(
                        sender,
                        value("set", set.key().asString()),
                        value("piece", pieceName),
                        value("pieces", pieces(set))
                );
                return;
            }

            chosen.add(piece);
        }

        List<ItemStack> items = new ArrayList<>();
        try {
            for (ArmorPiece piece : chosen) {
                items.add(piece.item());
            }
        } catch (IllegalStateException exception) {
            HyperionMessages.ARMOR__NOT_BUILT.send(sender);
            return;
        }

        Player player = target.single();
        String setName = set.key().asString();
        player.getScheduler().run(this.plugin, task -> {
            Map<Integer, ItemStack> left = player.getInventory().addItem(items.toArray(ItemStack[]::new));
            for (ItemStack item : left.values()) {
                player.getWorld().dropItemNaturally(CommandText.where(player), item);
            }

            HyperionMessages.ARMOR__RECEIVED.send(player, value("count", items.size()), value("set", setName));
        }, null);
        if (!sender.equals(player)) {
            HyperionMessages.ARMOR__GIVEN.send(
                    sender,
                    value("player", player.getName()),
                    value("count", items.size()),
                    value("set", setName)
            );
        }
    }

    /**
     * Suggest every set's key.
     *
     * @param context the command being typed
     * @param input what has been typed for this argument
     * @return the keys
     */
    @Suggestions(SETS)
    public List<String> suggestSets(final CommandContext<Source> context, final String input) {
        List<String> keys = new ArrayList<>();
        for (HyperionArmorSet set : this.armor.sorted()) {
            keys.add(set.key().asString());
        }

        return keys;
    }

    /**
     * Suggest every piece's name.
     *
     * @param context the command being typed
     * @param input what has been typed for this argument
     * @return the names
     */
    @Suggestions(PIECES)
    public List<String> suggestPieces(final CommandContext<Source> context, final String input) {
        List<String> names = new ArrayList<>();
        for (ArmorSlot slot : ArmorSlot.values()) {
            names.add(slot.piece());
        }

        return names;
    }

    /**
     * Find a set by its key, the {@code hyperion} namespace being the default.
     *
     * @param name the key as typed
     * @return the set, or null when there is none
     */
    private @Nullable ArmorSet find(final String name) {
        String lower = name.trim().toLowerCase(Locale.ROOT);
        String full = lower.contains(":") ? lower : BlueprintKeys.NAMESPACE + ":" + lower;
        if (!Key.parseable(full)) {
            return null;
        }

        return this.armor.find(Key.key(full)).orElse(null);
    }

    /**
     * Name a set's pieces.
     *
     * @param set the set
     * @return the names, comma separated
     */
    private static String pieces(final ArmorSet set) {
        StringJoiner names = new StringJoiner(", ");
        for (ArmorPiece piece : set.pieces()) {
            names.add(piece.slot().piece());
        }

        return names.toString();
    }
}
