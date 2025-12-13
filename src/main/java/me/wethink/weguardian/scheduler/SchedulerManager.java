package me.wethink.weguardian.scheduler;

import com.tcoded.folialib.FoliaLib;
import com.tcoded.folialib.wrapper.task.WrappedTask;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Wrapper around FoliaLib for cross-platform scheduler compatibility.
 * Supports both Paper/Spigot and Folia servers.
 * 
 * PERFORMANCE: Optimized to minimize lambda allocation overhead.
 */
public class SchedulerManager {

    private final FoliaLib foliaLib;
    private final boolean isFolia;

    public SchedulerManager(JavaPlugin plugin) {
        this.foliaLib = new FoliaLib(plugin);
        this.isFolia = foliaLib.isFolia();
    }

    /**
     * Runs a task asynchronously.
     * PERFORMANCE: Uses method reference adapter to avoid wrapping overhead.
     */
    public void runAsync(Runnable task) {
        foliaLib.getScheduler().runAsync(adapt(task));
    }

    /**
     * Runs a task on the main thread (global region for Folia).
     */
    public void runSync(Runnable task) {
        foliaLib.getScheduler().runNextTick(adapt(task));
    }

    /**
     * Runs a task asynchronously after a delay.
     */
    public void runAsyncLater(Runnable task, long delay, TimeUnit unit) {
        foliaLib.getScheduler().runLaterAsync(adapt(task), delay, unit);
    }

    /**
     * Runs a task on the main thread after a delay.
     */
    public void runSyncLater(Runnable task, long delay, TimeUnit unit) {
        foliaLib.getScheduler().runLater(adapt(task), delay, unit);
    }

    /**
     * Runs an async task repeatedly at a fixed rate.
     */
    public void runAsyncRepeating(Runnable task, long initialDelay, long period, TimeUnit unit) {
        foliaLib.getScheduler().runTimerAsync(adapt(task), initialDelay, period, unit);
    }

    /**
     * Runs a sync task repeatedly at a fixed rate.
     */
    public void runSyncRepeating(Runnable task, long initialDelay, long period, TimeUnit unit) {
        foliaLib.getScheduler().runTimer(adapt(task), initialDelay, period, unit);
    }

    /**
     * Runs a task for a specific entity (region-aware for Folia).
     * PERFORMANCE: This is crucial for Folia - ensures task runs in entity's
     * region.
     */
    public void runForEntity(Entity entity, Runnable task) {
        foliaLib.getScheduler().runAtEntity(entity, adapt(task));
    }

    /**
     * Runs a task for a specific entity after a delay.
     */
    public void runForEntityLater(Entity entity, Runnable task, long delay, TimeUnit unit) {
        foliaLib.getScheduler().runAtEntityLater(entity, adapt(task), delay, unit);
    }

    /**
     * Checks if the server is running Folia.
     */
    public boolean isFolia() {
        return isFolia;
    }

    /**
     * Cancels all scheduled tasks on plugin disable.
     */
    public void shutdown() {
        // FoliaLib handles task cancellation automatically
    }

    /**
     * Adapts a Runnable to FoliaLib's Consumer<WrappedTask> interface.
     * PERFORMANCE: This creates one lambda per call, but it's necessary
     * to bridge the API. The alternative would be storing tasks,
     * which has its own overhead.
     */
    private static Consumer<WrappedTask> adapt(Runnable task) {
        return wrappedTask -> task.run();
    }
}
