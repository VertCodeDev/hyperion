package dev.vertcode.hyperion.command;

import java.util.List;
import java.util.Locale;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.BoundingBox;
import org.incendo.cloud.annotations.Argument;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.CommandDescription;
import org.incendo.cloud.annotations.Permission;
import org.incendo.cloud.bukkit.annotation.specifier.DefaultNamespace;
import org.incendo.cloud.paper.util.sender.Source;

import dev.vertcode.hyperion.api.model.behavior.BehaviorMatcher;
import dev.vertcode.hyperion.blueprint.Blueprint;
import dev.vertcode.hyperion.blueprint.BlueprintBone;
import dev.vertcode.hyperion.blueprint.BlueprintKeys;
import dev.vertcode.hyperion.blueprint.BlueprintLocator;
import dev.vertcode.hyperion.blueprint.BlueprintRegistry;
import dev.vertcode.hyperion.blueprint.IssueSeverity;
import dev.vertcode.hyperion.blueprint.data.Animation;

/**
 * {@code /hyperion info <model>}: everything about one loaded model.
 */
public final class InfoCommand {

    private static final int MAX_BONES = 40;
    private static final String SIZE = "%.2f x %.2f x %.2f blocks";
    private static final String LENGTH = "%.2fs";

    private final BlueprintRegistry registry;

    /**
     * Create the command.
     *
     * @param registry where the blueprints come from
     */
    public InfoCommand(final BlueprintRegistry registry) {
        this.registry = registry;
    }

    /**
     * Show a model's source, size, bone tree, animations, locators and warnings.
     *
     * @param source who ran the command
     * @param key the model's key; {@code hyperion:} may be left out
     */
    @Command(HyperionCommands.ROOT + " info <model>")
    @CommandDescription("Show a model's bones, animations, locators and warnings")
    @Permission(HyperionPermissions.INFO)
    public void info(
            final Source source,
            @Argument(value = "model", suggestions = BlueprintSuggestions.NAME)
            @DefaultNamespace(BlueprintKeys.NAMESPACE) final NamespacedKey key
    ) {
        CommandSender sender = source.source();
        Blueprint blueprint = this.registry.find(Key.key(key.namespace(), key.value())).orElse(null);
        if (blueprint == null) {
            sender.sendMessage(CommandText.notLoaded(key));
            return;
        }

        TextComponent.Builder message = Component.text()
                .append(CommandText.heading(blueprint.key().asString()))
                .append(Component.text(" (revision " + blueprint.revision() + ")", NamedTextColor.DARK_GRAY));
        Plugin owner = blueprint.owner();
        BoundingBox box = blueprint.boundingBox();
        message.appendNewline().append(CommandText.row("Source", blueprint.source().name()
                + (owner == null ? "" : " from " + owner.getName())));
        message.appendNewline().append(CommandText.row("Format", blueprint.data().format()));
        message.appendNewline().append(CommandText.row(
                "Size",
                String.format(Locale.ROOT, SIZE, box.getWidthX(), box.getHeight(), box.getWidthZ())
        ));
        message.appendNewline().append(CommandText.row("Textures", String.valueOf(blueprint.textures().size())));

        this.bones(message, blueprint.bones());
        this.animations(message, blueprint);
        this.locators(message, blueprint.locators());
        this.warnings(message, blueprint);
        sender.sendMessage(message.build());
    }

    /**
     * Add the bone tree, indented by depth, with each bone's behavior tags.
     *
     * @param message the message being built
     * @param bones the bones, parents first
     */
    private void bones(final TextComponent.Builder message, final List<BlueprintBone> bones) {
        message.appendNewline().append(CommandText.heading("Bones (" + bones.size() + ")"));
        int shown = 0;
        for (BlueprintBone bone : bones) {
            if (shown == MAX_BONES) {
                String more = " ...and " + (bones.size() - shown) + " more";
                message.appendNewline().append(Component.text(more, NamedTextColor.DARK_GRAY));
                return;
            }

            String indent = "  ".repeat(bone.depth() + 1);
            NamedTextColor color = bone.hasGeometry() ? NamedTextColor.WHITE : NamedTextColor.GRAY;
            Component line = Component.text(indent + bone.name(), color);
            List<String> tags = bone.metadata().strings(BehaviorMatcher.TAG_FIELD);
            if (!tags.isEmpty()) {
                line = line.append(Component.text(" [" + String.join(", ", tags) + "]", HyperionCommands.HIGHLIGHT));
            }

            if (!bone.visibleByDefault()) {
                line = line.append(Component.text(" hidden", NamedTextColor.DARK_GRAY));
            }

            message.appendNewline().append(line);
            shown++;
        }
    }

    /**
     * Add every animation with its length and loop mode.
     *
     * @param message the message being built
     * @param blueprint the blueprint
     */
    private void animations(final TextComponent.Builder message, final Blueprint blueprint) {
        message.appendNewline().append(CommandText.heading("Animations (" + blueprint.animations().size() + ")"));
        for (String name : blueprint.animations()) {
            Animation animation = blueprint.animation(name).orElseThrow();
            String details = "  " + String.format(Locale.ROOT, LENGTH, animation.length()) + ", "
                    + animation.loop().name().toLowerCase(Locale.ROOT);
            message.appendNewline()
                    .append(Component.text("  " + name, NamedTextColor.WHITE))
                    .append(Component.text(details, NamedTextColor.GRAY));
        }
    }

    /**
     * Add every locator with the bone it hangs from.
     *
     * @param message the message being built
     * @param locators the locators
     */
    private void locators(final TextComponent.Builder message, final List<BlueprintLocator> locators) {
        if (locators.isEmpty()) {
            return;
        }

        message.appendNewline().append(CommandText.heading("Locators (" + locators.size() + ")"));
        for (BlueprintLocator locator : locators) {
            String parent = locator.parent().map(bone -> "  on " + bone.name()).orElse("");
            message.appendNewline()
                    .append(Component.text("  " + locator.name(), NamedTextColor.WHITE))
                    .append(Component.text(parent, NamedTextColor.GRAY));
        }
    }

    /**
     * Add the warning count, a click away from the full list.
     *
     * @param message the message being built
     * @param blueprint the blueprint
     */
    private void warnings(final TextComponent.Builder message, final Blueprint blueprint) {
        int warnings = blueprint.report().count(IssueSeverity.WARNING);
        if (warnings == 0) {
            return;
        }

        String key = blueprint.key().asString();
        Component line = CommandText.clickable(
                Component.text(" " + warnings + " warning(s), click to see them", NamedTextColor.YELLOW),
                "/hyperion validate " + key,
                "Show the warnings"
        );
        message.appendNewline().append(line);
    }
}
