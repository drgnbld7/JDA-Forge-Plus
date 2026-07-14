package me.vasir.forgeplus.backup;

import me.vasir.jdaforge.api.Bot;
import me.vasir.jdaforge.api.Log;
import me.vasir.jdaforge.api.Placeholder;
import me.vasir.jdaforge.util.Files;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import net.dv8tion.jda.api.utils.FileUpload;

import java.io.File;

public final class BackupSender {

    private BackupSender() {}

    public enum Result { SENT, NO_CHANNEL, TOO_LARGE, NO_JDA, ERROR }

    public static Result send(File archive, long channelId, int maxSizeMb, String message, boolean deleteAfter) {
        JDA jda = Bot.primaryJda();
        if (jda == null) {
            Log.error("[plus] Cannot upload backup: JDA is not connected.");
            return Result.NO_JDA;
        }

        TextChannel channel = jda.getTextChannelById(channelId);
        if (channel == null) {
            Log.error("[plus] Backup channel not found (id: " + channelId + "). Check backup-to-discord.channel-id.");
            return Result.NO_CHANNEL;
        }

        long limit = (maxSizeMb > 0)
                ? maxSizeMb * 1024L * 1024L
                : channel.getGuild().getMaxFileSize();
        if (archive.length() > limit) {
            Log.error("[plus] Backup '" + archive.getName() + "' (" + humanSize(archive.length())
                    + ") exceeds the upload limit (" + humanSize(limit) + "). Skipped.");
            return Result.TOO_LARGE;
        }

        try {
            String content = Placeholder.getInstance().translate(message, null);
            FileUpload upload = FileUpload.fromData(archive);

            MessageCreateAction action = (content == null || content.isBlank())
                    ? channel.sendFiles(upload)
                    : channel.sendMessage(content).addFiles(upload);

            action.queue(
                    ok -> {
                        Log.done("[plus] Backup uploaded to Discord: " + archive.getName());
                        if (deleteAfter) Files.deleteSafely(archive);
                    },
                    err -> Log.error("[plus] Failed to upload backup '" + archive.getName() + "': " + err.getMessage())
            );
            return Result.SENT;
        } catch (Exception e) {
            Log.error("[plus] Backup upload error: " + e.getMessage());
            return Result.ERROR;
        }
    }

    public static String humanSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }
}
