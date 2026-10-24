package platformjumper;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

/**
 * Local accounts. Passwords are never stored in plain text: each user has a
 * random salt and a PBKDF2 hash. Progress (unlocked level and best score per
 * level) is stored with the account. Users are found with a Linear Search.
 */
public class UserManager {
    private static final UserManager INSTANCE = new UserManager();
    private static final int ITERATIONS = 20000;

    private final List<User> users = new ArrayList<>();
    private final Path file = Paths.get(System.getProperty("user.home"), ".platform-jumper-users.txt");
    private User current;

    public static UserManager getInstance() { return INSTANCE; }
    private UserManager() { load(); }

    public static class User {
        public final String name;
        private final byte[] salt;
        private final byte[] hash;
        int unlocked = 1;
        final int[] best = new int[Level.TOTAL_LEVELS + 1]; // index 1..TOTAL_LEVELS
        User(String name, byte[] salt, byte[] hash) { this.name = name; this.salt = salt; this.hash = hash; }
        public int total() { int t = 0; for (int i = 1; i < best.length; i++) t += best[i]; return t; }
    }

    // ---- accounts ----

    /** Returns null on success, or an error message. */
    public String register(String name, String password) {
        name = name == null ? "" : name.trim();
        if (!name.matches("[A-Za-z0-9_]{3,16}")) return "Username must be 3-16 letters, numbers or _";
        if (password == null || password.length() < 4) return "Password must be at least 4 characters";
        if (findUser(name) != null) return "That username is already taken";
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        User u = new User(name, salt, hash(password, salt));
        users.add(u);
        current = u;
        save();
        return null;
    }

    /** Returns null on success, or an error message. */
    public String login(String name, String password) {
        User u = findUser(name == null ? "" : name.trim());
        if (u == null || password == null || !java.util.Arrays.equals(u.hash, hash(password, u.salt)))
            return "Wrong username or password";
        current = u;
        return null;
    }

    public void logout() { current = null; }
    public boolean isLoggedIn() { return current != null; }
    public String getCurrentName() { return current == null ? "" : current.name; }

    /** Linear Search by username (case-insensitive). */
    private User findUser(String name) {
        for (User u : users) if (u.name.equalsIgnoreCase(name)) return u;
        return null;
    }

    // ---- progress ----

    public int getUnlocked() { return current == null ? 1 : current.unlocked; }
    public int getBest(int level) { return current == null ? 0 : current.best[level]; }
    public int getTotal() { return current == null ? 0 : current.total(); }

    /** Saves a completed level: keeps the best score and unlocks the next level. */
    public void recordLevel(int level, int score) {
        if (current == null) return;
        if (score > current.best[level]) current.best[level] = score;
        if (level + 1 > current.unlocked) current.unlocked = Math.min(Level.TOTAL_LEVELS, level + 1);
        save();
    }

    public List<User> getUsers() { return Collections.unmodifiableList(users); }

    // ---- storage ----

    private static byte[] hash(String password, byte[] salt) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, 256);
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private void save() {
        try (BufferedWriter w = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            for (User u : users) {
                StringBuilder scores = new StringBuilder();
                for (int i = 1; i < u.best.length; i++) scores.append(i > 1 ? "," : "").append(u.best[i]);
                w.write(u.name + "|" + Base64.getEncoder().encodeToString(u.salt) + "|"
                    + Base64.getEncoder().encodeToString(u.hash) + "|" + u.unlocked + "|" + scores + System.lineSeparator());
            }
        } catch (IOException ex) {
            System.err.println("Could not save users: " + ex.getMessage());
        }
    }

    private void load() {
        if (!Files.exists(file)) return;
        try {
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                String[] p = line.split("\\|");
                if (p.length < 5) continue;
                try {
                    User u = new User(p[0], Base64.getDecoder().decode(p[1]), Base64.getDecoder().decode(p[2]));
                    u.unlocked = Math.max(1, Math.min(Level.TOTAL_LEVELS, Integer.parseInt(p[3])));
                    String[] sc = p[4].split(",");
                    for (int i = 0; i < sc.length && i + 1 < u.best.length; i++) u.best[i + 1] = Integer.parseInt(sc[i].trim());
                    users.add(u);
                } catch (IllegalArgumentException ignored) { /* skip malformed account */ }
            }
        } catch (IOException ex) {
            System.err.println("Could not load users: " + ex.getMessage());
        }
    }
}
