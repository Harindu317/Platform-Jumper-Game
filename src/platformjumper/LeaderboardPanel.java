package platformjumper;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;

/** Arcade leaderboard screen: podium, live search, and a readable score table. */
public class LeaderboardPanel extends JPanel {
    private static final Color GOLD   = new Color(255, 197, 63);
    private static final Color SILVER = new Color(191, 211, 239);
    private static final Color BRONZE = new Color(211, 143, 100);
    private static final Color CARD   = new Color(17, 27, 59);
    private static final Color ROW_A  = new Color(17, 27, 59);
    private static final Color ROW_B  = new Color(21, 33, 70);

    private final GameFrame frame;
    private final JTextField searchField = new JTextField() {
        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (getText().isEmpty()) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2.setColor(new Color(110, 134, 182));
                g2.setFont(getFont());
                Insets in = getInsets();
                g2.drawString("Search player name or score...", in.left, (getHeight() + g2.getFontMetrics().getAscent() - g2.getFontMetrics().getDescent()) / 2);
                g2.dispose();
            }
        }
    };
    private final DefaultTableModel model = new DefaultTableModel(new Object[]{"RANK", "PLAYER", "SCORE", "STATUS"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable table = new JTable(model);
    private final JPanel podium = new JPanel(new GridLayout(1, 3, 14, 0));
    private final JLabel resultLabel = new JLabel("ALL-TIME LEGENDS");

    public LeaderboardPanel(GameFrame frame) {
        this.frame = frame;
        setLayout(new BorderLayout());
        setBackground(new Color(8, 13, 35));

        JPanel root = new JPanel(new BorderLayout(0, 16));
        root.setOpaque(false);
        root.setBorder(new EmptyBorder(22, 34, 20, 34));
        add(root);

        // ---- Header: title on the left, back button vertically centred on the right ----
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JPanel titles = new JPanel();
        titles.setOpaque(false);
        titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("LEADERBOARD");
        title.setFont(new Font("SansSerif", Font.BOLD, 34));
        title.setForeground(Color.WHITE);
        JLabel subtitle = new JLabel("Each player\u2019s total of their best level scores. Type below to search.");
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 13));
        subtitle.setForeground(new Color(157, 178, 216));
        titles.add(title);
        titles.add(Box.createVerticalStrut(2));
        titles.add(subtitle);

        JButton back = new JButton("BACK");
        styleButton(back, new Dimension(150, 38));
        back.setToolTipText("Back to main menu (Esc)");
        back.addActionListener(e -> frame.showMenu());
        JPanel backWrap = new JPanel(new GridBagLayout());
        backWrap.setOpaque(false);
        backWrap.add(back);
        header.add(titles, BorderLayout.WEST);
        header.add(backWrap, BorderLayout.EAST);
        root.add(header, BorderLayout.NORTH);

        // ---- Centre: podium + table card ----
        JPanel center = new JPanel(new BorderLayout(0, 14));
        center.setOpaque(false);
        root.add(center, BorderLayout.CENTER);
        podium.setOpaque(false);
        center.add(podium, BorderLayout.NORTH);

        JPanel tableCard = new JPanel(new BorderLayout(0, 12));
        tableCard.setBackground(CARD);
        tableCard.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(66, 94, 153), 1), new EmptyBorder(14, 16, 12, 16)));

        // Search bar
        JPanel bar = new JPanel(new BorderLayout(12, 0));
        bar.setOpaque(false);
        resultLabel.setForeground(new Color(206, 222, 255));
        resultLabel.setFont(new Font("SansSerif", Font.BOLD, 12));

        searchField.setFont(new Font("SansSerif", Font.PLAIN, 14));
        searchField.setForeground(Color.WHITE);
        searchField.setCaretColor(Color.WHITE);
        searchField.setBackground(new Color(8, 16, 39));
        searchField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(64, 94, 150)), new EmptyBorder(0, 11, 0, 11)));
        searchField.setPreferredSize(new Dimension(260, 36));
        searchField.setToolTipText("Search player name or score (Linear Search)");
        JButton search = new JButton("SEARCH");
        styleButton(search, new Dimension(90, 36));
        JButton clear = new JButton("CLEAR");
        styleButton(clear, new Dimension(80, 36));
        clear.setBackground(new Color(30, 44, 80));
        search.addActionListener(e -> refreshRows(searchField.getText().trim()));
        clear.addActionListener(e -> { searchField.setText(""); refreshRows(""); searchField.requestFocusInWindow(); });
        searchField.addActionListener(e -> refreshRows(searchField.getText().trim()));
        // Results update as the user types
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            private void changed() { refreshRows(searchField.getText().trim()); }
            @Override public void insertUpdate(DocumentEvent e) { changed(); }
            @Override public void removeUpdate(DocumentEvent e) { changed(); }
            @Override public void changedUpdate(DocumentEvent e) { changed(); }
        });
        JPanel searchGroup = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        searchGroup.setOpaque(false);
        searchGroup.add(searchField);
        searchGroup.add(search);
        searchGroup.add(clear);
        bar.add(resultLabel, BorderLayout.WEST);
        bar.add(searchGroup, BorderLayout.EAST);
        tableCard.add(bar, BorderLayout.NORTH);

        // Table
        table.setRowHeight(40);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setBackground(ROW_A);
        table.setForeground(new Color(226, 235, 255));
        table.setFont(new Font("SansSerif", Font.PLAIN, 14));
        table.setSelectionBackground(new Color(39, 69, 120));
        table.setSelectionForeground(Color.WHITE);
        table.setFillsViewportHeight(true);
        table.setFocusable(false);
        table.setRowSelectionAllowed(false);

        JTableHeader th = table.getTableHeader();
        th.setReorderingAllowed(false);
        th.setResizingAllowed(false);
        th.setPreferredSize(new Dimension(0, 36));
        th.setDefaultRenderer(new HeaderRenderer());

        int[] widths = {70, 320, 130, 160};
        for (int i = 0; i < widths.length; i++) {
            TableColumn col = table.getColumnModel().getColumn(i);
            col.setPreferredWidth(widths[i]);
            col.setCellRenderer(new RowRenderer(i == 1 ? SwingConstants.LEFT : SwingConstants.CENTER));
        }
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(37, 57, 100)));
        scroll.getViewport().setBackground(CARD);
        tableCard.add(scroll, BorderLayout.CENTER);

        center.add(tableCard, BorderLayout.CENTER);

        // Esc = back to menu
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "back");
        getActionMap().put("back", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { frame.showMenu(); }
        });

        refreshRows("");
    }

    public void refreshRows(String query) {
        model.setRowCount(0);
        List<Leaderboard.Record> all = Leaderboard.getInstance().getRecords();
        String q = query == null ? "" : query.trim().toLowerCase();
        int rank = 0;
        for (Leaderboard.Record r : all) {
            if (!q.isEmpty() && !r.name.toLowerCase().contains(q) && !Integer.toString(r.score).contains(q)) continue;
            rank++;
            String status = rank == 1 ? "\u2605 CHAMPION" : rank == 2 ? "\u25C6 ELITE" : rank == 3 ? "\u2726 PRO" : "PLAYER";
            model.addRow(new Object[]{String.format("%02d", rank), r.name, String.format("%,d", r.score), status});
        }
        if (all.isEmpty()) {
            model.addRow(new Object[]{"\u2014", "No scores yet \u2013 play a game to get on the board!", "\u2014", ""});
        } else if (rank == 0) {
            model.addRow(new Object[]{"\u2014", "No matches for \u201C" + query.trim() + "\u201D", "\u2014", ""});
        }
        if (q.isEmpty()) resultLabel.setText("ALL-TIME LEGENDS  \u2022  " + all.size() + (all.size() == 1 ? " player" : " players"));
        else resultLabel.setText("SEARCH RESULTS  \u2022  " + rank + " found");

        podium.removeAll();
        int[] order = {1, 0, 2};
        for (int pos : order) {
            String medal = pos == 0 ? "#1  CHAMPION" : pos == 1 ? "#2  RUNNER-UP" : "#3  CONTENDER";
            Color c = pos == 0 ? GOLD : pos == 1 ? SILVER : BRONZE;
            if (pos < all.size()) {
                Leaderboard.Record r = all.get(pos);
                podium.add(makePodium(medal, r.name, String.format("%,d PTS", r.score), c, pos == 0));
            } else {
                podium.add(makePodium(medal, "Awaiting a player", "\u2014 PTS", new Color(78, 99, 142), false));
            }
        }
        podium.revalidate();
        podium.repaint();
    }

    private JPanel makePodium(String label, String name, String score, Color accent, boolean winner) {
        JPanel card = new JPanel(new BorderLayout(0, 6));
        card.setBackground(new Color(18, 30, 65));
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 160), winner ? 2 : 1),
            new EmptyBorder(11, 14, 11, 14)));
        JLabel l = new JLabel(label);
        l.setForeground(accent);
        l.setFont(new Font("SansSerif", Font.BOLD, 11));
        JLabel n = new JLabel(ellipsize(name, 20));
        n.setForeground(Color.WHITE);
        n.setFont(new Font("SansSerif", Font.BOLD, 17));
        JLabel s = new JLabel(score);
        s.setForeground(new Color(126, 220, 245));
        s.setFont(new Font("Monospaced", Font.BOLD, 14));
        card.add(l, BorderLayout.NORTH);
        card.add(n, BorderLayout.CENTER);
        card.add(s, BorderLayout.SOUTH);
        card.setPreferredSize(new Dimension(180, 90));
        return card;
    }

    private String ellipsize(String s, int n) { return s.length() > n ? s.substring(0, n - 1) + "\u2026" : s; }

    private void styleButton(JButton b, Dimension size) {
        b.setFont(new Font("SansSerif", Font.BOLD, 11));
        b.setForeground(Color.WHITE);
        b.setBackground(new Color(37, 75, 132));
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createLineBorder(new Color(72, 119, 186)));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setPreferredSize(size);
        b.setMinimumSize(size);
        b.setMaximumSize(size);
    }

    /** Header cells aligned the same way as their columns, without boxed borders. */
    private static class HeaderRenderer extends DefaultTableCellRenderer {
        HeaderRenderer() { setOpaque(true); }
        @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int row, int col) {
            super.getTableCellRendererComponent(t, v, false, false, row, col);
            setBackground(new Color(26, 42, 82));
            setForeground(new Color(120, 223, 246));
            setFont(new Font("SansSerif", Font.BOLD, 11));
            setHorizontalAlignment(col == 1 ? SwingConstants.LEFT : SwingConstants.CENTER);
            setBorder(new EmptyBorder(0, col == 1 ? 14 : 0, 0, 0));
            return this;
        }
    }

    /** Zebra rows, gold/silver/bronze for the top three, padded player column. */
    private static class RowRenderer extends DefaultTableCellRenderer {
        private final int align;
        RowRenderer(int align) { this.align = align; }
        @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int row, int col) {
            super.getTableCellRendererComponent(t, v, false, false, row, col);
            setHorizontalAlignment(align);
            setBorder(new EmptyBorder(0, align == SwingConstants.LEFT ? 14 : 0, 0, 0));
            setBackground(row % 2 == 0 ? ROW_A : ROW_B);
            boolean real = !"\u2014".equals(String.valueOf(t.getValueAt(row, 0)));
            Color fg = new Color(226, 235, 255);
            if (!real) fg = new Color(140, 160, 200);
            else if (row == 0 && (col == 0 || col == 3)) fg = GOLD;
            else if (row == 1 && (col == 0 || col == 3)) fg = SILVER;
            else if (row == 2 && (col == 0 || col == 3)) fg = BRONZE;
            setForeground(fg);
            setFont(t.getFont().deriveFont(col == 2 || (real && row < 3 && col != 1) ? Font.BOLD : Font.PLAIN));
            return this;
        }
    }

    @Override protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setPaint(new GradientPaint(0, 0, new Color(7, 12, 32), getWidth(), getHeight(), new Color(21, 29, 70)));
        g.fillRect(0, 0, getWidth(), getHeight());
        g.setColor(new Color(65, 107, 195, 20));
        for (int x = 0; x < getWidth(); x += 34) g.drawLine(x, 0, x, getHeight());
        for (int y = 0; y < getHeight(); y += 34) g.drawLine(0, y, getWidth(), y);
        g.dispose();
    }
}
