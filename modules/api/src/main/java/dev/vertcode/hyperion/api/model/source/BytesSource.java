package dev.vertcode.hyperion.api.model.source;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Arrays;

import org.jspecify.annotations.Nullable;

/**
 * A model read from bytes in memory. The bytes are copied in and never handed out.
 *
 * @param name a file name, with its extension
 * @param data the bytes
 */
public record BytesSource(String name, byte[] data) implements ModelSource {

    private static final String DESCRIPTION = "BytesSource[name=%s, size=%d]";

    /**
     * Create a source over a copy of the bytes.
     *
     * @param name a file name, with its extension
     * @param data the bytes
     */
    public BytesSource {
        data = data.clone();
    }

    @Override
    public InputStream open() {
        return new ByteArrayInputStream(this.data);
    }

    /**
     * Return a copy of the bytes.
     *
     * @return the bytes
     */
    @Override
    public byte[] data() {
        return this.data.clone();
    }

    @Override
    public boolean equals(final @Nullable Object other) {
        return other instanceof BytesSource source && this.name.equals(source.name) && Arrays.equals(this.data, source.data);
    }

    @Override
    public int hashCode() {
        return 31 * this.name.hashCode() + Arrays.hashCode(this.data);
    }

    @Override
    public String toString() {
        return String.format(DESCRIPTION, this.name, this.data.length);
    }
}
