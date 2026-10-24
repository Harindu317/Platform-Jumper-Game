package platformjumper;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.KeyEvent;

/** Themed pop-up with a title, a few info lines and a column of buttons (pause, level complete, game over). */
public class MenuDialog extends JDialog {
    private int result = -1;

    /** Returns the index of the clicked button, or -1 if the dialog was closed with Esc. */
    public static int show(JFrame owner, String title, Color titleColor, String[] lines, String[] buttons) {
        MenuDialog d = new MenuDialog(owner, title, titleColor, lines, buttons);
        d.setVisible(true);
        return d.result;
    }

    private MenuDialog(JFrame owner, String title, Color titleColor, String[] lines, String[] buttons) {
        super(owner, title, true);
        JPanel root = new Theme.Screen(new BorderLayout(0, 18));
        root.setBorder(new EmptyBorder(26, 40, 26, 40));

        JPanel head = new JPanel();
        head.setOpaque(false);
        head.setLayout(new BoxLayout(head, BoxLayout.Y_AXIS));
        JLabel t = Theme.label(title, 30, Font.BOLD, titleColor);
        t.setAlignmentX(Component.CENTER_ALIGNMENT);
        head.add(t);
        head.add(Box.createVerticalStrut(8));
        for (String line : lines) {
            JLabel l = Theme.label(line, 14, Font.PLAIN, Theme.MUTED);
            l.setAlignmentX(Component.CENTER_ALIGNMENT);
            head.add(l);
            head.add(Box.createVerticalStrut(3));
        }
        root.add(head, BorderLayout.NORTH);

        JPanel col = new JPanel();
        col.setOpaque(false);
        col.setLayout(new BoxLayout(col, BoxLayout.Y_AXIS));
        for (int i = 0; i < buttons.length; i++) {
            final int idx = i;
            JButton b = Theme.button(buttons[i], i == 0, 280, 44);
            b.setAlignmentX(Component.CENTER_ALIGNMENT);
            b.addActionListener(e -> { result = idx; dispose(); });
            col.add(b);
            if (i < buttons.length - 1) col.add(Box.createVerticalStrut(10));
            if (i == 0) getRootPane().setDefaultButton(b);
        }
        root.add(col, BorderLayout.CENTER);

        getRootPane().registerKeyboardAction(e -> { result = -1; dispose(); },
            KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
        setContentPane(root);
        setResizable(false);
        pack();
        setLocationRelativeTo(owner);
    }
}
