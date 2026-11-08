package dev.vertcode.hyperion.api.model.source;

import java.io.IOException;

import com.google.gson.JsonObject;
import net.kyori.adventure.key.Keyed;
import org.jetbrains.annotations.ApiStatus;

/**
 * Turns a file format into a Blockbench project, so Hyperion can load it.
 *
 * <p>Hyperion reads one shape, the Blockbench project. {@code .bbmodel} is built in and passes
 * straight through. A format for anything else converts to that shape, and keeps its own extras
 * as custom fields, which end up in the model's metadata.
 */
@ApiStatus.Experimental
public interface ModelFormat extends Keyed {

    /**
     * Decide whether this format reads a source, usually by its extension.
     *
     * @param source the source
     * @return true when this format reads it
     */
    boolean supports(ModelSource source);

    /**
     * Read a source into a Blockbench project. Called off the server thread.
     *
     * @param source the source
     * @return the project, as the JSON a {@code .bbmodel} file holds
     * @throws IOException if the source cannot be read or is not valid for this format
     */
    JsonObject read(ModelSource source) throws IOException;
}
