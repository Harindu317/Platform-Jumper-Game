package platformjumper;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/** Shared look for every menu-style screen and dialog. */
public final class Theme {
    private Theme() {}

    public static final Color CYAN   = new Color(120, 223, 246);
    public static final Color GOLD   = new Color(255, 207, 77);
    public static final Color RED    = new Color(255, 120, 125);
    public static final Color GREEN  = new Color(90, 225, 140);
    public static final Color MUTED  = new Color(157, 178, 216);
    public static final Color CARD   = new Color(17, 27, 59);
    public static final Color BORDER = new Color(66, 94, 153);

    /** Dark gradient with a faint grid, used behind every menu screen. */
    public static void paintBackground(Graphics graphics, int w, int h) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setPaint(new GradientPaint(0, 0, new Color(8, 13, 39), w, h, new Color(26, 31, 83)));
        g.fillRect(0, 0, w, h);
        g.setColor(new Color(65, 107, 195, 20));
        for (int x = 0; x < w; x += 34) g.drawLine(x, 0, x, h);
        for (int y = 0; y < h; y += 34) g.drawLine(0, y, w, y);
        g.dispose();
    }

    /** Rounded gradient button (primary = cyan, otherwise dark blue). */
    public static JButton button(String text, boolean primary, int width, int height) {
        JButton b = new JButton(text) {
            private boolean hover;
            { addMouseListener(new java.awt.event.MouseAdapter() {
                @Override public void mouseEntered(java.awt.event.MouseEvent e) { hover = true; repaint(); }
                @Override public void mouseExited(java.awt.event.MouseEvent e) { hover = false; repaint(); }
            }); }
            @Override protected void paintComponent(Graphics graphics) {
                Graphics2D g = (Graphics2D) graphics.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                boolean active = getModel().isPressed() || hover;
                Color top = primary ? new Color(0, 211, 255) : new Color(36, 58, 100);
                Color bottom = primary ? new Color(55, 107, 255) : new Color(24, 38, 70);
                if (active) {
                    top = primary ? new Color(89, 236, 255) : new Color(54, 81, 132);
                    bottom = primary ? new Color(73, 127, 255) : new Color(35, 55, 94);
                }
                g.setPaint(new GradientPaint(0, 0, top, 0, getHeight(), bottom));
                g.fillRoundRect(0, 0, getWidth(), getHeight(), 18, 18);
                g.setColor(primary ? new Color(119, 245, 255, 180) : new Color(108, 153, 221, 100));
                g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 18, 18);
                g.dispose();
                super.paintComponent(graphics);
            }
        };
        b.setFont(new Font("SansSerif", Font.BOLD, 14));
        b.setForeground(Color.WHITE);
        b.setContentAreaFilled(false);
        b.setOpaque(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setHorizontalAlignment(SwingConstants.CENTER);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        Dimension d = new Dimension(width, height);
        b.setPreferredSize(d);
        b.setMinimumSize(d);
        b.setMaximumSize(d);
        return b;
    }

    public static void styleField(JTextField f) {
        f.setFont(new Font("SansSerif", Font.PLAIN, 15));
        f.setForeground(Color.WHITE);
        f.setCaretColor(Color.WHITE);
        f.setBackground(new Color(8, 16, 39));
        f.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(64, 94, 150)), new EmptyBorder(0, 12, 0, 12)));
        f.setPreferredSize(new Dimension(300, 40));
        f.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        f.setAlignmentX(Component.LEFT_ALIGNMENT);
    }

    public static JLabel label(String text, int size, int style, Color color) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("SansSerif", style, size));
        l.setForeground(color);
        return l;
    }

    /** Panel that paints the shared menu background. */
    public static class Screen extends JPanel {
        public Screen(LayoutManager lm) { super(lm); }
        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            paintBackground(g, getWidth(), getHeight());
        }
    }
}
