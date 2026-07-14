package me.vasir.forgeplus.core;

import me.vasir.jdaforge.api.Bot;
import me.vasir.jdaforge.api.Config;
import me.vasir.jdaforge.api.Placeholder;
import me.vasir.jdaforge.util.Metrics;
import me.vasir.jdaforge.util.Times;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.sharding.ShardManager;

import java.lang.management.ManagementFactory;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class OpsPlaceholders {

    private OpsPlaceholders() {}

    public static void register(Placeholder p, Config cfg) {
        DateTimeFormatter dateFmt = safeFormatter(cfg.getString("date-format", "yyyy-MM-dd"), "yyyy-MM-dd");
        DateTimeFormatter timeFmt = safeFormatter(cfg.getString("time-format", "HH:mm:ss"), "HH:mm:ss");

        p.register("%uptime%", ctx -> Times.formatDuration(ManagementFactory.getRuntimeMXBean().getUptime()));
        p.register("%ram_used%", ctx -> usedMb() + " MB");
        p.register("%ram_max%", ctx -> maxMb() + " MB");
        p.register("%ram_percent%", ctx -> ramPercent() + "%");
        p.register("%cpu_cores%", ctx -> Metrics.getCpuCores());
        p.register("%guild_count%", ctx -> guildCount());
        p.register("%latency%", ctx -> {
            JDA jda = Bot.primaryJda();
            return jda != null ? jda.getGatewayPing() + "ms" : "0ms";
        });
        p.register("%thread_count%", ctx -> Thread.activeCount());
        p.register("%date%", ctx -> LocalDateTime.now().format(dateFmt));
        p.register("%time%", ctx -> LocalDateTime.now().format(timeFmt));
    }

    private static long usedMb() {
        Runtime rt = Runtime.getRuntime();
        return (rt.totalMemory() - rt.freeMemory()) / 1048576L;
    }

    private static long maxMb() {
        long max = Runtime.getRuntime().maxMemory();
        return (max == Long.MAX_VALUE ? Runtime.getRuntime().totalMemory() : max) / 1048576L;
    }

    private static long ramPercent() {
        long max = maxMb();
        return max <= 0 ? 0 : Math.round((usedMb() * 100.0) / max);
    }

    private static int guildCount() {
        Object core = Bot.core();
        if (core instanceof ShardManager sm) return sm.getGuilds().size();
        if (core instanceof JDA jda) return jda.getGuilds().size();
        return 0;
    }

    private static DateTimeFormatter safeFormatter(String pattern, String fallback) {
        try {
            return DateTimeFormatter.ofPattern(pattern);
        } catch (Exception e) {
            return DateTimeFormatter.ofPattern(fallback);
        }
    }
}
