package platformjumper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Leaderboard built from the signed-in accounts: each player's score is the
 * sum of their best score on every level. Uses a manual Bubble Sort and
 * Linear Search.
 */
public class Leaderboard {
    private static final Leaderboard INSTANCE = new Leaderboard();
    private final List<Record> records = new ArrayList<>();

    public static Leaderboard getInstance() { return INSTANCE; }
    private Leaderboard() {}

    public static class Record {
        public final String name;
        public final int score;
        public Record(String name, int score) { this.name = name; this.score = score; }
    }

    /** Rebuilds the list from the accounts (only players with a score). */
    private void reload() {
        records.clear();
        for (UserManager.User u : UserManager.getInstance().getUsers()) {
            if (u.total() > 0) records.add(new Record(u.name, u.total()));
        }
        bubbleSort();
    }

    /** Bubble Sort descending. Swaps whole records so names stay paired with scores. */
    public void bubbleSort() {
        for (int i = 0; i < records.size() - 1; i++) {
            boolean swapped = false;
            for (int j = 0; j < records.size() - 1 - i; j++) {
                if (records.get(j).score < records.get(j + 1).score) {
                    Record temp = records.get(j);
                    records.set(j, records.get(j + 1));
                    records.set(j + 1, temp);
                    swapped = true;
                }
            }
            if (!swapped) break;
        }
    }

    /** Linear Search by name, case-insensitive; returns first matching record or null. */
    public Record searchByName(String name) {
        if (name == null) return null;
        reload();
        for (Record r : records) if (r.name.equalsIgnoreCase(name.trim())) return r;
        return null;
    }

    /** Linear Search by exact score; returns first matching record or null. */
    public Record searchByScore(int score) {
        reload();
        for (Record r : records) if (r.score == score) return r;
        return null;
    }

    public List<Record> getRecords() {
        reload();
        return Collections.unmodifiableList(new ArrayList<>(records));
    }
}
