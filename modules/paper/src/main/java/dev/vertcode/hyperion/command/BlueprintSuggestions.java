package dev.vertcode.hyperion.command;

import java.util.ArrayList;
import java.util.List;

import org.incendo.cloud.annotations.suggestion.Suggestions;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.paper.util.sender.Source;

import dev.vertcode.hyperion.blueprint.Blueprint;
import dev.vertcode.hyperion.blueprint.BlueprintRegistry;

/**
 * Complete blueprint keys, for every command that takes one.
 */
public final class BlueprintSuggestions {

    /**
     * The name commands use in {@code @Argument(suggestions = ...)}.
     */
    public static final String NAME = "blueprints";

    private final BlueprintRegistry registry;

    /**
     * Create the suggestions.
     *
     * @param registry where the keys come from
     */
    public BlueprintSuggestions(final BlueprintRegistry registry) {
        this.registry = registry;
    }

    /**
     * Suggest every loaded blueprint's key.
     *
     * @param context the command being typed
     * @param input what has been typed for this argument
     * @return the keys, sorted
     */
    @Suggestions(NAME)
    public List<String> suggest(final CommandContext<Source> context, final String input) {
        List<String> keys = new ArrayList<>();
        for (Blueprint blueprint : this.registry.all()) {
            keys.add(blueprint.key().asString());
        }

        return keys;
    }
}
