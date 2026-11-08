package dev.vertcode.hyperion;

import org.bukkit.plugin.java.JavaPlugin;

import dev.vertcode.hyperion.command.HyperionCommands;

public final class HyperionPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        // Commands
        HyperionCommands.register(this);
    }
}
