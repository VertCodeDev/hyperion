package dev.vertcode.hyperion.blueprint;

import java.util.List;
import java.util.Map;

import net.kyori.adventure.key.Key;

/**
 * What a full reload did.
 *
 * @param loaded the blueprints that loaded, in load order
 * @param failed the reports of the files that could not load
 * @param changes what changed in each model that was loaded before, by key
 * @param removed the keys whose files are gone, so their blueprints were dropped
 * @param millis how long the reload took
 */
public record ReloadResult(
        List<Blueprint> loaded,
        List<LoadReport> failed,
        Map<Key, BlueprintChanges> changes,
        List<Key> removed,
        long millis
) {

    /**
     * Copy every collection, so the result never changes.
     */
    public ReloadResult {
        loaded = List.copyOf(loaded);
        failed = List.copyOf(failed);
        changes = Map.copyOf(changes);
        removed = List.copyOf(removed);
    }

    /**
     * Return how many warnings the loaded blueprints have together.
     *
     * @return the warning count
     */
    public int warnings() {
        int warnings = 0;
        for (Blueprint blueprint : this.loaded) {
            warnings += blueprint.report().count(IssueSeverity.WARNING);
        }

        return warnings;
    }
}
