package me.vasir.forgeplus.backup;

import me.vasir.jdaforge.api.Config;
import me.vasir.jdaforge.api.Log;

import java.io.File;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class RetentionTask {

    private final long maxAgeDays;
    private final int maxFiles;
    private final int intervalMinutes;
    private final File backupsDir = new File("backups");
    private ScheduledExecutorService scheduler;

    public RetentionTask(Config cfg) {
        this.maxAgeDays = cfg.getLong("max-age-days", 7);
        this.maxFiles = cfg.getInt("max-files", 0);
        this.intervalMinutes = Math.max(1, cfg.getInt("run-interval-minutes", 60));
    }

    public void start() {
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "ForgePlus-Backup-Retention");
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleAtFixedRate(this::prune, 1, intervalMinutes, TimeUnit.MINUTES);
        Log.done("[plus] backup retention started (every " + intervalMinutes + "m).");
    }

    public void stop() {
        if (scheduler != null) scheduler.shutdownNow();
    }

    private void prune() {
        try {
            int removed = FolderCleaner.prune(
                    backupsDir,
                    (d, n) -> n.toLowerCase().endsWith(".zip"),
                    maxAgeDays, maxFiles, null);
            if (removed > 0) Log.info("[plus] Backup retention removed " + removed + " old archive(s).");
        } catch (Exception e) {
            Log.error("[plus] Backup retention error: " + e.getMessage());
        }
    }
}
