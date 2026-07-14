package me.vasir.forgeplus.backup;

import me.vasir.jdaforge.api.Config;
import me.vasir.jdaforge.api.Log;

import java.io.File;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class BackupUploader {

    private final long channelId;
    private final int maxSizeMb;
    private final boolean deleteAfter;
    private final int intervalSeconds;
    private final String message;

    private final File backupsDir = new File("backups");
    private final Set<String> seen = ConcurrentHashMap.newKeySet();
    private ScheduledExecutorService scheduler;

    public BackupUploader(Config cfg) {
        this.channelId = parseLong(cfg.getString("channel", "0"));
        this.maxSizeMb = cfg.getInt("max-size-mb", 0);
        this.deleteAfter = cfg.getBoolean("delete-after-upload", false);
        this.intervalSeconds = Math.max(5, cfg.getInt("watch-interval-seconds", 30));
        this.message = cfg.getString("message", "New database backup");
    }

    public void start() {
        if (channelId <= 0) {
            Log.warn("[plus] backup-to-discord is enabled but channel-id is not set; watcher not started.");
            return;
        }
        seedExisting();

        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "ForgePlus-Backup-Watcher");
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleAtFixedRate(this::scan, intervalSeconds, intervalSeconds, TimeUnit.SECONDS);
        Log.done("[plus] backup-to-Discord watcher started (every " + intervalSeconds + "s).");
    }

    public void stop() {
        if (scheduler != null) scheduler.shutdownNow();
    }

    private void seedExisting() {
        File[] files = listArchives();
        if (files != null) {
            for (File f : files) seen.add(f.getName());
        }
    }

    private void scan() {
        try {
            File[] files = listArchives();
            if (files == null) return;
            for (File f : files) {
                if (seen.contains(f.getName())) continue;
                if (!isStable(f)) continue;
                seen.add(f.getName());
                BackupSender.send(f, channelId, maxSizeMb, message, deleteAfter);
            }
        } catch (Exception e) {
            Log.error("[plus] Backup watcher error: " + e.getMessage());
        }
    }

    private File[] listArchives() {
        return backupsDir.listFiles((d, n) -> n.toLowerCase().endsWith(".zip"));
    }

    private boolean isStable(File f) {
        return f.length() > 0 && (System.currentTimeMillis() - f.lastModified()) > 2000L;
    }

    private static long parseLong(String s) {
        try {
            return Long.parseLong(s.trim());
        } catch (Exception e) {
            return 0L;
        }
    }
}
