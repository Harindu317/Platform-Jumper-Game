package platformjumper;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Manages player progression (unlocked levels and best scores).
 * Anyone can play directly without an account.
 */
public class ProgressManager {
    private static final ProgressManager INSTANCE = new ProgressManager();

    private int unlocked = 1;
    private final int[] best = new int[Level.TOTAL_LEVELS + 1]; // index 1..TOTAL_LEVELS
    private final Path file = Paths.get(System.getProperty("user.home"), ".jumpbro-progress.txt");

    public static ProgressManager getInstance() {
        return INSTANCE;
    }

    private ProgressManager() {
        load();
    }

    public int getUnlocked() {
        return unlocked;
    }

    public int getBest(int level) {
        if (level < 1 || level >= best.length) return 0;
        return best[level];
    }

    public int getTotal() {
        int t = 0;
        for (int i = 1; i < best.length; i++) {
            t += best[i];
        }
        return t;
    }

    /** Saves a completed level: updates best score and unlocks next level. */
    public void recordLevel(int level, int score) {
        if (level >= 1 && level < best.length) {
            if (score > best[level]) {
                best[level] = score;
            }
        }
        if (level + 1 > unlocked) {
            unlocked = Math.min(Level.TOTAL_LEVELS, level + 1);
        }
        save();
    }

    public void resetProgress() {
        unlocked = 1;
        for (int i = 1; i < best.length; i++) {
            best[i] = 0;
        }
        save();
    }

    private void save() {
        try (BufferedWriter w = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            StringBuilder scores = new StringBuilder();
            for (int i = 1; i < best.length; i++) {
                scores.append(i > 1 ? "," : "").append(best[i]);
            }
            w.write(unlocked + "|" + scores + System.lineSeparator());
        } catch (IOException ex) {
            System.err.println("Could not save progress: " + ex.getMessage());
        }
    }

    private void load() {
        if (Files.exists(file)) {
            try {
                for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                    String[] p = line.split("\\|");
                    if (p.length < 2) continue;
                    try {
                        unlocked = Math.max(1, Math.min(Level.TOTAL_LEVELS, Integer.parseInt(p[0].trim())));
                        String[] sc = p[1].split(",");
                        for (int i = 0; i < sc.length && i + 1 < best.length; i++) {
                            best[i + 1] = Integer.parseInt(sc[i].trim());
                        }
                    } catch (IllegalArgumentException ignored) {
                        /* skip malformed lines */
                    }
                }
                return;
            } catch (IOException ex) {
                System.err.println("Could not load progress: " + ex.getMessage());
            }
        }

        // Migrate from old save files if present so existing progress is not lost
        Path oldProgressFile = Paths.get(System.getProperty("user.home"), ".platform-jumper-progress.txt");
        if (Files.exists(oldProgressFile)) {
            try {
                for (String line : Files.readAllLines(oldProgressFile, StandardCharsets.UTF_8)) {
                    String[] p = line.split("\\|");
                    if (p.length < 2) continue;
                    try {
                        int prevUnlocked = Math.max(1, Math.min(Level.TOTAL_LEVELS, Integer.parseInt(p[0].trim())));
                        if (prevUnlocked > unlocked) unlocked = prevUnlocked;
                        String[] sc = p[1].split(",");
                        for (int i = 0; i < sc.length && i + 1 < best.length; i++) {
                            int score = Integer.parseInt(sc[i].trim());
                            if (score > best[i + 1]) best[i + 1] = score;
                        }
                    } catch (IllegalArgumentException ignored) {}
                }
                save();
                return;
            } catch (IOException ignored) {}
        }

        Path oldUsersFile = Paths.get(System.getProperty("user.home"), ".platform-jumper-users.txt");
        if (Files.exists(oldUsersFile)) {
            try {
                for (String line : Files.readAllLines(oldUsersFile, StandardCharsets.UTF_8)) {
                    String[] p = line.split("\\|");
                    if (p.length < 5) continue;
                    try {
                        int userUnlocked = Math.max(1, Math.min(Level.TOTAL_LEVELS, Integer.parseInt(p[3])));
                        if (userUnlocked > unlocked) unlocked = userUnlocked;
                        String[] sc = p[4].split(",");
                        for (int i = 0; i < sc.length && i + 1 < best.length; i++) {
                            int score = Integer.parseInt(sc[i].trim());
                            if (score > best[i + 1]) best[i + 1] = score;
                        }
                    } catch (IllegalArgumentException ignored) {}
                }
                save();
            } catch (IOException ignored) {}
        }
    }
}
