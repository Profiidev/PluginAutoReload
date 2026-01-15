package io.profidev.PluginAutoReload;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.WorldConfig;

import javax.annotation.Nonnull;

public class PluginAutoReload extends JavaPlugin {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public PluginAutoReload(@Nonnull JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        LOGGER.atInfo().log("Setting up plugin " + this.getName());
        this.getCommandRegistry()
                .registerCommand(new ExampleCommand(this.getName(), this.getManifest().getVersion().toString()));

        var universe = Universe.get();
        var worlds = universe.getWorlds();
        var testWorld = "test_world";

        for (var world : worlds.keySet()) {
            LOGGER.atInfo().log("Existing world: " + world);
        }

        if (!worlds.containsKey(testWorld)) {
            var config = new WorldConfig();
            // universe.makeWorld(testWorld, testWorld, config);
        }
    }
}
