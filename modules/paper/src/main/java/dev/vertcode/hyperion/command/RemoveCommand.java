package dev.vertcode.hyperion.command;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.incendo.cloud.annotation.specifier.Range;
import org.incendo.cloud.annotations.Argument;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.CommandDescription;
import org.incendo.cloud.annotations.Flag;
import org.incendo.cloud.annotations.Permission;
import org.incendo.cloud.paper.util.sender.PlayerSource;
import org.jspecify.annotations.Nullable;

import dev.vertcode.hyperion.api.model.EntityAnchor;
import dev.vertcode.hyperion.api.model.Model;
import dev.vertcode.hyperion.api.model.RemovalReason;
import dev.vertcode.hyperion.config.HyperionMessages;
import dev.vertcode.hyperion.model.ModelManager;

import static dev.vertcode.hyperion.config.HyperionMessages.value;

/**
 * {@code /hyperion near [radius]} and {@code /hyperion remove [radius]}: list the models around
 * you, each a click away from removal, and remove them.
 *
 * <p>Each model is removed on the thread that owns it, so this works for models in other
 * regions too.
 */
public final class RemoveCommand {

    private static final double NEAREST_RADIUS = 16d;
    private static final double NEAR_RADIUS = 32d;
    private static final int MAX_LINES = 20;
    private static final String DECIMALS = "%.1f";

    private final ModelManager models;

    /**
     * Create the commands.
     *
     * @param models the live models
     */
    public RemoveCommand(final ModelManager models) {
        this.models = models;
    }

    /**
     * Remove the nearest model, every model within a radius, or one model by id.
     *
     * @param source the player
     * @param radius how far around the player to remove every model, or null for the nearest
     * @param id the id of one model to remove, as the near list's links pass it
     */
    @Command(HyperionCommands.ROOT + " remove [radius]")
    @CommandDescription("Remove the nearest model, or every model within a radius")
    @Permission(HyperionPermissions.REMOVE)
    public void remove(
            final PlayerSource source,
            @Argument(value = "radius", suggestions = ValueSuggestions.RADII) @Range(min = "1", max = "256") final @Nullable Double radius,
            @Flag("id") final @Nullable UUID id
    ) {
        Player player = source.source();
        if (id != null) {
            Optional<Model> model = this.models.byId(id);
            if (model.isEmpty()) {
                HyperionMessages.REMOVE__GONE.send(player);
                return;
            }

            this.models.remove(model.get(), RemovalReason.REMOVED);
            HyperionMessages.REMOVE__REMOVED.send(player, value("count", 1));
            return;
        }

        double range = radius == null ? NEAREST_RADIUS : radius;
        List<Model> near = this.sorted(CommandText.where(player), range);
        if (near.isEmpty()) {
            HyperionMessages.REMOVE__NONE.send(player, value("radius", format(range)));
            return;
        }

        List<Model> chosen = radius == null ? near.subList(0, 1) : near;
        for (Model model : chosen) {
            this.models.remove(model, RemovalReason.REMOVED);
        }

        HyperionMessages.REMOVE__REMOVED.send(player, value("count", chosen.size()));
    }

    /**
     * List the models around the player, nearest first.
     *
     * @param source the player
     * @param radius how far around the player to look, 32 when left out
     */
    @Command(HyperionCommands.ROOT + " near [radius]")
    @CommandDescription("List the models around you, click one to remove it")
    @Permission(HyperionPermissions.REMOVE)
    public void near(
            final PlayerSource source,
            @Argument(value = "radius", suggestions = ValueSuggestions.RADII) @Range(min = "1", max = "256") final @Nullable Double radius
    ) {
        Player player = source.source();
        double range = radius == null ? NEAR_RADIUS : radius;
        Location here = CommandText.where(player);
        List<Model> near = this.sorted(here, range);
        if (near.isEmpty()) {
            HyperionMessages.NEAR__NONE.send(player, value("radius", format(range)));
            return;
        }

        TextComponent.Builder message = Component.text()
                .append(HyperionMessages.NEAR__HEADING.component(value("count", near.size())));
        for (Model model : near.subList(0, Math.min(MAX_LINES, near.size()))) {
            Component line = HyperionMessages.NEAR__ENTRY.component(
                    value("model", model.blueprint().key().asString()),
                    value("distance", format(model.location().distance(here))),
                    value("anchor", anchor(model))
            );
            String command = "/hyperion remove --id " + model.uniqueId();
            message.appendNewline().append(CommandText.clickable(line, command, HyperionMessages.NEAR__ENTRY_HOVER.component()));
        }

        player.sendMessage(message.build());
    }

    /**
     * Return the models within a radius, nearest first.
     *
     * @param center where to measure from
     * @param radius how far to look
     * @return the models
     */
    private List<Model> sorted(final Location center, final double radius) {
        List<Model> near = new ArrayList<>(this.models.near(center, radius));
        near.sort(Comparator.comparingDouble(model -> model.location().distanceSquared(center)));

        return near;
    }

    /**
     * Describe what carries a model.
     *
     * @param model the model
     * @return the description
     */
    private static Component anchor(final Model model) {
        if (model.anchor() instanceof EntityAnchor(Entity entity)) {
            return HyperionMessages.NEAR__ON_ENTITY.component(value("entity", entity.getName()));
        }

        return HyperionMessages.NEAR__STANDING.component();
    }

    /**
     * Write a distance with one decimal.
     *
     * @param blocks the distance
     * @return the text
     */
    private static String format(final double blocks) {
        return String.format(Locale.ROOT, DECIMALS, blocks);
    }
}
