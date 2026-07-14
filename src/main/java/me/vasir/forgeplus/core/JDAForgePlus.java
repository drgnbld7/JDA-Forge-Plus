package me.vasir.forgeplus.core;

import me.vasir.forgeplus.backup.BackupUploader;
import me.vasir.forgeplus.backup.LogRetentionTask;
import me.vasir.forgeplus.backup.RetentionTask;
import me.vasir.jdaforge.api.Config;
import me.vasir.jdaforge.api.ForgeModule;
import me.vasir.jdaforge.api.Log;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;

public class JDAForgePlus extends ForgeModule {

    private BackupUploader backupUploader;
    private RetentionTask retentionTask;
    private LogRetentionTask logRetentionTask;

    @Override
    public void onEnable() {
        Config cfg = config("jda-forge-plus.yml");

        if (cfg.section("placeholders").getBoolean("enabled", true)) {
            OpsPlaceholders.register(placeholders(), cfg.section("placeholders"));
        }

        PlusCommands commands = new PlusCommands(cfg);
        for (CommandData command : commands.buildCommands()) {
            registerCommand(command);
        }
        registerListener(commands);

        Config backup = cfg.section("backup-to-discord");
        if (backup.getBoolean("enabled", false)) {
            backupUploader = new BackupUploader(backup);
            backupUploader.start();
        }

        Config retention = cfg.section("backup-retention");
        if (retention.getBoolean("enabled", false)) {
            retentionTask = new RetentionTask(retention);
            retentionTask.start();
        }

        Config logRetention = cfg.section("log-retention");
        if (logRetention.getBoolean("enabled", false)) {
            logRetentionTask = new LogRetentionTask(logRetention);
            logRetentionTask.start();
        }

        Log.info("enabling " + name() + " " + version());
    }

    @Override
    public void onDisable() {
        if (backupUploader != null) backupUploader.stop();
        if (retentionTask != null) retentionTask.stop();
        if (logRetentionTask != null) logRetentionTask.stop();
    }
}
