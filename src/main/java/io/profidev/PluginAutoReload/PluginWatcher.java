package io.profidev.PluginAutoReload;

import com.hypixel.hytale.codec.ExtraInfo;
import com.hypixel.hytale.codec.util.RawJsonReader;
import com.hypixel.hytale.common.plugin.PluginIdentifier;
import com.hypixel.hytale.common.plugin.PluginManifest;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.PluginManager;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.util.UUID;
import java.util.jar.JarFile;

public class PluginWatcher extends Thread {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    @SuppressWarnings("rawtypes")
    private static final WatchEvent.Kind[] EVENTS = new WatchEvent.Kind[]{
            StandardWatchEventKinds.ENTRY_CREATE,
            StandardWatchEventKinds.ENTRY_MODIFY,
    };

    private final @Nonnull Path path;
    private final @Nonnull PluginIdentifier thisPluginId;
    private final @Nonnull Debouncer debouncer;
    private final @Nonnull UUID id;

    public PluginWatcher(@Nonnull Path path, @Nonnull PluginIdentifier thisPluginId, @Nonnull Debouncer debouncer) {
        this.path = path;
        this.thisPluginId = thisPluginId;
        this.debouncer = debouncer;
        this.id = UUID.randomUUID();
    }

    @Override
    public void run() {
        try {
            var fs = FileSystems.getDefault();
            var ws = fs.newWatchService();
            path.register(ws, EVENTS);
            LOGGER.atInfo().log("Watching directory " + path + " for changes.");

            while (true) {
                var key = ws.take();
                for (var event : key.pollEvents()) {
                    var kind = event.kind();
                    var changed = path.resolve((Path) event.context());

                    // Wait for writes to settle before reloading
                    debouncer.debounce(id, () -> {
                        LOGGER.atInfo().log("Detected " + kind.name() + " on " + changed + ".");
                        var pluginId = getPluginIdFromJar(changed);
                        if (pluginId == null || pluginId.getGroup().equals("Hytale") || pluginId == thisPluginId) {
                            return;
                        }


                        LOGGER.atInfo().log("Reloading plugin: " + pluginId);
                        PluginManager.get().reload(pluginId);
                    }, 1000);
                }
                key.reset();
            }
        } catch (IOException e) {
            LOGGER.atWarning().withCause(e).log("Failed to watch directory " + path);
        } catch (InterruptedException e) {
            LOGGER.atInfo().log("Plugin watcher for directory " + path + " interrupted and stopping.");
            Thread.currentThread().interrupt();
        }
    }

    private PluginIdentifier getPluginIdFromJar(Path jarPath) {
        try (var jarFile = new JarFile(jarPath.toFile())) {
            var entry = jarFile.getJarEntry("manifest.json");
            if (entry == null) {
                LOGGER.atWarning().log("No manifest.json found in jar: " + jarPath);
                return null;
            }

            try (var stream = jarFile.getInputStream(entry);
                 var reader = new InputStreamReader(stream, StandardCharsets.UTF_8);
            ) {
                var buffer = RawJsonReader.READ_BUFFER.get();
                var rawJsonreader = new RawJsonReader(reader, buffer);
                var extraInfo = ExtraInfo.THREAD_LOCAL.get();
                var manifest = PluginManifest.CODEC.decodeJson(rawJsonreader, extraInfo);
                extraInfo.getValidationResults().logOrThrowValidatorExceptions(LOGGER);

                if (manifest != null) {
                    return new PluginIdentifier(manifest);
                }
            }
        } catch (IOException e) {
            LOGGER.atWarning().withCause(e).log("Failed to open jar file: " + jarPath);
        }

        LOGGER.atWarning().log("Could not retrieve plugin ID from jar: " + jarPath);
        return null;
    }
}
