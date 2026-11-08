package dev.vertcode.hyperion.blueprint.bbmodel;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import net.kyori.adventure.key.Key;

import dev.vertcode.hyperion.api.model.source.ModelFormat;
import dev.vertcode.hyperion.api.model.source.ModelSource;

/**
 * The built-in format: Blockbench's own {@code .bbmodel} files.
 */
public final class BbmodelFormat implements ModelFormat {

    private static final Key KEY = Key.key("hyperion", "bbmodel");
    private static final String EXTENSION = ".bbmodel";
    private static final String NOT_AN_OBJECT = "%s is not a Blockbench project: the file is not a JSON object";
    private static final String BROKEN_JSON = "%s is not valid JSON: %s";

    @Override
    public Key key() {
        return KEY;
    }

    @Override
    public boolean supports(final ModelSource source) {
        return source.name().toLowerCase(Locale.ROOT).endsWith(EXTENSION);
    }

    @Override
    public JsonObject read(final ModelSource source) throws IOException {
        try (InputStream stream = source.open();
             Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            JsonElement root = JsonParser.parseReader(reader);
            if (!(root instanceof JsonObject object)) {
                throw new IOException(String.format(NOT_AN_OBJECT, source.name()));
            }

            return object;
        } catch (JsonParseException exception) {
            throw new IOException(String.format(BROKEN_JSON, source.name(), exception.getMessage()), exception);
        }
    }
}
