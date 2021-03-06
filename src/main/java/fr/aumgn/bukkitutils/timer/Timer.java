package fr.aumgn.bukkitutils.timer;

import java.util.concurrent.TimeUnit;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.plugin.Plugin;

import com.google.common.base.Stopwatch;

/**
 * A timer class which display time according
 * to the delays specified in the {@link TimerConfig}
 * using the abstract method {@link #sendTimeMessage(String)}.
 */
public abstract class Timer implements Runnable {

    private static final int TICKS_PER_SECONDS = 20;

    private final Plugin plugin;
    private final int majorDelay;
    private final int minorDelay;
    private final String format;
    private final String endMessage;
    private Runnable runnable;

    private int remainingTime;

    private int taskId;
    private Stopwatch watch;
    private int currentDelay;
    private int pauseDelay;

    private Timer(boolean dummy, Plugin plugin, TimerConfig config, int seconds) {
        this.plugin = plugin;
        this.majorDelay = config.getMajorDuration();
        this.minorDelay = config.getMinorDuration();
        this.format = config.getFormat();
        this.endMessage = config.getEndMessage();

        this.taskId = -1;
        this.remainingTime = seconds;

        this.currentDelay = 0;
    }

    public Timer(Plugin plugin, TimerConfig config, int seconds, Runnable runnable) {
        this(true, plugin, config, seconds);
        this.runnable = runnable;
    }

    public Timer(Plugin plugin, TimerConfig config, int seconds) {
        this(false, plugin, config, seconds);
        this.runnable = new Runnable() {
            public void run() {
                onFinish();
            }
        };
    }

    private void scheduleAndPrintTime(int delay) {
        long minutes = TimeUnit.SECONDS.toMinutes(remainingTime);
        String msg = String.format(format, minutes, remainingTime % 60);
        sendTimeMessage(getCurrentColor() + msg);
        schedule(delay);
    }

    private void schedule(int delay) {
        currentDelay = delay;
        watch = new Stopwatch();
        watch.start();
        taskId = Bukkit.getScheduler().scheduleSyncDelayedTask(
                plugin, this, (long) delay * TICKS_PER_SECONDS);
    }

    private ChatColor getCurrentColor() {
        if (remainingTime >= majorDelay) {
            return ChatColor.YELLOW;
        } else if (remainingTime >= minorDelay){
            return ChatColor.GOLD;
        } else {
            return ChatColor.RED;
        }
    }

    public int getRemainingTime() {
        return remainingTime;
    }

    public void start() {
        remainingTime -= currentDelay;
        if (remainingTime > majorDelay) {
            schedule(majorDelay);
        } else if (remainingTime > minorDelay) {
            schedule(minorDelay);
        } else if (remainingTime > 0) {
            schedule(1);
        } else {
            runnable.run();
        }
    }

    @Override
    public void run() {
        remainingTime -= currentDelay;
        if (remainingTime > majorDelay) {
            scheduleAndPrintTime(majorDelay);
        } else if (remainingTime > minorDelay) {
            scheduleAndPrintTime(minorDelay);
        } else if (remainingTime > 0) {
            scheduleAndPrintTime(1);
        } else {
            runnable.run();
        }
    }

    public void cancel() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = -1;
        }
    }

    public void pause() {
        cancel();
        watch.stop();
        pauseDelay = (int) watch.elapsedTime(TimeUnit.SECONDS);
        remainingTime -= pauseDelay;
    }

    public void resume() {
        int delay = currentDelay - pauseDelay;
        if (delay > 0) {
            scheduleAndPrintTime(delay);
        } else {
            runnable.run();
        }
    }

    public void onFinish() {
        sendTimeMessage(endMessage);
    }

    public abstract void sendTimeMessage(String string);
}
