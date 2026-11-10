package dev.vertcode.hyperion.pack.armor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.jspecify.annotations.Nullable;

import dev.vertcode.hyperion.pack.ModelOverride;

/**
 * Write the model file of a vanilla armor item for 1.21 to 1.21.3, with Hyperion's custom model
 * data added after vanilla's trim overrides.
 *
 * <p>Before 1.21.4 a custom icon hangs off the item's own model file, which also holds the trim
 * looks. Writing the file again with every trim kept means trimmed vanilla armor still shows its
 * trim.
 */
final class TrimOverrides {

    private static final String[] TRIMS = {
        "quartz", "iron", "netherite", "redstone", "copper", "gold", "emerald", "diamond", "lapis", "amethyst"
    };
    private static final float TRIM_STEP = 0.1f;
    private static final String LEATHER = "leather_";

    private TrimOverrides() {
    }

    /**
     * Write an armor item's model.
     *
     * @param item the item's name, such as {@code netherite_chestplate}
     * @param darker the trim material that has a darker look on this armor, or null
     * @param custom the custom model data overrides for it
     * @return the model
     */
    static JsonObject model(final String item, final @Nullable String darker, final List<ModelOverride> custom) {
        JsonObject textures = new JsonObject();
        textures.addProperty("layer0", "minecraft:item/" + item);
        if (item.startsWith(LEATHER)) {
            textures.addProperty("layer1", "minecraft:item/" + item + "_overlay");
        }

        JsonArray overrides = new JsonArray();
        for (int index = 0; index < TRIMS.length; index++) {
            String trim = TRIMS[index];
            String look = trim.equals(darker) ? trim + "_darker" : trim;
            JsonObject predicate = new JsonObject();
            predicate.addProperty("trim_type", Math.round((index + 1) * TRIM_STEP * 10f) / 10f);
            JsonObject entry = new JsonObject();
            entry.addProperty("model", "minecraft:item/" + item + "_" + look + "_trim");
            entry.add("predicate", predicate);
            overrides.add(entry);
        }

        List<ModelOverride> sorted = new ArrayList<>(custom);
        sorted.sort(Comparator.comparingInt(ModelOverride::customModelData));
        for (ModelOverride override : sorted) {
            JsonObject predicate = new JsonObject();
            predicate.addProperty("custom_model_data", override.customModelData());
            JsonObject entry = new JsonObject();
            entry.add("predicate", predicate);
            entry.addProperty("model", override.model());
            overrides.add(entry);
        }

        JsonObject model = new JsonObject();
        model.addProperty("parent", "minecraft:item/generated");
        model.add("textures", textures);
        model.add("overrides", overrides);

        return model;
    }
}
