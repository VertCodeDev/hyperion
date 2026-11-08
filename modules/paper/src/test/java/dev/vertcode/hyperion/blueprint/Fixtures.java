package dev.vertcode.hyperion.blueprint;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;

import com.google.gson.JsonObject;
import net.kyori.adventure.key.Key;

import dev.vertcode.hyperion.api.model.source.FileSource;
import dev.vertcode.hyperion.blueprint.bbmodel.BbmodelFormat;
import dev.vertcode.hyperion.blueprint.bbmodel.BbmodelReader;
import dev.vertcode.hyperion.blueprint.data.ModelData;

/**
 * The hand-written model files under {@code src/test/resources/models}.
 */
public final class Fixtures {

    private Fixtures() {
    }

    /**
     * Return the path of a fixture.
     *
     * @param name the file name without the extension
     * @return the path
     */
    public static Path path(final String name) {
        URL url = Fixtures.class.getResource("/models/" + name + ".bbmodel");
        if (url == null) {
            throw new IllegalArgumentException("No fixture named " + name);
        }

        try {
            return Path.of(url.toURI());
        } catch (URISyntaxException exception) {
            throw new IllegalStateException(exception);
        }
    }

    /**
     * Read a fixture into model data.
     *
     * @param name the file name without the extension
     * @param report where problems are written
     * @return the model data
     * @throws IOException if the file cannot be read
     */
    public static ModelData read(final String name, final LoadReport report) throws IOException {
        JsonObject json = new BbmodelFormat().read(new FileSource(path(name)));

        return BbmodelReader.read(json, report);
    }

    /**
     * Read a fixture and build its blueprint.
     *
     * @param name the file name without the extension
     * @param report where problems are written
     * @return the blueprint
     * @throws IOException if the file cannot be read
     */
    public static Blueprint blueprint(final String name, final LoadReport report) throws IOException {
        ModelData data = read(name, report);

        return BlueprintFactory.build(
                Key.key("hyperion", name),
                null,
                1,
                new FileSource(path(name)),
                data,
                report,
                ModelSpawner.UNAVAILABLE
        );
    }
}
