package io.profidev.PluginAutoReload;

import com.hypixel.hytale.common.plugin.PluginIdentifier;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.Options;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.plugin.PluginManager;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class PluginAutoReload extends JavaPlugin {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private final @Nonnull List<PluginWatcher> watchers;
    private final @Nonnull Debouncer debouncer;

    public PluginAutoReload(@Nonnull JavaPluginInit init) {
        super(init);
        watchers = new ArrayList<>();
        debouncer = new Debouncer();
    }

    @Override
    protected void setup() {
        LOGGER.atInfo().log("Setting up directory watchers for plugin reloading.");
        var thisPluginId = new PluginIdentifier(this.getManifest());

        LOGGER.atInfo().log("Watching core mods directory: " + PluginManager.MODS_PATH);
        var path = PluginManager.MODS_PATH.toAbsolutePath();
        watchers.add(new PluginWatcher(path, thisPluginId, debouncer));

        for (var modsPath : Options.getOptionSet().valuesOf(Options.MODS_DIRECTORIES)) {
            LOGGER.atInfo().log("Watching mods directory: " + modsPath);
            var modsDirPath = modsPath.toAbsolutePath();
            watchers.add(new PluginWatcher(modsDirPath, thisPluginId, debouncer));
        }
    }

    @Override
    protected void start() {
        LOGGER.atInfo().log("Starting directory watchers for plugin reloading.");
        for (var watcher : watchers) {
            watcher.start();
        }
    }

    @Override
    protected void shutdown() {
        LOGGER.atInfo().log("Stopping directory watchers for plugin reloading.");
        for (var watcher : watchers) {
            watcher.interrupt();
            try {
                watcher.join();
            } catch (InterruptedException e) {
                LOGGER.atWarning().withCause(e).log("Interrupted while waiting for watcher to stop.");
            }
        }

        debouncer.shutdown();
        LOGGER.atInfo().log("Finished stopping directory watchers.");
    }
}
