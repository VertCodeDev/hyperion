package dev.vertcode.hyperion.pack;

/**
 * How the online players are doing with the pack.
 *
 * @param loaded players who have it loaded
 * @param pending players downloading or applying it
 * @param declined players who said no
 * @param failed players whose download or load failed
 */
public record PackStatus(int loaded, int pending, int declined, int failed) {
}
