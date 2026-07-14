package me.vasir.forgeplus.backup;

import me.vasir.jdaforge.util.Files;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Arrays;
import java.util.Comparator;

public final class FolderCleaner {

    private FolderCleaner() {}

    public static int prune(File dir, FilenameFilter filter, long maxAgeDays, int maxFiles, String protectedName) {
        File[] files = dir.listFiles(filter);
        if (files == null || files.length == 0) return 0;

        int removed = 0;

        if (maxAgeDays > 0) {
            long cutoff = System.currentTimeMillis() - maxAgeDays * 24L * 60L * 60L * 1000L;
            for (File f : files) {
                if (isProtected(f, protectedName)) continue;
                if (f.lastModified() < cutoff) {
                    Files.deleteSafely(f);
                    removed++;
                }
            }
        }

        if (maxFiles > 0) {
            File[] remaining = dir.listFiles(filter);
            if (remaining != null && remaining.length > maxFiles) {
                Arrays.sort(remaining, Comparator.comparingLong(File::lastModified).reversed());
                for (int i = maxFiles; i < remaining.length; i++) {
                    if (isProtected(remaining[i], protectedName)) continue;
                    Files.deleteSafely(remaining[i]);
                    removed++;
                }
            }
        }

        return removed;
    }

    private static boolean isProtected(File f, String protectedName) {
        return f.getName().equalsIgnoreCase(protectedName);
    }
}
