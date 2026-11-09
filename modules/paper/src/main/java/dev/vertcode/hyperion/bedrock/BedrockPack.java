package dev.vertcode.hyperion.bedrock;

/**
 * A built Bedrock pack and the list of models the Geyser extension registers.
 *
 * @param zip the {@code .mcpack}
 * @param sha1 the pack's SHA-1 in hex
 * @param manifest the model list, as JSON
 */
public record BedrockPack(byte[] zip, String sha1, String manifest) {
}
