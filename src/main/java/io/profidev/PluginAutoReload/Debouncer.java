package io.profidev.PluginAutoReload;

import java.util.concurrent.*;

public class Debouncer {
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private final ConcurrentHashMap<Object, ScheduledFuture<?>> delayedTasks = new ConcurrentHashMap<>();

    public void debounce(Object key, Runnable runnable, long delay) {
        ScheduledFuture<?> prev = delayedTasks.put(key, executor.schedule(() -> {
            try {
                runnable.run();
            } finally {
                delayedTasks.remove(key);
            }
        }, delay, TimeUnit.MILLISECONDS));

        if (prev != null) {
            prev.cancel(false);
        }
    }

    public void shutdown() {
        executor.shutdownNow();
    }
}
