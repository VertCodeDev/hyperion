package dev.vertcode.hyperion.command;

import java.util.List;
import java.util.Locale;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
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
import dev.vertcode.hyperion.config.HyperionMessages;

import static dev.vertcode.hyperion.config.HyperionMessages.value;

/**
 * {@code /hyperion info <model>}: everything about one loaded model.
 */
public final class InfoCommand {

    private static final int MAX_BONES = 40;
    private static final String DECIMALS = "%.2f";

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
            HyperionMessages.MODEL_NOT_LOADED.send(sender, value("model", key.asString()));
            return;
        }

        TextComponent.Builder message = Component.text().append(HyperionMessages.INFO__HEADING.component(
                value("model", blueprint.key().asString()),
                value("revision", blueprint.revision())
        ));
        this.details(message, blueprint);
        this.bones(message, blueprint.blueprintBones());
        this.animations(message, blueprint);
        this.locators(message, blueprint.blueprintLocators());
        this.warnings(message, blueprint);
        sender.sendMessage(message.build());
    }

    /**
     * Add the source, format, size and texture count.
     *
     * @param message the message being built
     * @param blueprint the blueprint
     */
    private void details(final TextComponent.Builder message, final Blueprint blueprint) {
        Plugin owner = blueprint.owner();
        Component source = owner == null
                ? HyperionMessages.INFO__SOURCE.component(value("source", blueprint.source().name()))
                : HyperionMessages.INFO__SOURCE_PLUGIN.component(
                        value("source", blueprint.source().name()),
                        value("plugin", owner.getName())
                );
        BoundingBox box = blueprint.boundingBox();

        message.appendNewline().append(source);
        Component format = HyperionMessages.INFO__FORMAT.component(value("format", blueprint.data().format()));
        Component textures = HyperionMessages.INFO__TEXTURES.component(value("count", blueprint.textures().size()));
        message.appendNewline().append(format);
        message.appendNewline().append(HyperionMessages.INFO__SIZE.component(
                value("x", decimals(box.getWidthX())),
                value("y", decimals(box.getHeight())),
                value("z", decimals(box.getWidthZ()))
        ));
        message.appendNewline().append(textures);
    }

    /**
     * Add the bone tree, indented by depth, with each bone's behavior tags.
     *
     * @param message the message being built
     * @param bones the bones, parents first
     */
    private void bones(final TextComponent.Builder message, final List<BlueprintBone> bones) {
        message.appendNewline().append(HyperionMessages.INFO__BONES.component(value("count", bones.size())));
        for (int i = 0; i < bones.size(); i++) {
            if (i == MAX_BONES) {
                int more = bones.size() - i;
                message.appendNewline().append(HyperionMessages.INFO__MORE_BONES.component(value("count", more)));
                return;
            }

            BlueprintBone bone = bones.get(i);
            List<String> tags = bone.metadata().strings(BehaviorMatcher.TAG_FIELD);
            Component tagged = tags.isEmpty()
                    ? Component.empty()
                    : HyperionMessages.INFO__BONE_TAGS.component(value("tags", String.join(", ", tags)));
            Component hidden = bone.visibleByDefault()
                    ? Component.empty()
                    : HyperionMessages.INFO__BONE_HIDDEN.component();
            HyperionMessages line = bone.hasGeometry()
                    ? HyperionMessages.INFO__BONE
                    : HyperionMessages.INFO__BONE_WITHOUT_CUBES;
            message.appendNewline().append(line.component(
                    value("indent", "  ".repeat(bone.depth() + 1)),
                    value("name", bone.name()),
                    value("tags", tagged),
                    value("hidden", hidden)
            ));
        }
    }

    /**
     * Add every animation with its length and loop mode.
     *
     * @param message the message being built
     * @param blueprint the blueprint
     */
    private void animations(final TextComponent.Builder message, final Blueprint blueprint) {
        int count = blueprint.animations().size();
        message.appendNewline().append(HyperionMessages.INFO__ANIMATIONS.component(value("count", count)));
        for (String name : blueprint.animations()) {
            Animation animation = blueprint.animationData(name).orElseThrow();
            message.appendNewline().append(HyperionMessages.INFO__ANIMATION.component(
                    value("name", name),
                    value("length", decimals(animation.length())),
                    value("loop", animation.loop().name().toLowerCase(Locale.ROOT))
            ));
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

        message.appendNewline().append(HyperionMessages.INFO__LOCATORS.component(value("count", locators.size())));
        for (BlueprintLocator locator : locators) {
            Component line = locator.parent()
                    .map(bone -> HyperionMessages.INFO__LOCATOR_ON_BONE.component(
                            value("name", locator.name()),
                            value("bone", bone.name())
                    ))
                    .orElseGet(() -> HyperionMessages.INFO__LOCATOR.component(value("name", locator.name())));
            message.appendNewline().append(line);
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

        Component line = CommandText.clickable(
                HyperionMessages.INFO__WARNINGS.component(value("count", warnings)),
                "/hyperion validate " + blueprint.key().asString(),
                HyperionMessages.INFO__WARNINGS_HOVER.component()
        );
        message.appendNewline().append(line);
    }

    /**
     * Write a number with two decimals, the same in every locale.
     *
     * @param number the number
     * @return the text, such as {@code 1.25}
     */
    private static String decimals(final double number) {
        return String.format(Locale.ROOT, DECIMALS, number);
    }
}
