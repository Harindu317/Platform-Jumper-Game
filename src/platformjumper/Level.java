package platformjumper;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Level layouts use arrays as the assignment's explicit array demonstration.
 * The 15 levels are split into three worlds of five levels each:
 *   levels 1-5   Earth  (normal gravity;  cacti, birds)
 *   levels 6-10  Space  (floaty gravity;  asteroids, lasers, UFOs)
 *   levels 11-15 Moon   (low gravity;     crystals, meteors, aliens)
 * Every level is generated from a fixed seed, so each player always gets the
 * same layout, and difficulty rises from level to level.
 */
public class Level {
    public static final int TOTAL_LEVELS = 15;
    public static final int LEVELS_PER_WORLD = 5;
    public static final int EARTH = 0, SPACE = 1, MOON = 2;

    private static final String[] WORLD_NAMES = {"EARTH", "SPACE", "MOON"};
    private static final Obstacle.Kind[][] WORLD_KINDS = {
        {Obstacle.Kind.CACTUS,   Obstacle.Kind.CACTUS, Obstacle.Kind.BIRD},
        {Obstacle.Kind.ASTEROID, Obstacle.Kind.LASER,  Obstacle.Kind.UFO},
        {Obstacle.Kind.CRYSTAL,  Obstacle.Kind.METEOR, Obstacle.Kind.ALIEN}
    };

    // Per-world tuning: {gap base, gap growth, gap random, max gap, max gap after a climb}
    private static final int[][] GAP = {
        {50, 50, 21, 110,  85},   // Earth
        {60, 55, 26, 125,  95},   // Space (longer jumps)
        {75, 70, 31, 165, 120}    // Moon  (much longer jumps)
    };
    private static final double[] GRAVITY    = {0.52, 0.42, 0.28};
    private static final double[] JUMP_POWER = {11.5, 10.5, 9.6};

    public final int number;
    public final int world;
    public final double gravity, jumpPower;
    public int worldWidth = 1800;
    public Platform[] platforms;
    public Coin[] coins;
    public Obstacle[] obstacles;
    public int[][] checkpoints = new int[0][];   // each entry {playerX, playerY}
    public final int startX = 45, startY = 400;
    public int finishX = 1700;

    public Level(int number) {
        this.number = number;
        this.world = worldOf(number);
        this.gravity = GRAVITY[world];
        this.jumpPower = JUMP_POWER[world];
        generate(number);
    }

    public static int worldOf(int n) { return (n - 1) / LEVELS_PER_WORLD; }
    /** 1..5 inside its world. */
    public static int stageOf(int n) { return (n - 1) % LEVELS_PER_WORLD + 1; }
    public static String worldName(int n) { return WORLD_NAMES[worldOf(n)]; }

    /** Difficulty label (kept for other screens). */
    public static String difficultyName(int n) {
        if (n <= 4) return "EASY";
        if (n <= 8) return "NORMAL";
        if (n <= 12) return "HARD";
        return "EXPERT";
    }

