package platformjumper;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

/** Themed "How to Play" dialog that matches the neon arcade menu. */
public class HowToPlayDialog extends JDialog {

    public HowToPlayDialog(JFrame owner) {
        super(owner, "How to Play", true);
        JPanel root = new JPanel(new BorderLayout(0, 18)) {
            @Override protected void paintComponent(Graphics graphics) {
                super.paintComponent(graphics);
                Graphics2D g = (Graphics2D) graphics.create();
                g.setPaint(new GradientPaint(0, 0, new Color(8, 13, 39), getWidth(), getHeight(), new Color(26, 31, 83)));
                g.fillRect(0, 0, getWidth(), getHeight());
                g.setColor(new Color(65, 107, 195, 20));
                for (int x = 0; x < getWidth(); x += 34) g.drawLine(x, 0, x, getHeight());
                for (int y = 0; y < getHeight(); y += 34) g.drawLine(0, y, getWidth(), y);
                g.dispose();
            }
        };
        root.setBorder(new EmptyBorder(26, 34, 24, 34));

        // Title
        JPanel head = new JPanel();
        head.setOpaque(false);
        head.setLayout(new BoxLayout(head, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("HOW TO PLAY");
        title.setFont(new Font("SansSerif", Font.BOLD, 30));
        title.setForeground(Color.WHITE);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel sub = new JLabel("Clear all 15 levels, one after another");
        sub.setFont(new Font("SansSerif", Font.PLAIN, 13));
        sub.setForeground(new Color(157, 178, 216));
        sub.setAlignmentX(Component.CENTER_ALIGNMENT);
        head.add(title);
        head.add(Box.createVerticalStrut(4));
        head.add(sub);
        root.add(head, BorderLayout.NORTH);

        // Controls
        JPanel rows = new JPanel();
        rows.setOpaque(false);
        rows.setLayout(new BoxLayout(rows, BoxLayout.Y_AXIS));
        rows.add(row("MOVE", new String[]{"A", "D"}, new String[]{"\u2190", "\u2192"}));
        rows.add(Box.createVerticalStrut(10));
        rows.add(row("JUMP", new String[]{"SPACE"}, new String[]{"\u2191"}));
        rows.add(Box.createVerticalStrut(10));
        rows.add(row("PAUSE", new String[]{"P"}, new String[]{"ESC"}));
        rows.add(Box.createVerticalStrut(18));
        rows.add(goalCard());

        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(new Color(17, 27, 59, 235));
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(66, 94, 153), 1), new EmptyBorder(18, 22, 18, 22)));
        card.add(rows);
        root.add(card, BorderLayout.CENTER);

        // Button
        JButton ok = makeButton("GOT IT");
        JPanel south = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        south.setOpaque(false);
        south.add(ok);
        root.add(south, BorderLayout.SOUTH);

        ok.addActionListener(e -> dispose());
        getRootPane().setDefaultButton(ok);
        getRootPane().registerKeyboardAction(e -> dispose(),
            KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);

        setContentPane(root);
        setResizable(false);
        pack();
        setLocationRelativeTo(owner);
    }

    private JPanel row(String action, String[] keysA, String[] keysB) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        p.setOpaque(false);
        JLabel l = new JLabel(action);
        l.setFont(new Font("SansSerif", Font.BOLD, 13));
        l.setForeground(new Color(120, 223, 246));
        l.setPreferredSize(new Dimension(72, 32));
        p.add(l);
        for (String k : keysA) p.add(new KeyChip(k));
        JLabel or = new JLabel("or");
        or.setFont(new Font("SansSerif", Font.PLAIN, 12));
        or.setForeground(new Color(140, 160, 200));
        p.add(or);
        for (String k : keysB) p.add(new KeyChip(k));
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        return p;
    }

    private JPanel goalCard() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(new Color(8, 16, 39));
        p.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(64, 94, 150)), new EmptyBorder(12, 14, 12, 14)));
        p.add(line("\u2605  Collect coins for 100 points each", new Color(255, 207, 77)));
        p.add(Box.createVerticalStrut(5));
        p.add(line("\u25B2  Hazards and gaps cost a life; flags save your spot", new Color(255, 120, 125)));
        p.add(Box.createVerticalStrut(5));
        p.add(line("\u2691  Reach the green flag to unlock the next level", new Color(90, 225, 140)));
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        return p;
    }

    private JLabel line(String text, Color c) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("SansSerif", Font.PLAIN, 13));
        l.setForeground(c);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    private JButton makeButton(String text) {
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
                Color top = active ? new Color(89, 236, 255) : new Color(0, 211, 255);
                Color bottom = active ? new Color(73, 127, 255) : new Color(55, 107, 255);
                g.setPaint(new GradientPaint(0, 0, top, 0, getHeight(), bottom));
                g.fillRoundRect(0, 0, getWidth(), getHeight(), 18, 18);
                g.setColor(new Color(119, 245, 255, 180));
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
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        Dimension d = new Dimension(180, 42);
        b.setPreferredSize(d);
        return b;
    }

    /** Small keyboard-key shaped label. */
    private static class KeyChip extends JComponent {
        private final String text;
        private final Font font = new Font("SansSerif", Font.BOLD, 13);
        KeyChip(String text) {
            this.text = text;
            FontMetrics fm = getFontMetrics(font);
            setPreferredSize(new Dimension(Math.max(34, fm.stringWidth(text) + 22), 32));
        }
        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();
            g.setColor(new Color(10, 18, 45));
            g.fillRoundRect(0, 3, w, h - 3, 9, 9);
            g.setPaint(new GradientPaint(0, 0, new Color(54, 81, 132), 0, h - 4, new Color(36, 58, 100)));
            g.fillRoundRect(0, 0, w, h - 4, 9, 9);
            g.setColor(new Color(108, 153, 221, 140));
            g.drawRoundRect(0, 0, w - 1, h - 5, 9, 9);
            g.setFont(font);
            g.setColor(Color.WHITE);
            FontMetrics fm = g.getFontMetrics();
            g.drawString(text, (w - fm.stringWidth(text)) / 2, (h - 4 + fm.getAscent() - fm.getDescent()) / 2);
            g.dispose();
        }
    }
}
