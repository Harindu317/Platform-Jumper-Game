package platformjumper;

import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;

/**
 * A hazard. Every world has its own three kinds:
 *   Earth: CACTUS, BIRD
 *   Space: ASTEROID, LASER, UFO
 *   Moon : CRYSTAL, METEOR, ALIEN
 * Some kinds are static, some patrol, some run on a timer (laser / meteor).
 */
public class Obstacle {
    public enum Kind { SPIKE, CACTUS, BIRD, ASTEROID, LASER, UFO, CRYSTAL, METEOR, ALIEN }

    // Timer cycles (in frames, the game runs at about 60 frames per second).
    public static final int LASER_CYCLE = 150, LASER_WARN_AT = 60, LASER_ON_AT = 90;
    public static final int METEOR_CYCLE = 210, METEOR_WARN_AT = 50, METEOR_FALL_AT = 100;

    public final Kind kind;
    public final int width, height;
    public int x, y;
    public boolean triggered;
    public boolean facingRight = true;

    private final int baseX, baseY;
    private final int range;      // patrol / bob distance, or meteor fall distance
    private final int phase;      // start offset in frames so hazards are not all in sync
    private final double speed;
    private int tick;

    /** Plain spike (kept for compatibility with the old constructor). */
    public Obstacle(int x, int y, int width, int height) {
        this(Kind.SPIKE, x, y, width, height, 0, 0, 0);
    }

    public Obstacle(Kind kind, int x, int y, int width, int height, int range, double speed, int phase) {
        this.kind = kind;
        this.baseX = x; this.baseY = y;
        this.width = width; this.height = height;
        this.range = Math.max(range, 0);
        this.speed = speed;
        this.phase = phase;
        applyPosition();
    }

    /** True for kinds that patrol sideways along a platform. */
    public static boolean isPatrol(Kind k) { return k == Kind.BIRD || k == Kind.UFO || k == Kind.ALIEN; }

    public int animTick() { return tick; }

    public void update() {
        tick++;
        applyPosition();
    }

    private void applyPosition() {
        switch (kind) {
            case BIRD: case UFO: case ALIEN: {
                double span = 2.0 * range;
                double pos = span <= 0 ? 0 : ((tick + phase) * speed) % span;
                boolean forward = pos < range;
                x = baseX + (int) (forward ? pos : span - pos);
                y = baseY;
                facingRight = forward;
                break;
            }
            case ASTEROID: {
                double span = 2.0 * range;
                double pos = span <= 0 ? 0 : ((tick + phase) * speed) % span;
                x = baseX;
                y = baseY + (int) (pos < range ? pos : span - pos);
                break;
            }
            case METEOR: {
                int t = (tick + phase) % METEOR_CYCLE;
                x = baseX;
                y = t >= METEOR_FALL_AT ? baseY + (int) ((t - METEOR_FALL_AT) * speed) : baseY;
                break;
            }
            default:
                x = baseX; y = baseY;
        }
    }

    /** 0 = off, 1 = warning flicker (harmless), 2 = deadly. Only meaningful for lasers. */
    public int laserState() {
        int t = (tick + phase) % LASER_CYCLE;
        return t >= LASER_ON_AT ? 2 : (t >= LASER_WARN_AT ? 1 : 0);
    }

    /** True while a meteor is about to fall (harmless telegraph). */
    public boolean meteorWarning() {
        int t = (tick + phase) % METEOR_CYCLE;
        return t >= METEOR_WARN_AT && t < METEOR_FALL_AT;
    }

    /** Y position where a meteor lands (the platform surface). */
    public int meteorImpactY() { return baseY + range + height; }

    /** Whether the hazard can hurt right now. */
    public boolean isActive() {
        switch (kind) {
            case LASER:  return laserState() == 2;
            case METEOR: {
                int t = (tick + phase) % METEOR_CYCLE;
                return t >= METEOR_FALL_AT && (t - METEOR_FALL_AT) * speed < range;
            }
            default: return true;
        }
    }

    public Rectangle bounds() { return new Rectangle(x, y, width, height); }

    /** The visible danger area, so only touching the hazard itself counts (not empty corners). */
    public Shape shape() {
        switch (kind) {
            case SPIKE:
                return new Polygon(new int[]{x, x + width / 2, x + width},
                                   new int[]{y + height, y, y + height}, 3);
            case CRYSTAL:
                return new Polygon(
                    new int[]{x, x + width * 20 / 100, x + width * 35 / 100, x + width * 55 / 100,
                              x + width * 75 / 100, x + width * 90 / 100, x + width},
                    new int[]{y + height, y + height * 40 / 100, y + height * 62 / 100, y,
                              y + height * 50 / 100, y + height * 30 / 100, y + height}, 7);
            case CACTUS:
                return new Rectangle2D.Double(x + 3, y + 2, width - 6, height - 2);
            case LASER:
                return new Rectangle2D.Double(x, y, width, height);
            case BIRD: case UFO: case ALIEN:
                return new Ellipse2D.Double(x + 3, y + 3, width - 6, height - 5);
            default: // ASTEROID, METEOR
                return new Ellipse2D.Double(x + 3, y + 3, width - 6, height - 6);
        }
    }

    public boolean hits(Rectangle r) { return isActive() && shape().intersects(r); }
}