    private void generate(int n) {
        Random rnd = new Random(1000L * n + 7);
        int stage = stageOf(n) - 1;                  // 0..4 inside the world
        double d = (n - 1) / 14.0;                   // 0 .. 1 over the whole game
        int count = 10 + n + n / 3;                  // platforms in the middle (11 .. 30): longer levels
        int[] gp = GAP[world];
        Obstacle.Kind[] kinds = WORLD_KINDS[world];
        int available = stage < 2 ? 2 : 3;           // the third hazard of each world appears from its 3rd level

        List<Platform> pl = new ArrayList<>();
        List<Coin> co = new ArrayList<>();
        List<Obstacle> ob = new ArrayList<>();
        List<int[]> cps = new ArrayList<>();

        pl.add(new Platform(0, 480, 220, 40));
        co.add(new Coin(130, 440));
        int x = 220, y = 480;
        for (int i = 0; i < count; i++) {
            // Checkpoint every 7th platform, but never within the last 4 platforms before the finish flag.
            boolean checkpoint = i % 7 == 6 && count - 1 - i >= 4;
            int gap = gp[0] + (int) (d * gp[1]) + rnd.nextInt(gp[2]);
            int w = 210 - (int) (d * 40) + rnd.nextInt(41);
            int dy = rnd.nextInt(111) - 55;          // negative = higher platform
            if (world == MOON) dy = rnd.nextInt(131) - 65;
            int ny = Math.max(300, Math.min(470, y + dy));
            if (y - ny > 35) gap = Math.min(gap, gp[4]);   // big climbs need a shorter gap
            gap = Math.min(gap, gp[3]);

            // Decide what (if anything) sits on this platform.
            double chance = 0.55 + 0.25 * d + 0.15 * (stage / 4.0);
            boolean hazard = i > 0 && !checkpoint && rnd.nextDouble() < chance;
            Obstacle.Kind kind = kinds[rnd.nextInt(available)];
            boolean doubleHazard = hazard && !Obstacle.isPatrol(kind) && rnd.nextDouble() < 0.15 + d * 0.45;
            if (checkpoint) w = Math.max(w, 230);
            else if (doubleHazard) w = Math.max(w, 260);
            else if (hazard && Obstacle.isPatrol(kind)) w = Math.max(w, 220);
            else if (hazard) w = Math.max(w, 180);

            int px = x + gap;
            pl.add(new Platform(px, ny, w, 22));

            if (checkpoint) {
                cps.add(new int[]{px + w / 2 - 14, ny - 40});
            } else if (doubleHazard) {
                ob.add(makeHazard(kind, px, ny, w, 0.28, d, rnd));
                Obstacle.Kind second = kinds[rnd.nextInt(available)];
                if (Obstacle.isPatrol(second)) second = kind;
                ob.add(makeHazard(second, px, ny, w, 0.72, d, rnd));
            } else if (hazard) {
                ob.add(makeHazard(kind, px, ny, w, 0.5, d, rnd));
            }

            co.add(new Coin(px + w / 2 - 8 + rnd.nextInt(41) - 20, ny - 48));
            x = px + w;
            y = ny;
        }
        int px = x + 80;
        pl.add(new Platform(px, 480, 260, 40));
        worldWidth = px + 260;
        finishX = worldWidth - 110;
        platforms = pl.toArray(new Platform[0]);
        coins = co.toArray(new Coin[0]);
        obstacles = ob.toArray(new Obstacle[0]);
        checkpoints = cps.toArray(new int[0][]);
    }

    /** Builds one hazard sitting on (or flying above) the platform at px, ny with width w. */
    private Obstacle makeHazard(Obstacle.Kind kind, int px, int ny, int w, double at, double d, Random rnd) {
        int cx = px + (int) (w * at) + rnd.nextInt(21) - 10;     // centre x for fixed hazards
        int patrolStart = px + (int) (w * 0.2);
        int patrolRange = Math.max(40, (int) (w * 0.6));          // patrol covers the middle 60% only
        switch (kind) {
            case SPIKE:
                return new Obstacle(Obstacle.Kind.SPIKE, cx - 15, ny - 24, 30, 24, 0, 0, 0);
            case CACTUS: {
                int ch = 36 + rnd.nextInt(11);   // slight height variety (36-46)
                return new Obstacle(Obstacle.Kind.CACTUS, cx - 12, ny - ch, 24, ch, 0, 0, 0);
            }
            case BIRD:
                return new Obstacle(Obstacle.Kind.BIRD, patrolStart, ny - 70, 40, 22,
                    patrolRange - 40, 1.5 + 0.7 * d, rnd.nextInt(300));
            case ASTEROID:
                return new Obstacle(Obstacle.Kind.ASTEROID, cx - 15, ny - 116, 30, 30,
                    62, 1.3 + 0.7 * d, rnd.nextInt(300));
            case LASER:
                return new Obstacle(Obstacle.Kind.LASER, cx - 5, ny - 80, 10, 80, 0, 0,
                    rnd.nextInt(Obstacle.LASER_CYCLE));
            case UFO:
                return new Obstacle(Obstacle.Kind.UFO, patrolStart, ny - 74, 46, 22,
                    patrolRange - 46, 1.4 + 0.7 * d, rnd.nextInt(300));
            case CRYSTAL:
                return new Obstacle(Obstacle.Kind.CRYSTAL, cx - 15, ny - 36, 30, 36, 0, 0, 0);
            case METEOR: {
                int top = -70;
                return new Obstacle(Obstacle.Kind.METEOR, cx - 13, top, 26, 26,
                    ny - 26 - top, 9.0, rnd.nextInt(Obstacle.METEOR_CYCLE));
            }
            default: // ALIEN
                return new Obstacle(Obstacle.Kind.ALIEN, patrolStart, ny - 22, 30, 22,
                    patrolRange - 30, 1.2 + 0.6 * d, rnd.nextInt(300));
        }
    }

    /**
     * Controlled recursion: counts how many levels are still left after
     * {@code completedLevel}. Each call advances one level and the base case
     * (all levels done) returns 0, so it always terminates.
     */
    public static int levelsRemaining(int completedLevel) {
        if (completedLevel >= TOTAL_LEVELS) return 0;
        return 1 + levelsRemaining(completedLevel + 1);
    }
}
