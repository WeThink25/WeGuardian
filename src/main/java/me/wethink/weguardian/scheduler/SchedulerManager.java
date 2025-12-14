package me.wethink.weguardian.scheduler;

import com.tcoded.folialib.FoliaLib;
import com.tcoded.folialib.wrapper.task.WrappedTask;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;


public class SchedulerManager {

    private final FoliaLib foliaLib;
    private final boolean isFolia;

    public SchedulerManager(JavaPlugin plugin) {
        this.foliaLib = new FoliaLib(plugin);
        this.isFolia = foliaLib.isFolia();
    }


    public void runAsync(Runnable task) {
        foliaLib.getScheduler().runAsync(adapt(task));
    }


    public void runSync(Runnable task) {
        foliaLib.getScheduler().runNextTick(adapt(task));
    }


    public void runAsyncLater(Runnable task, long delay, TimeUnit unit) {
        foliaLib.getScheduler().runLaterAsync(adapt(task), delay, unit);
    }


    public void runSyncLater(Runnable task, long delay, TimeUnit unit) {
        foliaLib.getScheduler().runLater(adapt(task), delay, unit);
    }


    public void runAsyncRepeating(Runnable task, long initialDelay, long period, TimeUnit unit) {
        foliaLib.getScheduler().runTimerAsync(adapt(task), initialDelay, period, unit);
    }


    public void runSyncRepeating(Runnable task, long initialDelay, long period, TimeUnit unit) {
        foliaLib.getScheduler().runTimer(adapt(task), initialDelay, period, unit);
    }


    public void runForEntity(Entity entity, Runnable task) {
        foliaLib.getScheduler().runAtEntity(entity, adapt(task));
    }


    public void runForEntityLater(Entity entity, Runnable task, long delay, TimeUnit unit) {
        foliaLib.getScheduler().runAtEntityLater(entity, adapt(task), delay, unit);
    }


    public boolean isFolia() {
        return isFolia;
    }


    public void shutdown() {
    }

    private static Consumer<WrappedTask> adapt(Runnable task) {
        return wrappedTask -> task.run();
    }
}
