package platformjumper;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.util.HashSet;
import java.util.Set;

public class GamePanel extends JPanel implements ActionListener {
    private static final int VIEW_H = 600;
    private final GameFrame frame;
    private final Timer timer;
    private final Player player = new Player(45, 400);
    private final GameEventManager eventManager = new GameEventManager();
    private final Set<Integer> keys = new HashSet<>();
    private Level level;
    private int levelNumber = 1, score = 0, coinsCollected = 0, lives = 3;
    private int cameraX = 0;
    private int frameCount = 0;
    private int invincible = 0; // frames of protection after respawning
    private int checkpointIndex = -1, respawnX = 45, respawnY = 400, checkpointMsg = 0;
    private boolean facingRight = true;
    private boolean paused = false, ended = false, levelEventQueued = false;
    private Runnable pendingDialog = null;
    private final Rectangle backButton = new Rectangle(0, 46, 120, 30);
    private boolean backHover = false;

    public GamePanel(GameFrame frame) {
        this.frame = frame;
        setFocusable(true);
        setBackground(new Color(120, 202, 245));
        setPreferredSize(new Dimension(960, 600));
        timer = new Timer(16, this);
        addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                keys.add(e.getKeyCode());
                if ((e.getKeyCode() == KeyEvent.VK_B || e.getKeyCode() == KeyEvent.VK_BACK_SPACE) && !paused && !ended) { goBack(); return; }
                if ((e.getKeyCode() == KeyEvent.VK_P || e.getKeyCode() == KeyEvent.VK_ESCAPE) && !paused && !ended) pauseGame();
            }
            @Override public void keyReleased(KeyEvent e) { keys.remove(e.getKeyCode()); }
        });
        addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) { if (backButton.contains(e.getPoint())) goBack(); }
            @Override public void mouseExited(MouseEvent e) { backHover = false; }
        });
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override public void mouseMoved(MouseEvent e) {
                boolean over = backButton.contains(e.getPoint());
                if (over != backHover) { backHover = over; setCursor(Cursor.getPredefinedCursor(over ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR)); }
            }
        });
        // Prevents a stuck key if the window loses focus while a key is held.
        addFocusListener(new FocusAdapter() {
            @Override public void focusLost(FocusEvent e) { keys.clear(); }
        });
    }

    /** Starts (or restarts) a level with 3 fresh lives. */
    public void startLevel(int number) {
        levelNumber = number;
        score = 0; coinsCollected = 0; lives = 3;
        paused = false; ended = false; pendingDialog = null;
        eventManager.clear();
        loadLevel(number);
        timer.start();
        requestFocusInWindow();
    }

    /** Leaves the level and returns to the level-select screen. */
    private void goBack() {
        if (ended) return;
        setCursor(Cursor.getDefaultCursor());
        backHover = false;
        frame.showLevelSelect();
    }

    public void stopGame() { timer.stop(); keys.clear(); pendingDialog = null; }

    @Override public void actionPerformed(ActionEvent e) {
        if (!paused && !ended) updateGame();
        repaint();
        if (pendingDialog != null) {
            Runnable r = pendingDialog;
            pendingDialog = null;
            SwingUtilities.invokeLater(r);
        }
    }

    private void updateGame() {
        if (level == null) return;
        frameCount++;
        if (checkpointMsg > 0) checkpointMsg--;
        boolean left = keys.contains(KeyEvent.VK_LEFT) || keys.contains(KeyEvent.VK_A);
        boolean right = keys.contains(KeyEvent.VK_RIGHT) || keys.contains(KeyEvent.VK_D);
        boolean jump = keys.contains(KeyEvent.VK_SPACE) || keys.contains(KeyEvent.VK_UP);
        if (left != right) facingRight = right;
        player.update(left, right, jump, level.platforms, level.worldWidth);

        for (Obstacle hazard : level.obstacles) hazard.update();

        // Checkpoints: standing on a checkpoint platform saves your respawn point.
        int[][] cps = level.checkpoints;
        if (checkpointIndex + 1 < cps.length && player.onGround && player.x >= cps[checkpointIndex + 1][0]) {
            checkpointIndex++;
            respawnX = cps[checkpointIndex][0];
            respawnY = cps[checkpointIndex][1];
            checkpointMsg = 120;
        }

        for (Coin coin : level.coins) {
            if (!coin.collected && player.bounds().intersects(coin.bounds())) {
                coin.collected = true;
                eventManager.enqueue(new GameEvent(GameEvent.Type.COIN_COLLECTED, 100));
            }
        }
        if (invincible > 0) invincible--;
        // Slightly smaller box than the sprite so near-misses feel fair.
        Rectangle body = new Rectangle(player.x + 4, player.y + 4, player.width - 8, player.height - 4);
        for (Obstacle hazard : level.obstacles) {
            if (invincible == 0 && hazard.hits(body)) {
                eventManager.enqueue(new GameEvent(GameEvent.Type.OBSTACLE_HIT, 0));
                invincible = 90;
                break;
            }
        }
        if (player.y > VIEW_H + 40) {
            eventManager.enqueue(new GameEvent(GameEvent.Type.PLAYER_FELL, 0));
            // Move back up now so the same fall cannot enqueue repeatedly.
            player.reset(respawnX, respawnY);
        }
        if (player.x >= level.finishX && !levelEventQueued) {
            levelEventQueued = true;
            eventManager.enqueue(new GameEvent(GameEvent.Type.LEVEL_COMPLETED, 0));
        }
        eventManager.process(this);
        int viewW = Math.max(getWidth(), 1);
        cameraX = Math.max(0, Math.min(level.worldWidth - viewW, player.x - viewW / 3));
    }

    void applyCoinEvent(int points) { score += points; coinsCollected++; }

    /** Hazard or fall: lose a life and go back to the last checkpoint (or the start). */
    void applyDamageEvent() {
        if (ended) return;
        lives--;
        if (lives <= 0) {
            ended = true;
            timer.stop();
            pendingDialog = this::showGameOverDialog;
        } else {
            player.reset(respawnX, respawnY);
            invincible = 90;
        }
    }

    void applyLevelCompleteEvent() {
        if (ended) return;
        ended = true;
        timer.stop();
        score += 500 + lives * 100;
        UserManager.getInstance().recordLevel(levelNumber, score);
        pendingDialog = this::showCompleteDialog;
    }

    private void loadLevel(int number) {
        level = new Level(number);
        player.setPhysics(level.gravity, level.jumpPower);
        respawnX = level.startX; respawnY = level.startY;
        checkpointIndex = -1; checkpointMsg = 0;
        player.reset(respawnX, respawnY);
        facingRight = true;
        invincible = 0;
        cameraX = 0; levelEventQueued = false;
        keys.clear(); eventManager.clear();
    }

    // ---- menu-style dialogs ----

    private void pauseGame() {
        paused = true;
        timer.stop();
        keys.clear();
        repaint();
        SwingUtilities.invokeLater(() -> {
            int choice = MenuDialog.show(frame, "PAUSED", Color.WHITE,
                new String[]{Level.worldName(levelNumber) + " " + Level.stageOf(levelNumber) + "/" + Level.LEVELS_PER_WORLD
                             + "   \u2022   Level " + levelNumber + " of " + Level.TOTAL_LEVELS + "   \u2022   Score " + score},
                new String[]{"RESUME", "RESTART LEVEL", "LEVEL SELECT", "MAIN MENU"});
            switch (choice) {
                case 1: startLevel(levelNumber); break;
                case 2: frame.showLevelSelect(); break;
                case 3: frame.showMenu(); break;
                default: // resume (also when closed with Esc)
                    paused = false; keys.clear(); timer.start(); requestFocusInWindow();
            }
        });
    }

    private void showCompleteDialog() {
        int remaining = Level.levelsRemaining(levelNumber);
        int choice;
        if (remaining > 0) {
            String next = Level.worldOf(levelNumber + 1) != Level.worldOf(levelNumber)
                ? "New world unlocked: " + Level.worldName(levelNumber + 1)
                : "Level " + (levelNumber + 1) + " unlocked";
            choice = MenuDialog.show(frame, "LEVEL " + levelNumber + " COMPLETE", Theme.GREEN,
                new String[]{"Score: " + String.format("%,d", score) + "   \u2022   Coins: " + coinsCollected, next},
                new String[]{"NEXT LEVEL", "PLAY AGAIN", "LEVEL SELECT", "MAIN MENU"});
            switch (choice) {
                case 0: startLevel(levelNumber + 1); break;
                case 1: startLevel(levelNumber); break;
                case 3: frame.showMenu(); break;
                default: frame.showLevelSelect();
            }
        } else {
            choice = MenuDialog.show(frame, "YOU WIN!", Theme.GOLD,
                new String[]{"Earth, Space and Moon cleared - all " + Level.TOTAL_LEVELS + " levels complete",
                             "Total score: " + String.format("%,d", UserManager.getInstance().getTotal())},
                new String[]{"LEVEL SELECT", "LEADERBOARD", "MAIN MENU"});
            switch (choice) {
                case 1: frame.showScores(); break;
                case 2: frame.showMenu(); break;
                default: frame.showLevelSelect();
            }
        }
    }

    private void showGameOverDialog() {
        int choice = MenuDialog.show(frame, "GAME OVER", Theme.RED,
            new String[]{"Level " + levelNumber + "   \u2022   Score " + String.format("%,d", score),
                         "Finish the level to save your score"},
            new String[]{"TRY AGAIN", "LEVEL SELECT", "MAIN MENU"});
        switch (choice) {
            case 0: startLevel(levelNumber); break;
            case 2: frame.showMenu(); break;
            default: frame.showLevelSelect();
        }
    }

    // ---- drawing ----

    @Override protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        int world = level == null ? Level.EARTH : level.world;
        drawBackground(g2, world);
        if (level != null) {
            g2.translate(-cameraX, 0);
            for (Platform p : level.platforms) drawPlatform(g2, p, world);
            for (int i = 0; i < level.checkpoints.length; i++) drawCheckpoint(g2, level.checkpoints[i], i <= checkpointIndex);
            for (Coin c : level.coins) if (!c.collected) drawCoin(g2, c);
            for (Obstacle o : level.obstacles) drawObstacle(g2, o);
            drawFinish(g2, level.finishX, 480);
            drawPlayer(g2, world);
            g2.translate(cameraX, 0);
        }
        drawHud(g2);
        if (paused || ended) { g2.setColor(new Color(0, 0, 0, 110)); g2.fillRect(0, 0, getWidth(), getHeight()); }
        g2.dispose();
    }

    private void drawBackground(Graphics2D g, int world) {
        if (world == Level.SPACE) drawSpaceBackground(g);
        else if (world == Level.MOON) drawMoonBackground(g);
        else drawEarthBackground(g);
    }

    private void drawStars(Graphics2D g, int count, int parallax, int maxY) {
        int w = Math.max(1, getWidth());
        for (int i = 0; i < count; i++) {
            int sx = Math.floorMod(i * 193 + 71 - cameraX / (parallax + i % 4), w);
            int sy = (i * 89 + 17) % maxY;
            int size = 1 + (i % 7 == 0 ? 2 : (i % 3 == 0 ? 1 : 0));
            int a = 150 + (int) (100 * Math.sin((frameCount + i * 13) * 0.07));
            g.setColor(new Color(255, 255, 255, Math.max(40, Math.min(255, a))));
            g.fillOval(sx, sy, size, size);
        }
    }

    private void drawEarthBackground(Graphics2D g) {
        // Desert sky: blue at the top fading to a warm haze at the horizon
        g.setPaint(new GradientPaint(0,0,new Color(86,164,232),0,getHeight(),new Color(255,228,176)));
        g.fillRect(0,0,getWidth(),getHeight());
        // Sun with soft halo
        g.setColor(new Color(255,236,153,45)); g.fillOval(715,8,165,165);
        g.setColor(new Color(255,244,180)); g.fillOval(762,54,70,70);
        // Far mesas (slow parallax)
        g.setColor(new Color(214,150,112));
        for (int i = 0; i < 6; i++) {
            int x = i * 330 - (cameraX / 6) % 330;
            g.fillRoundRect(x + 40, 330, 150, 200, 14, 14);
            g.fillRoundRect(x + 190, 365, 90, 170, 12, 12);
        }
        // Rolling sand dunes, far then near
        g.setColor(new Color(236,190,122));
        for (int i = 0; i < 8; i++) { int x = i * 270 - (cameraX / 4) % 270; g.fillOval(x, 380, 360, 210); }
        g.setColor(new Color(224,172,102));
        for (int i = 0; i < 7; i++) { int x = i * 330 - (cameraX / 2) % 330; g.fillOval(x, 430, 390, 155); }
        // Sandy ground
        g.setPaint(new GradientPaint(0,500,new Color(232,186,114),0,getHeight(),new Color(190,134,76)));
        g.fillRect(0,510,getWidth(),Math.max(0,getHeight()-510));
        g.setColor(new Color(250,222,160)); g.fillRect(0,510,getWidth(),5);
    }

    private void drawSpaceBackground(Graphics2D g) {
        g.setPaint(new GradientPaint(0,0,new Color(5,6,28),0,getHeight(),new Color(38,22,84)));
        g.fillRect(0,0,getWidth(),getHeight());
        // Nebula glows
        g.setColor(new Color(150,70,220,34)); g.fillOval(120-cameraX/12, 90, 420, 230);
        g.setColor(new Color(60,120,255,30)); g.fillOval(520-cameraX/9, 210, 460, 250);
        g.setColor(new Color(255,90,160,22)); g.fillOval(300-cameraX/10, 330, 380, 200);
        drawStars(g, 90, 3, 480);
        // Ringed planet
        int px = Math.floorMod(640 - cameraX / 8, getWidth() + 300) - 150, py = 110;
        g.setPaint(new GradientPaint(px,py,new Color(255,180,110),px+110,py+110,new Color(150,70,60)));
        g.fillOval(px,py,110,110);
        g.setColor(new Color(255,230,190,150)); g.setStroke(new BasicStroke(5f));
        g.drawOval(px-40,py+38,190,34);
        g.setStroke(new BasicStroke(1f));
        // Small distant planet
        int qx = Math.floorMod(180 - cameraX / 5, getWidth() + 200) - 100;
        g.setPaint(new GradientPaint(qx,330,new Color(110,220,255),qx+46,376,new Color(30,90,160)));
        g.fillOval(qx,330,46,46);
    }

    private void drawMoonBackground(Graphics2D g) {
        g.setPaint(new GradientPaint(0,0,new Color(3,4,14),0,getHeight(),new Color(26,28,52)));
        g.fillRect(0,0,getWidth(),getHeight());
        drawStars(g, 70, 4, 330);
        // Earth hanging in the sky
        int ex = getWidth() - 190, ey = 62;
        g.setColor(new Color(110,190,255,40)); g.fillOval(ex-10,ey-10,110,110);
        g.setPaint(new GradientPaint(ex,ey,new Color(80,160,255),ex+90,ey+90,new Color(20,70,160)));
        g.fillOval(ex,ey,90,90);
        g.setColor(new Color(70,190,110)); g.fillOval(ex+16,ey+22,30,22); g.fillOval(ex+50,ey+46,26,24);
        g.setColor(new Color(255,255,255,150)); g.fillOval(ex+8,ey+10,34,10); g.fillOval(ex+44,ey+30,38,9);
        // Grey crater hills (parallax)
        g.setColor(new Color(88,92,110));
        for(int i=0;i<8;i++){ int x=i*270-(cameraX/4)%270; g.fillOval(x,350,360,220); }
        g.setColor(new Color(64,68,86));
        for(int i=0;i<7;i++){ int x=i*330-(cameraX/2)%330; g.fillOval(x,418,390,150); }
        g.setColor(new Color(45,48,64));
        for(int i=0;i<10;i++){ int x=i*210-(cameraX/2)%210+40; g.fillOval(x,452+(i%3)*14,70,16); }
        g.setPaint(new GradientPaint(0,500,new Color(120,122,136),0,getHeight(),new Color(52,54,70)));
        g.fillRect(0,510,getWidth(),Math.max(0,getHeight()-510));
        g.setColor(new Color(190,192,205)); g.fillRect(0,510,getWidth(),4);
    }

    private void drawPlatform(Graphics2D g, Platform p, int world) {
        if (world == Level.SPACE) { drawSpacePlatform(g, p); return; }
        if (world == Level.MOON) { drawMoonPlatform(g, p); return; }
        // Desert platform: sandstone block with a sand-coloured top
        g.setColor(new Color(90,55,30,70)); g.fillRoundRect(p.x+3,p.y+5,p.width,p.height,10,10);
        g.setPaint(new GradientPaint(p.x,p.y,new Color(196,138,84),p.x,p.y+p.height,new Color(128,82,56)));
        g.fillRoundRect(p.x,p.y,p.width,p.height,9,9);
        g.setColor(new Color(110,70,44,120));                       // sandstone layers
        for (int yy = p.y + 14; yy < p.y + p.height - 2; yy += 7) g.drawLine(p.x + 6, yy, p.x + p.width - 6, yy);
        g.setPaint(new GradientPaint(p.x,p.y,new Color(250,224,152),p.x,p.y+9,new Color(228,184,106)));
        g.fillRoundRect(p.x,p.y,p.width,9,8,8);
        g.setColor(new Color(255,240,190,190)); g.drawLine(p.x+7,p.y+2,p.x+p.width-8,p.y+2);
    }

    private void drawSpacePlatform(Graphics2D g, Platform p) {
        g.setColor(new Color(0,0,0,70)); g.fillRoundRect(p.x+3,p.y+5,p.width,p.height,10,10);
        g.setPaint(new GradientPaint(p.x,p.y,new Color(70,80,112),p.x,p.y+p.height,new Color(28,32,56)));
        g.fillRoundRect(p.x,p.y,p.width,p.height,9,9);
        g.setColor(new Color(15,18,36,150));
        for (int x = p.x + 30; x < p.x + p.width - 10; x += 40) g.drawLine(x, p.y + 9, x, p.y + p.height - 3);
        g.setPaint(new GradientPaint(p.x,p.y,new Color(120,240,255),p.x,p.y+7,new Color(35,150,215)));
        g.fillRoundRect(p.x,p.y,p.width,7,7,7);
        g.setColor(new Color(90,230,255,60)); g.fillRoundRect(p.x-2,p.y-2,p.width+4,12,10,10);
        g.setColor(new Color(255,255,255,170)); g.drawLine(p.x+7,p.y+1,p.x+p.width-8,p.y+1);
    }

    private void drawMoonPlatform(Graphics2D g, Platform p) {
        g.setColor(new Color(0,0,0,80)); g.fillRoundRect(p.x+3,p.y+5,p.width,p.height,10,10);
        g.setPaint(new GradientPaint(p.x,p.y,new Color(150,152,165),p.x,p.y+p.height,new Color(76,78,94)));
        g.fillRoundRect(p.x,p.y,p.width,p.height,9,9);
        g.setColor(new Color(60,62,78,170));
        for (int i = 0; i < 3; i++) {
            int cx = p.x + 14 + Math.floorMod(p.x * 7 + i * 61, Math.max(20, p.width - 40));
            int cy = p.y + 12 + (i % 2) * 5;
            if (cy + 6 < p.y + p.height) g.fillOval(cx, cy, 14 + i * 4, 6);
        }
        g.setPaint(new GradientPaint(p.x,p.y,new Color(222,224,232),p.x,p.y+8,new Color(172,174,188)));
        g.fillRoundRect(p.x,p.y,p.width,8,8,8);
        g.setColor(new Color(255,255,255,170)); g.drawLine(p.x+7,p.y+2,p.x+p.width-8,p.y+2);
    }

    private void drawCheckpoint(Graphics2D g, int[] cp, boolean reached) {
        int fx = cp[0] + 24, fy = cp[1] + 40; // pole base on the platform surface
        g.setColor(new Color(210,215,230)); g.fillRect(fx, fy - 56, 3, 56);
        g.setColor(reached ? new Color(80,235,140) : new Color(120,130,160));
        g.fillPolygon(new int[]{fx+3, fx+30, fx+3}, new int[]{fy-56, fy-46, fy-36}, 3);
        if (reached) { g.setColor(new Color(80,235,140,60)); g.fillOval(fx-10, fy-66, 50, 40); }
    }

    private void drawCoin(Graphics2D g, Coin c) {
        g.setColor(new Color(255,205,48,55)); g.fillOval(c.x-5,c.y-5,c.size+10,c.size+10);
        g.setPaint(new GradientPaint(c.x,c.y,new Color(255,249,151),c.x+c.size,c.y+c.size,new Color(225,142,10)));
        g.fillOval(c.x,c.y,c.size,c.size);
        g.setColor(new Color(255,255,213)); g.drawOval(c.x+3,c.y+3,c.size-6,c.size-6);
        g.setColor(new Color(255,255,224)); g.drawLine(c.x+7,c.y+4,c.x+7,c.y+c.size-5);
    }

    private void drawObstacle(Graphics2D g, Obstacle o) {
        switch (o.kind) {
            case SPIKE:    drawSpike(g, o); break;
            case CACTUS:   drawCactus(g, o); break;
            case BIRD:     drawBird(g, o); break;
            case ASTEROID: drawAsteroid(g, o); break;
            case LASER:    drawLaser(g, o); break;
            case UFO:      drawUfo(g, o); break;
            case CRYSTAL:  drawCrystal(g, o); break;
            case METEOR:   drawMeteor(g, o); break;
            case ALIEN:    drawAlien(g, o); break;
            default: break;
        }
    }

    private void drawSpike(Graphics2D g, Obstacle o) {
        g.setColor(new Color(235,65,73));
        int[] xs={o.x,o.x+o.width/2,o.x+o.width}; int[] ys={o.y+o.height,o.y,o.y+o.height};
        g.fillPolygon(xs,ys,3);
        g.setColor(new Color(145,25,45)); g.drawPolygon(xs,ys,3);
    }

    private void drawCactus(Graphics2D g, Obstacle o) {
        int cx = o.x + o.width / 2;
        Color body = new Color(52,160,84), dark = new Color(24,98,52);
        g.setColor(body);
        g.fillRoundRect(cx - 6, o.y, 12, o.height, 10, 10);
        g.fillRect(o.x, o.y + 14, cx - o.x, 6);          // left arm
        g.fillRoundRect(o.x, o.y + 6, 6, 14, 6, 6);
        g.fillRect(cx, o.y + 22, o.x + o.width - cx, 6); // right arm
        g.fillRoundRect(o.x + o.width - 6, o.y + 14, 6, 14, 6, 6);
        g.setColor(new Color(120,215,140)); g.fillRect(cx - 3, o.y + 3, 2, o.height - 6);
        g.setColor(dark);
        g.drawRoundRect(cx - 6, o.y, 12, o.height, 10, 10);
        g.setColor(new Color(255,245,200));
        for (int yy = o.y + 8; yy < o.y + o.height - 4; yy += 9) { g.drawLine(cx + 6, yy, cx + 9, yy - 2); g.drawLine(cx - 6, yy + 3, cx - 9, yy + 1); }
    }

    private void drawBird(Graphics2D g, Obstacle o) {
        int dir = o.facingRight ? 1 : -1;
        int cx = o.x + o.width / 2, cy = o.y + o.height / 2;
        double flap = Math.sin(o.animTick() * 0.35);
        int wingY = cy - 4 - (int) (flap * 12);
        g.setColor(new Color(40,36,60));
        g.fillPolygon(new int[]{cx - 12, cx + 4, cx - 2}, new int[]{cy - 2, cy - 2, wingY - 8}, 3); // wing
        g.fillOval(o.x + 4, o.y + 3, o.width - 12, o.height - 5);                                   // body
        int hx = dir > 0 ? o.x + o.width - 14 : o.x + 2;
        g.fillOval(hx, o.y, 12, 12);                                                                  // head
        g.setColor(new Color(255,190,50));
        int bx = dir > 0 ? o.x + o.width - 3 : o.x + 3;
        g.fillPolygon(new int[]{bx, bx + dir * 8, bx}, new int[]{o.y + 4, o.y + 7, o.y + 10}, 3);   // beak
        g.setColor(new Color(255,70,70)); g.fillOval(hx + (dir > 0 ? 6 : 3), o.y + 3, 3, 3);          // eye
    }

    private void drawAsteroid(Graphics2D g, Obstacle o) {
        AffineTransform old = g.getTransform();
        double cx = o.x + o.width / 2.0, cy = o.y + o.height / 2.0;
        g.rotate(o.animTick() * 0.04, cx, cy);
        g.setColor(new Color(255,150,60,45)); g.fill(new Ellipse2D.Double(o.x - 4, o.y - 4, o.width + 8, o.height + 8));
        g.setPaint(new GradientPaint(o.x, o.y, new Color(150,138,128), o.x + o.width, o.y + o.height, new Color(70,62,66)));
        g.fillOval(o.x, o.y, o.width, o.height);
        g.setColor(new Color(55,50,56)); g.fillOval(o.x + 6, o.y + 7, 8, 7); g.fillOval(o.x + 17, o.y + 15, 7, 6); g.fillOval(o.x + 10, o.y + 19, 5, 4);
        g.setColor(new Color(35,30,38)); g.drawOval(o.x, o.y, o.width, o.height);
        g.setTransform(old);
    }

    private void drawLaser(Graphics2D g, Obstacle o) {
        int state = o.laserState();
        int cx = o.x + o.width / 2;
        // emitters at the bottom and top of the beam
        g.setColor(new Color(70,76,104)); g.fillRoundRect(o.x - 6, o.y + o.height - 7, o.width + 12, 7, 4, 4);
        g.fillRoundRect(o.x - 6, o.y - 4, o.width + 12, 7, 4, 4);
        if (state == 2) {
            g.setColor(new Color(255,40,90,60)); g.fillRect(o.x - 6, o.y, o.width + 12, o.height);
            g.setColor(new Color(255,70,110,150)); g.fillRect(o.x - 2, o.y, o.width + 4, o.height);
            g.setColor(new Color(255,230,235)); g.fillRect(cx - 2, o.y, 4, o.height);
        } else if (state == 1) {
            if ((o.animTick() / 3) % 2 == 0) { g.setColor(new Color(255,90,120,140)); g.fillRect(cx - 1, o.y, 2, o.height); }
        } else {
            g.setColor(new Color(255,90,120,45)); g.fillRect(cx, o.y, 1, o.height);
        }
        g.setColor(state == 2 ? new Color(255,80,110) : new Color(110,120,150));
        g.fillOval(cx - 3, o.y - 2, 6, 4); g.fillOval(cx - 3, o.y + o.height - 5, 6, 4);
    }

    private void drawUfo(Graphics2D g, Obstacle o) {
        int cx = o.x + o.width / 2;
        g.setColor(new Color(120,255,200,35)); g.fillOval(o.x - 5, o.y - 4, o.width + 10, o.height + 10);
        g.setColor(new Color(150,240,255,190)); g.fillOval(cx - 10, o.y - 6, 20, 16);            // dome
        g.setPaint(new GradientPaint(o.x, o.y, new Color(200,205,225), o.x, o.y + o.height, new Color(90,96,130)));
        g.fillOval(o.x, o.y + 4, o.width, o.height - 6);                                           // saucer
        g.setColor(new Color(50,55,85)); g.drawOval(o.x, o.y + 4, o.width, o.height - 6);
        Color[] lights = {new Color(255,90,90), new Color(255,230,90), new Color(90,255,150)};
        for (int i = 0; i < 3; i++) {
            g.setColor(((o.animTick() / 8) + i) % 3 == 0 ? Color.WHITE : lights[i]);
            g.fillOval(o.x + 9 + i * 13, o.y + 10, 5, 5);
        }
    }

    private void drawCrystal(Graphics2D g, Obstacle o) {
        Shape s = o.shape();
        g.setPaint(new GradientPaint(o.x, o.y, new Color(190,140,255), o.x + o.width, o.y + o.height, new Color(80,60,190)));
        g.fill(s);
        g.setColor(new Color(235,215,255,170));
        g.drawLine(o.x + o.width * 55 / 100, o.y + 2, o.x + o.width * 45 / 100, o.y + o.height - 2);
        g.drawLine(o.x + o.width * 55 / 100, o.y + 2, o.x + o.width * 68 / 100, o.y + o.height - 2);
        g.setColor(new Color(40,25,100)); g.draw(s);
        g.setColor(new Color(190,150,255,40)); g.fillOval(o.x - 4, o.y + o.height - 10, o.width + 8, 14);
    }

    private void drawMeteor(Graphics2D g, Obstacle o) {
        int cx = o.x + o.width / 2;
        int impact = o.meteorImpactY();
        if (o.meteorWarning()) {
            // Telegraph: dashed line from the sky and a flashing mark where it will land.
            if ((o.animTick() / 4) % 2 == 0) {
                g.setColor(new Color(255,70,60,150));
                g.fillOval(cx - 22, impact - 5, 44, 8);
                g.setColor(new Color(255,90,70,70));
                for (int yy = 0; yy < impact - 10; yy += 18) g.fillRect(cx - 1, yy, 2, 9);
            }
        }
        if (!o.isActive()) return;
        g.setPaint(new GradientPaint(cx, o.y - 70, new Color(255,120,30,0), cx, o.y + 10, new Color(255,170,60,170)));
        g.fillPolygon(new int[]{cx - 10, cx + 10, cx}, new int[]{o.y + 6, o.y + 6, o.y - 70}, 3);
        g.setColor(new Color(255,150,40,80)); g.fillOval(o.x - 6, o.y - 6, o.width + 12, o.height + 12);
        g.setPaint(new GradientPaint(o.x, o.y, new Color(190,110,70), o.x + o.width, o.y + o.height, new Color(80,40,35)));
        g.fillOval(o.x, o.y, o.width, o.height);
        g.setColor(new Color(60,30,30)); g.fillOval(o.x + 6, o.y + 6, 7, 6); g.fillOval(o.x + 14, o.y + 14, 6, 5);
        g.setColor(new Color(255,210,120)); g.drawOval(o.x, o.y, o.width, o.height);
    }

    private void drawAlien(Graphics2D g, Obstacle o) {
        int step = (o.animTick() / 8) % 2;
        g.setColor(new Color(40,120,70));
        for (int i = 0; i < 4; i++) { int lx = o.x + 4 + i * 7; g.fillRect(lx, o.y + o.height - 5, 3, 5 - (i + step) % 2 * 2); }
        g.setPaint(new GradientPaint(o.x, o.y, new Color(150,255,120), o.x, o.y + o.height, new Color(50,170,90)));
        g.fillOval(o.x, o.y + 4, o.width, o.height - 6);
        g.setColor(new Color(30,110,60)); g.drawOval(o.x, o.y + 4, o.width, o.height - 6);
        int dir = o.facingRight ? 1 : -1;
        int ex = o.x + o.width / 2 + dir * 4;
        g.setColor(new Color(50,170,90));
        g.drawLine(ex - 6, o.y + 6, ex - 8, o.y - 1); g.drawLine(ex + 6, o.y + 6, ex + 8, o.y - 1);
        g.setColor(Color.WHITE); g.fillOval(ex - 11, o.y - 5, 7, 7); g.fillOval(ex + 4, o.y - 5, 7, 7);
        g.setColor(Color.BLACK); g.fillOval(ex - 9 + dir * 2, o.y - 3, 3, 3); g.fillOval(ex + 6 + dir * 2, o.y - 3, 3, 3);
    }

    private void drawFinish(Graphics2D g,int x,int y) {
        g.setColor(new Color(75,61,50)); g.fillRect(x,y-100,5,100);
        g.setColor(new Color(44,205,103)); int[] xs={x+5,x+65,x+5}; int[] ys={y-100,y-80,y-60};
        g.fillPolygon(xs,ys,3);
    }

    private static void limb(Graphics2D g, int x1, int y1, int x2, int y2, int w, Color c) {
        g.setStroke(new BasicStroke(w, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(c);
        g.drawLine(x1, y1, x2, y2);
        g.setStroke(new BasicStroke(1f));
    }

    /**
     * Draws the player with an outfit that matches the world:
     * Earth = casual hiker (cap, hoodie, jeans), Space = astronaut suit with glass helmet,
     * Moon = heavy white EVA suit with a gold visor and life-support pack.
     * Drawn in local coordinates (x -14..14, y 0..36) facing right, then mirrored when facing left.
     */
    private void drawPlayer(Graphics2D g, int world) {
        if (invincible > 0 && (invincible / 6) % 2 == 0) return; // blink while protected
        AffineTransform old = g.getTransform();
        g.translate(player.x + 14, player.y);
        if (!facingRight) g.scale(-1, 1);

        boolean air = !player.onGround;
        boolean moving = Math.abs(player.vx) > 0.1 && !air;
        int swing = moving ? (int) Math.round(Math.sin(frameCount * 0.45) * 6) : 0;
        int frontLeg = air ? 5 : swing, backLeg = air ? -5 : -swing;
        int frontArmX = air ? 7 : -swing, backArmX = air ? -7 : swing;
        int armEndY = air ? 15 : 27;

        // soft ground shadow
        if (!air) { g.setColor(new Color(0, 0, 0, 40)); g.fillOval(-10, 33, 22, 5); }

        if (world == Level.SPACE) drawAstronaut(g, frontLeg, backLeg, frontArmX, backArmX, armEndY);
        else if (world == Level.MOON) drawMoonWalker(g, frontLeg, backLeg, frontArmX, backArmX, armEndY);
        else drawHiker(g, frontLeg, backLeg, frontArmX, backArmX, armEndY);

        g.setTransform(old);
    }

    private void drawHiker(Graphics2D g, int fl, int bl, int fa, int ba, int ay) {
        Color jeans = new Color(52, 78, 135), jeansDark = new Color(36, 56, 104), boot = new Color(88, 58, 40);
        Color hoodie = new Color(232, 122, 52), hoodieDark = new Color(184, 86, 34), skin = new Color(246, 185, 135);
        limb(g, 0, 18, ba, ay, 5, hoodieDark);                                    // back arm
        limb(g, -3, 25, -3 + bl, 32, 6, jeansDark);                               // back leg
        g.setColor(new Color(64, 42, 30)); g.fillOval(-3 + bl - 4, 31, 9, 5);
        g.setColor(new Color(120, 82, 52)); g.fillRoundRect(-12, 16, 6, 11, 4, 4); // small backpack
        limb(g, 3, 25, 3 + fl, 32, 6, jeans);                                      // front leg
        g.setColor(boot); g.fillRoundRect(3 + fl - 3, 31, 10, 5, 4, 4);
        g.setPaint(new GradientPaint(0, 15, hoodie, 0, 28, hoodieDark));           // hoodie
        g.fillRoundRect(-8, 15, 16, 13, 6, 6);
        g.setColor(new Color(60, 40, 30)); g.fillRect(-8, 26, 16, 2);              // belt
        g.setColor(new Color(255, 255, 255, 120)); g.drawLine(3, 16, 3, 25);       // zipper
        g.setColor(hoodieDark); g.drawRoundRect(-5, 21, 7, 4, 2, 2);               // pocket
        g.setColor(skin); g.fillRect(-1, 13, 5, 4);                                // neck
        g.fillOval(-6, 1, 14, 15);                                                 // head
        g.setColor(new Color(224, 160, 112)); g.fillOval(-6, 7, 4, 5);             // ear
        g.setColor(new Color(60, 38, 28)); g.fillArc(-7, 0, 16, 12, 90, 120);      // hair at back
        g.setColor(new Color(34, 52, 100)); g.fillArc(-7, -2, 17, 14, 0, 180);     // cap dome
        g.fillRoundRect(3, 4, 10, 3, 3, 3);                                        // cap brim
        g.setColor(Color.WHITE); g.fillOval(3, 7, 4, 4);                           // eye
        g.setColor(new Color(30, 30, 40)); g.fillOval(5, 8, 2, 3);
        g.setColor(new Color(170, 90, 80)); g.drawLine(4, 13, 7, 13);              // mouth
        limb(g, 0, 18, fa, ay, 5, hoodie);                                         // front arm
        g.setColor(skin); g.fillOval(fa - 3, ay - 2, 6, 6);                        // hand
    }

    private void drawAstronaut(Graphics2D g, int fl, int bl, int fa, int ba, int ay) {
        Color suit = new Color(236, 240, 250), suitDark = new Color(160, 168, 196), accent = new Color(60, 200, 245);
        limb(g, 0, 18, ba, ay, 5, suitDark);                                       // back arm
        g.setColor(new Color(240, 140, 40)); g.fillOval(ba - 3, ay - 2, 6, 6);     // back glove
        limb(g, -3, 25, -3 + bl, 32, 6, suitDark);
        g.setColor(new Color(60, 66, 90)); g.fillRoundRect(-3 + bl - 4, 31, 10, 5, 4, 4);
        g.setPaint(new GradientPaint(-12, 0, new Color(96, 110, 160), -6, 0, new Color(60, 70, 112)));
        g.fillRoundRect(-13, 14, 7, 14, 4, 4);                                     // life-support pack
        g.setColor(accent); g.fillRect(-11, 17, 3, 2);
        limb(g, 3, 25, 3 + fl, 32, 6, suit);
        g.setColor(new Color(70, 78, 105)); g.fillRoundRect(3 + fl - 3, 31, 11, 5, 4, 4);
        g.setColor(accent); g.fillRect(3 + fl - 2, 34, 9, 1);                      // boot stripe
        g.setPaint(new GradientPaint(0, 15, suit, 0, 28, suitDark));
        g.fillRoundRect(-8, 15, 16, 13, 6, 6);                                     // torso
        g.setColor(new Color(40, 50, 80)); g.fillRoundRect(-3, 18, 9, 6, 2, 2);    // chest control box
        g.setColor(new Color(255, 80, 80)); g.fillRect(-2, 19, 2, 2);
        g.setColor(new Color(90, 255, 150)); g.fillRect(1, 19, 2, 2);
        g.setColor(accent); g.fillRect(4, 19, 2, 2);
        g.setColor(accent); g.fillRect(-8, 25, 16, 2);                             // waist band
        g.setColor(new Color(150, 156, 180)); g.fillRoundRect(-7, 14, 15, 4, 3, 3); // neck ring
        g.setColor(new Color(246, 185, 135)); g.fillOval(-4, 3, 11, 12);           // face inside helmet
        g.setColor(Color.WHITE); g.fillOval(3, 7, 3, 3);
        g.setColor(new Color(30, 30, 40)); g.fillOval(4, 8, 2, 2);
        g.setColor(new Color(60, 38, 28)); g.fillArc(-5, 2, 12, 8, 60, 140);       // hair
        g.setColor(new Color(150, 225, 255, 70)); g.fillOval(-10, -3, 23, 21);     // glass helmet
        g.setColor(new Color(220, 245, 255, 190)); g.setStroke(new BasicStroke(1.5f));
        g.drawOval(-10, -3, 23, 21);
        g.drawArc(-6, 0, 14, 14, 110, 50);                                         // glass glare
        g.setStroke(new BasicStroke(1f));
        limb(g, 0, 18, fa, ay, 5, suit);                                           // front arm
        g.setColor(new Color(240, 140, 40)); g.fillOval(fa - 3, ay - 2, 6, 6);     // glove
    }

    private void drawMoonWalker(Graphics2D g, int fl, int bl, int fa, int ba, int ay) {
        Color suit = new Color(244, 244, 248), shade = new Color(178, 180, 194), dark = new Color(96, 98, 112);
        limb(g, 0, 18, ba, ay, 6, shade);                                          // back arm
        g.setColor(dark); g.fillOval(ba - 3, ay - 2, 7, 7);
        limb(g, -3, 25, -3 + bl, 32, 7, shade);                                    // back leg
        g.setColor(dark); g.fillRoundRect(-3 + bl - 5, 31, 11, 5, 3, 3);
        g.setPaint(new GradientPaint(-14, 0, new Color(250, 250, 252), -6, 0, new Color(190, 192, 206)));
        g.fillRoundRect(-15, 11, 9, 18, 5, 5);                                     // big life-support pack
        g.setColor(new Color(120, 124, 140)); g.drawRoundRect(-15, 11, 9, 18, 5, 5);
        g.setColor(new Color(230, 80, 70)); g.fillRect(-12, 15, 4, 2);
        g.setColor(new Color(80, 150, 240)); g.fillRect(-12, 19, 4, 2);
        limb(g, 3, 25, 3 + fl, 32, 7, suit);                                       // front leg
        g.setColor(dark); g.fillRoundRect(3 + fl - 4, 31, 12, 5, 3, 3);            // chunky boot
        g.setColor(new Color(60, 62, 74)); g.fillRect(3 + fl - 4, 35, 12, 1);
        g.setPaint(new GradientPaint(0, 14, suit, 0, 28, shade));
        g.fillRoundRect(-9, 14, 18, 15, 7, 7);                                     // bulky torso
        g.setColor(new Color(210, 50, 55)); g.fillRect(-6, 17, 6, 2);              // flag patch
        g.setColor(Color.WHITE); g.fillRect(-6, 19, 6, 1);
        g.setColor(new Color(50, 80, 190)); g.fillRect(-6, 20, 6, 1);
        g.setColor(new Color(60, 62, 74)); g.fillRect(-9, 25, 18, 2);              // belt
        g.setColor(new Color(200, 202, 214)); g.fillRoundRect(-8, 11, 16, 5, 4, 4); // neck ring
        g.setPaint(new GradientPaint(-10, -3, Color.WHITE, 12, 17, new Color(200, 202, 216)));
        g.fillOval(-10, -4, 23, 22);                                               // helmet shell
        g.setColor(new Color(120, 124, 140)); g.drawOval(-10, -4, 23, 22);
        g.setPaint(new GradientPaint(0, 1, new Color(255, 226, 130), 8, 15, new Color(150, 98, 28)));
        g.fillOval(0, 1, 12, 13);                                                  // gold visor
        g.setColor(new Color(255, 255, 255, 190)); g.fillOval(2, 3, 4, 3);         // visor reflection
        g.setColor(new Color(110, 70, 20)); g.drawOval(0, 1, 12, 13);
        limb(g, -6, 14, -10, 17, 2, dark);                                         // hose to the pack
        limb(g, 0, 18, fa, ay, 6, suit);                                           // front arm
        g.setColor(new Color(70, 72, 86)); g.fillOval(fa - 3, ay - 2, 7, 7);       // glove
    }

    private void drawHud(Graphics2D g) {
        g.setColor(new Color(20,35,68,220)); g.fillRoundRect(15,12,400,84,12,12);
        g.setFont(new Font("Monospaced",Font.BOLD,16)); g.setColor(Color.WHITE);
        g.drawString("LEVEL "+levelNumber+"/"+Level.TOTAL_LEVELS+"   "+Level.worldName(levelNumber)+" "+Level.stageOf(levelNumber)+"/"+Level.LEVELS_PER_WORLD,28,37);
        g.drawString("SCORE "+score+"   COINS "+coinsCollected,28,60);
        g.drawString("LIVES "+lives,28,83);
        g.setFont(new Font("Monospaced",Font.PLAIN,12));
        String hint = "A/D or Arrows: move  |  Space: jump  |  P: pause";
        int hw = g.getFontMetrics().stringWidth(hint) + 24;
        g.setColor(new Color(20,35,68,190)); g.fillRoundRect(getWidth()-hw-15,12,hw,28,12,12);
        g.setColor(Color.WHITE); g.drawString(hint,getWidth()-hw-3,31);
        backButton.x = getWidth() - backButton.width - 15;
        g.setColor(backHover ? new Color(54,81,132,235) : new Color(20,35,68,205));
        g.fillRoundRect(backButton.x, backButton.y, backButton.width, backButton.height, 12, 12);
        g.setColor(new Color(108,153,221,150));
        g.drawRoundRect(backButton.x, backButton.y, backButton.width, backButton.height, 12, 12);
        g.setFont(new Font("Monospaced",Font.BOLD,13)); g.setColor(Color.WHITE);
        String bt = "BACK";
        g.drawString(bt, backButton.x + (backButton.width - g.getFontMetrics().stringWidth(bt)) / 2, backButton.y + 20);
        if (checkpointMsg > 0) {
            g.setFont(new Font("Monospaced",Font.BOLD,22));
            String msg = "CHECKPOINT!";
            int mw = g.getFontMetrics().stringWidth(msg);
            g.setColor(new Color(80,235,140, Math.min(255, checkpointMsg * 6)));
            g.drawString(msg, (getWidth() - mw) / 2, 130);
        }
    }
}
