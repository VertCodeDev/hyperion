package dev.vertcode.hyperion.api.version;

import org.bukkit.Bukkit;

/**
 * Holds the version the server runs, read the first time it is asked for.
 *
 * <p>A class of its own so the server is only asked when the version is needed, never when
 * {@link MinecraftVersion} merely loads, which keeps that record usable without a server.
 */
final class RunningVersion {

    static final MinecraftVersion VERSION = MinecraftVersion.parse(Bukkit.getMinecraftVersion());

    private RunningVersion() {
    }
}
