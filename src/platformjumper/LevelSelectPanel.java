package platformjumper;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/** Level map: 15 tiles, only the levels you have unlocked can be played. */
public class LevelSelectPanel extends Theme.Screen {
    private final GameFrame frame;
    private final JLabel playerLabel = Theme.label(" ", 13, Font.PLAIN, Theme.MUTED);
    private final JPanel grid = new JPanel(new GridLayout(3, 5, 14, 14));

    public LevelSelectPanel(GameFrame frame) {
        super(new BorderLayout(0, 18));
        this.frame = frame;
        setBorder(new EmptyBorder(24, 34, 22, 34));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JPanel titles = new JPanel();
        titles.setOpaque(false);
        titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));
        titles.add(Theme.label("SELECT LEVEL", 34, Font.BOLD, Color.WHITE));
        titles.add(Box.createVerticalStrut(2));
        titles.add(playerLabel);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttons.setOpaque(false);
        JButton back = Theme.button("BACK", false, 150, 38);
        back.addActionListener(e -> frame.showMenu());
        buttons.add(back);
        JPanel buttonWrap = new JPanel(new GridBagLayout());
        buttonWrap.setOpaque(false);
        buttonWrap.add(buttons);
        header.add(titles, BorderLayout.WEST);
        header.add(buttonWrap, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        grid.setOpaque(false);
        add(grid, BorderLayout.CENTER);

        JLabel foot = Theme.label("Rows are worlds: Earth, Space, Moon. Finish a level to unlock the next. Hazards and falls cost a life; checkpoints save your spot.", 12, Font.PLAIN, new Color(106, 137, 192));
        foot.setHorizontalAlignment(SwingConstants.CENTER);
        add(foot, BorderLayout.SOUTH);
    }

    /** Rebuilds the tiles from player progress. */
    public void refresh() {
        ProgressManager pm = ProgressManager.getInstance();
        int unlocked = pm.getUnlocked();
        playerLabel.setText("Unlocked " + unlocked + " of " + Level.TOTAL_LEVELS
            + "   \u2022   Total score " + String.format("%,d", pm.getTotal()));
        grid.removeAll();
        for (int n = 1; n <= Level.TOTAL_LEVELS; n++) grid.add(new Tile(n, n <= unlocked, pm.getBest(n), n == unlocked));
        grid.revalidate();
        grid.repaint();
    }

    private class Tile extends JComponent {
        private final int number, best;
        private final boolean unlocked, current;
        private boolean hover;

        Tile(int number, boolean unlocked, int best, boolean current) {
            this.number = number; this.unlocked = unlocked; this.best = best; this.current = current;
            if (unlocked) {
                setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                setToolTipText("Play level " + number);
            } else {
                setToolTipText("Locked \u2013 finish level " + (number - 1) + " first");
            }
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
                @Override public void mouseExited(MouseEvent e) { hover = false; repaint(); }
                @Override public void mouseClicked(MouseEvent e) { if (Tile.this.unlocked) frame.startLevel(Tile.this.number); }
            });
        }

        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();
            Color top, bottom, edge;
            if (!unlocked) { top = new Color(20, 28, 55); bottom = new Color(14, 20, 42); edge = new Color(40, 54, 90); }
            else if (current) { top = hover ? new Color(89, 236, 255) : new Color(0, 211, 255); bottom = hover ? new Color(73, 127, 255) : new Color(55, 107, 255); edge = new Color(119, 245, 255, 200); }
            else {
                int wd = Level.worldOf(number);
                Color[] t = {new Color(38, 104, 84), new Color(70, 54, 138), new Color(92, 96, 114)};
                Color[] b = {new Color(22, 62, 58), new Color(38, 30, 84), new Color(54, 57, 72)};
                Color[] e = {new Color(110, 225, 160, 140), new Color(176, 146, 255, 140), new Color(215, 217, 230, 140)};
                top = hover ? t[wd].brighter() : t[wd]; bottom = hover ? b[wd].brighter() : b[wd]; edge = e[wd];
            }
            g.setPaint(new GradientPaint(0, 0, top, 0, h, bottom));
            g.fillRoundRect(0, 0, w, h, 18, 18);
            g.setColor(edge);
            g.drawRoundRect(0, 0, w - 1, h - 1, 18, 18);

            String num = String.valueOf(number);
            g.setFont(new Font("SansSerif", Font.BOLD, 34));
            FontMetrics fm = g.getFontMetrics();
            g.setColor(unlocked ? Color.WHITE : new Color(80, 96, 138));
            g.drawString(num, (w - fm.stringWidth(num)) / 2, h / 2 - 2);

            g.setFont(new Font("SansSerif", Font.BOLD, 10));
            fm = g.getFontMetrics();
            String diff = Level.worldName(number) + " " + Level.stageOf(number);
            g.setColor(unlocked ? (current ? new Color(10, 20, 60) : Theme.CYAN) : new Color(70, 86, 128));
            g.drawString(diff, (w - fm.stringWidth(diff)) / 2, 20);

            String bottomText;
            Color bottomColor;
            if (!unlocked) { bottomText = "LOCKED"; bottomColor = new Color(80, 96, 138); }
            else if (best > 0) { bottomText = "\u2713 " + String.format("%,d", best); bottomColor = Theme.GOLD; }
            else { bottomText = "PLAY"; bottomColor = current ? Color.WHITE : Theme.MUTED; }
            g.setFont(new Font("SansSerif", Font.BOLD, 12));
            fm = g.getFontMetrics();
            g.setColor(bottomColor);
            g.drawString(bottomText, (w - fm.stringWidth(bottomText)) / 2, h - 14);

            if (!unlocked) { // small padlock
                int lx = w - 26, ly = 10;
                g.setColor(new Color(80, 96, 138));
                g.setStroke(new BasicStroke(2f));
                g.drawArc(lx + 2, ly - 2, 8, 9, 0, 180);
                g.fillRoundRect(lx, ly + 3, 12, 9, 3, 3);
            }
            g.dispose();
        }
    }
}
