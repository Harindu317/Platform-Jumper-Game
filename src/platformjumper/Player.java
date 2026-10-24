package platformjumper;

import java.awt.Rectangle;

public class Player {
    int x, y, width = 28, height = 36;
    double vx, vy;
    boolean onGround;
    private boolean jumpWasDown;
    // Physics differ per world (Earth normal, Space floaty, Moon low gravity).
    private double gravity = 0.52, jumpPower = 11.5;

    public Player(int x, int y) { this.x = x; this.y = y; }

    public Rectangle bounds() { return new Rectangle(x, y, width, height); }

    public void update(boolean left, boolean right, boolean jump, Platform[] platforms, int worldWidth) {
        vx = left == right ? 0 : (left ? -4.0 : 4.0);
        if (jump && !jumpWasDown && onGround) {
            vy = -jumpPower;
            onGround = false;
        }
        jumpWasDown = jump;

        int oldY = y;
        x += (int) vx;
        if (x < 0) x = 0;
        if (x + width > worldWidth) x = worldWidth - width;

        vy += gravity;
        if (vy > 12) vy = 12;
        y += (int) vy;
        onGround = false;

        if (vy >= 0) {
            Rectangle now = bounds();
            for (Platform p : platforms) {
                Rectangle pr = p.bounds();
                if (now.x < pr.x + pr.width && now.x + now.width > pr.x &&
                    oldY + height <= pr.y + 4 && y + height >= pr.y) {
                    y = pr.y - height;
                    vy = 0;
                    onGround = true;
                    break;
                }
            }
        }
    }

    public void setPhysics(double gravity, double jumpPower) { this.gravity = gravity; this.jumpPower = jumpPower; }

    public void bounce() { vy = -7.5; onGround = false; }
    public void reset(int startX, int startY) { x=startX; y=startY; vx=0; vy=0; onGround=false; jumpWasDown=false; }
}
