package dev.vertcode.hyperion.command;

import org.incendo.cloud.annotation.specifier.FlagYielding;
import org.incendo.cloud.annotation.specifier.Greedy;
import org.incendo.cloud.annotations.Argument;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.CommandDescription;
import org.incendo.cloud.annotations.Flag;
import org.incendo.cloud.annotations.Permission;
import org.incendo.cloud.paper.util.sender.PlayerSource;
import org.jspecify.annotations.Nullable;

import dev.vertcode.hyperion.ui.Gallery;

/**
 * {@code /hyperion gallery [filter] [--effects] [--armor]}: browse every model, every effect or
 * every armor set in a menu.
 */
public final class GalleryCommand {

    private final Gallery gallery;

    /**
     * Create the command.
     *
     * @param gallery the gallery
     */
    public GalleryCommand(final Gallery gallery) {
        this.gallery = gallery;
    }

    /**
     * Open the gallery of models whose key contains the filter, or of every effect.
     *
     * @param source the player
     * @param filter the text a model's key must contain, or null for every model
     * @param effects true to open the effects instead
     * @param armor true to open the armor sets instead
     */
    @Command(HyperionCommands.ROOT + " gallery [filter]")
    @CommandDescription("Browse, preview and spawn every model, every effect with --effects, or every armor set with --armor")
    @Permission(HyperionPermissions.GALLERY)
    public void gallery(
            final PlayerSource source,
            @Argument("filter") @Greedy @FlagYielding final @Nullable String filter,
            @Flag("effects") final boolean effects,
            @Flag("armor") final boolean armor
    ) {
        if (effects) {
            this.gallery.openEffects(source.source());
            return;
        }

        if (armor) {
            this.gallery.openArmor(source.source());
            return;
        }

        this.gallery.openModels(source.source(), filter);
    }
}
