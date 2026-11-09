package dev.vertcode.hyperion.bedrock;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.HexFormat;

/**
 * The token the Geyser extension proves itself with, kept in its own file so it is never shared
 * by pasting {@code config.yml}.
 */
public final class BedrockToken {

    /**
     * The file name, in the data folder.
     */
    public static final String FILE = "bedrock-token.txt";

    private static final int BYTES = 32;

    private BedrockToken() {
    }

    /**
     * Read the token, making a random one when the file is missing or empty.
     *
     * @param file the token file
     * @return the token
     * @throws IOException if the file cannot be read or written
     */
    public static String load(final Path file) throws IOException {
        if (Files.exists(file)) {
            String token = Files.readString(file, StandardCharsets.UTF_8).trim();
            if (!token.isEmpty()) {
                return token;
            }
        }

        byte[] random = new byte[BYTES];
        new SecureRandom().nextBytes(random);
        String token = HexFormat.of().formatHex(random);
        Files.createDirectories(file.toAbsolutePath().getParent());
        Files.writeString(file, token + System.lineSeparator(), StandardCharsets.UTF_8);

        return token;
    }
}
