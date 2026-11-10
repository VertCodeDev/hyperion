package dev.vertcode.hyperion.pack;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import net.kyori.adventure.key.Key;

import dev.vertcode.hyperion.armor.ArmorPackInfo;

/**
 * A built resource pack: the zip, its hash, and which parts every bone is drawn with.
 *
 * <p>Immutable, so any thread may read it. The zip is copied out, never handed over.
 */
public final class ResourcePack {

    private static final long KIBIBYTE = 1024L;
    private static final String SIZE = "%.1f KiB";

    private final byte[] zip;
    private final String sha1;
    private final int files;
    private final long millis;
    private final Map<Key, Map<String, List<ModelPart>>> parts;
    private final ArmorPackInfo armor;

    /**
     * Create a built pack.
     *
     * @param zip the zip's bytes, kept as they are
     * @param sha1 the zip's SHA-1, in lowercase hex
     * @param files how many files the zip holds
     * @param millis how long the build took
     * @param parts the parts of every bone with cubes, by blueprint and bone name
     * @param armor the numbers armor items need
     */
    ResourcePack(
            final byte[] zip,
            final String sha1,
            final int files,
            final long millis,
            final Map<Key, Map<String, List<ModelPart>>> parts,
            final ArmorPackInfo armor
    ) {
        this.zip = zip;
        this.sha1 = sha1;
        this.files = files;
        this.millis = millis;
        this.parts = Map.copyOf(parts);
        this.armor = armor;
    }

    /**
     * Return the numbers this pack gave armor items.
     *
     * @return the armor info
     */
    public ArmorPackInfo armor() {
        return this.armor;
    }

    /**
     * Return the zip.
     *
     * @return a copy of the zip's bytes
     */
    public byte[] zip() {
        return this.zip.clone();
    }

    /**
     * Write the zip to a stream without copying it first, for the built-in web server.
     *
     * @param stream where to write
     * @throws IOException if the stream fails
     */
    void writeZip(final OutputStream stream) throws IOException {
        stream.write(this.zip);
    }

    /**
     * Return the zip's size.
     *
     * @return the size in bytes
     */
    public int size() {
        return this.zip.length;
    }

    /**
     * Return the zip's size for people, such as {@code 412.5 KiB}.
     *
     * @return the size
     */
    public String describeSize() {
        return String.format(Locale.ROOT, SIZE, this.zip.length / (double) KIBIBYTE);
    }

    /**
     * Return the zip's SHA-1, which clients use to skip a pack they already have.
     *
     * @return the hash in lowercase hex
     */
    public String sha1() {
        return this.sha1;
    }

    /**
     * Return how many files the zip holds.
     *
     * @return the file count
     */
    public int files() {
        return this.files;
    }

    /**
     * Return how long the build took.
     *
     * @return the time in milliseconds
     */
    public long millis() {
        return this.millis;
    }

    /**
     * Return the parts one bone is drawn with.
     *
     * @param blueprint the blueprint's key
     * @param bone the bone's name
     * @return the parts, empty for a bone without cubes or a blueprint not in this pack
     */
    public List<ModelPart> parts(final Key blueprint, final String bone) {
        return this.parts.getOrDefault(blueprint, Map.of()).getOrDefault(bone, List.of());
    }
}
