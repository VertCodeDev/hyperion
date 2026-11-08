package dev.vertcode.hyperion.api.model.source;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * A model read from a file.
 *
 * @param path the file
 */
public record FileSource(Path path) implements ModelSource {

    @Override
    public String name() {
        return String.valueOf(this.path.getFileName());
    }

    @Override
    public InputStream open() throws IOException {
        return Files.newInputStream(this.path);
    }
}
