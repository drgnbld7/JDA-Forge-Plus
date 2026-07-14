package me.vasir.forgeplus.backup;

import me.vasir.jdaforge.api.Config;
import me.vasir.jdaforge.api.Log;

import java.io.File;
import java.io.FilenameFilter;
import java.time.LocalDate;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class LogRetentionTask {

    private final long maxAgeDays;
    private final int maxFiles;
    private final boolean includeCrashDumps;
    private final int intervalMinutes;
    private final File logsDir = new File("logs");
    private ScheduledExecutorService scheduler;

    public LogRetentionTask(Config cfg) {
        this.maxAgeDays = cfg.getLong("max-age-days", 14);
        this.maxFiles = cfg.getInt("max-files", 0);
        this.includeCrashDumps = cfg.getBoolean("include-crash-dumps", true);
        this.intervalMinutes = Math.max(1, cfg.getInt("run-interval-minutes", 360));
    }

    public void start() {
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "ForgePlus-Log-Retention");
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleAtFixedRate(this::prune, 1, intervalMinutes, TimeUnit.MINUTES);
        Log.done("[plus] log retention started (every " + intervalMinutes + "m).");
    }

    public void stop() {
        if (scheduler != null) scheduler.shutdownNow();
    }

    private void prune() {
        try {
            FilenameFilter filter = (d, n) -> {
                String low = n.toLowerCase();
                return low.endsWith(".log") || (includeCrashDumps && low.endsWith(".txt"));
            };
            String activeLog = LocalDate.now() + ".log";
            int removed = FolderCleaner.prune(logsDir, filter, maxAgeDays, maxFiles, activeLog);
            if (removed > 0) Log.info("[plus] Log retention removed " + removed + " old log file(s).");
        } catch (Exception e) {
            Log.error("[plus] Log retention error: " + e.getMessage());
        }
    }
}
