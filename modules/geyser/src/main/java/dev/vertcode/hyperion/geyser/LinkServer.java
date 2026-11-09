package dev.vertcode.hyperion.geyser;

/**
 * One Hyperion server the extension links to.
 *
 * @param url the address of its Bedrock link, without a trailing slash
 * @param token the token it checks every request against
 */
record LinkServer(String url, String token) {
}
