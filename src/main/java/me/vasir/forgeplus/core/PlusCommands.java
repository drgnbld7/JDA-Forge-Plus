package me.vasir.forgeplus.core;

import me.vasir.forgeplus.backup.BackupSender;
import me.vasir.jdaforge.api.Bot;
import me.vasir.jdaforge.api.Config;
import me.vasir.jdaforge.api.Modules;
import me.vasir.jdaforge.util.Colors;
import me.vasir.jdaforge.util.Threads;
import me.vasir.jdaforge.util.Times;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDAInfo;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.InteractionContextType;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.sharding.ShardManager;

import java.awt.Color;
import java.io.File;
import java.lang.management.ManagementFactory;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class PlusCommands extends ListenerAdapter {

    private static final String FOOTER = "JDA-Forge-Plus";
    private static final Set<String> OWNED = Set.of(
            "botinfo", "ping", "uptime", "serverinfo", "userinfo", "avatar", "backup", "reload", "modules");

    private final boolean requireAdmin;
    private final boolean ephemeral;
    private final Set<Long> allowedRoles;
    private final Color embedColor;

    private final boolean cmdBotinfo, cmdPing, cmdUptime, cmdServerinfo, cmdUserinfo, cmdAvatar, cmdBackup;

    private final long backupChannelId;
    private final int backupMaxSizeMb;
    private final String backupMessage;
    private final boolean backupDeleteAfter;

    public PlusCommands(Config root) {
        Config cmds = root.section("commands");
        this.requireAdmin = cmds.getBoolean("require-admin", true);
        this.ephemeral = cmds.getBoolean("ephemeral", true);
        this.allowedRoles = new HashSet<>(cmds.getLongList("allowed-role-ids"));

        Config info = root.section("info-commands");
        this.embedColor = Colors.fromHex(info.getString("embed-color", "#9bb0cc"));
        this.cmdBotinfo = info.getBoolean("botinfo", true);
        this.cmdPing = info.getBoolean("ping", true);
        this.cmdUptime = info.getBoolean("uptime", true);
        this.cmdServerinfo = info.getBoolean("serverinfo", true);
        this.cmdUserinfo = info.getBoolean("userinfo", true);
        this.cmdAvatar = info.getBoolean("avatar", true);

        Config backup = root.section("backup-to-discord");
        this.cmdBackup = backup.getBoolean("command-enabled", true);
        this.backupChannelId = parseLong(backup.getString("channel", "0"));
        this.backupMaxSizeMb = backup.getInt("max-size-mb", 0);
        this.backupMessage = backup.getString("message", "Database backup");
        this.backupDeleteAfter = backup.getBoolean("delete-after-upload", false);
    }

    public List<CommandData> buildCommands() {
        List<CommandData> out = new ArrayList<>();
        if (cmdBotinfo) out.add(gate(Commands.slash("botinfo", "Bot status: uptime, RAM, CPU, guilds, ping.")));
        if (cmdPing) out.add(gate(Commands.slash("ping", "Show the gateway and REST latency.")));
        if (cmdUptime) out.add(gate(Commands.slash("uptime", "Show how long the bot has been running.")));
        if (cmdServerinfo) out.add(gate(Commands.slash("serverinfo", "Show information about this server.")));
        if (cmdUserinfo) out.add(gate(Commands.slash("userinfo", "Show information about a user.")
                .addOption(OptionType.USER, "user", "The user to inspect (defaults to you).", false)));
        if (cmdAvatar) out.add(gate(Commands.slash("avatar", "Show a user's avatar.")
                .addOption(OptionType.USER, "user", "The user (defaults to you).", false)));
        if (cmdBackup) out.add(gate(Commands.slash("backup", "Upload the most recent database backup to Discord.")));
        out.add(gate(Commands.slash("reload", "Reload all modules.")));
        out.add(gate(Commands.slash("modules", "List loaded modules and their status.")));
        return out;
    }

    private SlashCommandData gate(SlashCommandData data) {
        data.setContexts(InteractionContextType.GUILD);
        if (requireAdmin) {
            data.setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR));
        }
        return data;
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent e) {
        if (!OWNED.contains(e.getName())) return;

        if (!isAllowed(e.getMember())) {
            e.reply("⛔ You are not allowed to use this command.").setEphemeral(true).queue();
            return;
        }

        switch (e.getName()) {
            case "botinfo" -> handleBotinfo(e);
            case "ping" -> handlePing(e);
            case "uptime" -> handleUptime(e);
            case "serverinfo" -> handleServerinfo(e);
            case "userinfo" -> handleUserinfo(e);
            case "avatar" -> handleAvatar(e);
            case "backup" -> handleBackup(e);
            case "reload" -> handleReload(e);
            case "modules" -> handleModules(e);
            default -> { }
        }
    }

    private boolean isAllowed(Member member) {
        if (member == null) return false;
        if (member.hasPermission(Permission.ADMINISTRATOR)) return true;
        if (!allowedRoles.isEmpty()
                && member.getRoles().stream().anyMatch(r -> allowedRoles.contains(r.getIdLong()))) {
            return true;
        }
        return !requireAdmin && allowedRoles.isEmpty();
    }

    private void handleBotinfo(SlashCommandInteractionEvent e) {
        JDA jda = e.getJDA();
        Runtime rt = Runtime.getRuntime();
        long usedMb = (rt.totalMemory() - rt.freeMemory()) / 1048576L;
        long maxMb = (rt.maxMemory() == Long.MAX_VALUE ? rt.totalMemory() : rt.maxMemory()) / 1048576L;
        long uptime = ManagementFactory.getRuntimeMXBean().getUptime();

        EmbedBuilder eb = base("🤖 " + jda.getSelfUser().getName())
                .setThumbnail(jda.getSelfUser().getEffectiveAvatarUrl());
        eb.addField("⏱️ Uptime", "`" + Times.formatDuration(uptime) + "`", true);
        eb.addField("📡 Gateway", "`" + jda.getGatewayPing() + " ms`", true);
        eb.addField("🌐 Guilds", "`" + guildCount() + "`", true);
        eb.addField("💾 Memory", "`" + usedMb + " / " + maxMb + " MB`", true);
        eb.addField("🧠 CPU Cores", "`" + rt.availableProcessors() + "`", true);
        eb.addField("🧵 Threads", "`" + Thread.activeCount() + "`", true);
        eb.addField("☕ Java", "`" + System.getProperty("java.version") + "`", true);
        eb.addField("📚 JDA", "`" + JDAInfo.VERSION + "`", true);
        reply(e, eb);
    }

    private void handlePing(SlashCommandInteractionEvent e) {
        long gateway = e.getJDA().getGatewayPing();
        e.getJDA().getRestPing().queue(rest -> {
            EmbedBuilder eb = base("🏓 Pong!");
            eb.addField("📡 Gateway", "`" + gateway + " ms`", true);
            eb.addField("🌐 REST", "`" + rest + " ms`", true);
            e.replyEmbeds(eb.build()).setEphemeral(ephemeral).queue();
        });
    }

    private void handleUptime(SlashCommandInteractionEvent e) {
        long uptime = ManagementFactory.getRuntimeMXBean().getUptime();
        EmbedBuilder eb = base("⏱️ Uptime");
        eb.setDescription("The bot has been running for **" + Times.formatDuration(uptime) + "**.");
        reply(e, eb);
    }

    private void handleServerinfo(SlashCommandInteractionEvent e) {
        Guild g = e.getGuild();
        if (g == null) {
            e.reply("⚠️ This command can only be used in a server.").setEphemeral(true).queue();
            return;
        }
        EmbedBuilder eb = base("📊 " + g.getName());
        if (g.getIconUrl() != null) eb.setThumbnail(g.getIconUrl());
        eb.addField("🆔 ID", "`" + g.getId() + "`", true);
        eb.addField("👥 Members", "`" + g.getMemberCount() + "`", true);
        eb.addField("🚀 Boosts", "`" + g.getBoostCount() + "` (Tier " + g.getBoostTier().getKey() + ")", true);
        eb.addField("💬 Channels", "`" + g.getChannels().size() + "`", true);
        eb.addField("🎭 Roles", "`" + g.getRoles().size() + "`", true);
        eb.addField("📅 Created", relative(g.getTimeCreated()), true);
        g.retrieveOwner().queue(
                owner -> {
                    eb.addField("👑 Owner", owner.getUser().getAsMention(), true);
                    e.replyEmbeds(eb.build()).setEphemeral(ephemeral).queue();
                },
                err -> e.replyEmbeds(eb.build()).setEphemeral(ephemeral).queue()
        );
    }

    private void handleUserinfo(SlashCommandInteractionEvent e) {
        User user = resolveUser(e);
        Guild g = e.getGuild();
        EmbedBuilder eb = base("👤 " + user.getName())
                .setThumbnail(user.getEffectiveAvatarUrl());
        eb.addField("🆔 ID", "`" + user.getId() + "`", true);
        eb.addField("🤖 Bot", user.isBot() ? "Yes" : "No", true);
        eb.addField("📅 Created", relative(user.getTimeCreated()), true);

        Member m = (g != null) ? g.getMember(user) : null;
        if (m != null) {
            eb.addField("📥 Joined", relative(m.getTimeJoined()), true);
            eb.addField("🎭 Roles", "`" + m.getRoles().size() + "`", true);
            if (m.getColor() != null) eb.setColor(m.getColor());
        }
        reply(e, eb);
    }

    private void handleAvatar(SlashCommandInteractionEvent e) {
        User user = resolveUser(e);
        EmbedBuilder eb = base("🖼️ " + user.getName() + "'s Avatar");
        eb.setImage(user.getEffectiveAvatarUrl() + "?size=512");
        reply(e, eb);
    }

    private void handleBackup(SlashCommandInteractionEvent e) {
        if (backupChannelId <= 0) {
            e.reply("⚠️ `backup-to-discord.channel` is not set in the config.").setEphemeral(true).queue();
            return;
        }
        File latest = latestBackup();
        if (latest == null) {
            e.reply("📭 No backup found in the `backups/` folder yet.").setEphemeral(true).queue();
            return;
        }
        BackupSender.Result result = BackupSender.send(
                latest, backupChannelId, backupMaxSizeMb, backupMessage, backupDeleteAfter);
        String msg = switch (result) {
            case SENT -> "✅ Uploading the latest backup — `" + latest.getName() + "`";
            case TOO_LARGE -> "❌ Backup `" + latest.getName() + "` exceeds the channel's upload limit.";
            case NO_CHANNEL -> "❌ Target channel not found (`" + backupChannelId + "`).";
            case NO_JDA -> "❌ The bot is not connected to Discord.";
            case ERROR -> "❌ Something went wrong while uploading the backup.";
        };
        e.reply(msg).setEphemeral(true).queue();
    }

    private void handleReload(SlashCommandInteractionEvent e) {
        e.reply("🔄 Reloading modules…").setEphemeral(true).queue(hook ->
                Threads.async(() -> {
                    try {
                        Modules.reload();
                        hook.editOriginal("✅ Reloaded **" + Modules.list().size() + "** module(s).").queue();
                    } catch (Exception ex) {
                        hook.editOriginal("❌ Reload failed: " + ex.getMessage()).queue();
                    }
                })
        );
    }

    private void handleModules(SlashCommandInteractionEvent e) {
        List<Modules.Info> list = Modules.list();
        EmbedBuilder eb = base("📦 Loaded Modules — " + list.size());
        if (list.isEmpty()) {
            eb.setDescription("No modules are currently loaded.");
        } else {
            StringBuilder sb = new StringBuilder();
            for (Modules.Info m : list) {
                sb.append(m.enabled() ? "🟢 " : "🔴 ")
                  .append("**").append(m.name()).append("**")
                  .append("  `v").append(m.version()).append("`")
                  .append("  ·  ").append(m.author())
                  .append('\n');
            }
            eb.setDescription(sb.toString());
        }
        reply(e, eb);
    }

    // ---- Helpers --------------------------------------------------------------

    private EmbedBuilder base(String title) {
        EmbedBuilder eb = new EmbedBuilder()
                .setTitle(title)
                .setFooter(FOOTER)
                .setTimestamp(Instant.now());
        if (embedColor != null) eb.setColor(embedColor);
        return eb;
    }

    private void reply(SlashCommandInteractionEvent e, EmbedBuilder eb) {
        e.replyEmbeds(eb.build()).setEphemeral(ephemeral).queue();
    }

    /** Renders a Discord relative timestamp (e.g. "2 years ago"); hovering shows the exact date. */
    private static String relative(OffsetDateTime time) {
        return "<t:" + time.toEpochSecond() + ":R>";
    }

    private static User resolveUser(SlashCommandInteractionEvent e) {
        OptionMapping opt = e.getOption("user");
        return opt != null ? opt.getAsUser() : e.getUser();
    }

    private static int guildCount() {
        Object core = Bot.core();
        if (core instanceof ShardManager sm) return sm.getGuilds().size();
        if (core instanceof JDA jda) return jda.getGuilds().size();
        return 0;
    }

    private File latestBackup() {
        File[] files = new File("backups").listFiles((d, n) -> n.toLowerCase().endsWith(".zip"));
        if (files == null || files.length == 0) return null;
        File newest = files[0];
        for (File f : files) {
            if (f.lastModified() > newest.lastModified()) newest = f;
        }
        return newest;
    }

    private static long parseLong(String s) {
        try {
            return Long.parseLong(s.trim());
        } catch (Exception e) {
            return 0L;
        }
    }
}
