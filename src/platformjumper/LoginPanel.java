package platformjumper;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/** Sign in / create account screen. Players must sign in before they can play. */
public class LoginPanel extends Theme.Screen {
    private final GameFrame frame;
    private final JTextField userField = new JTextField();
    private final JPasswordField passField = new JPasswordField();
    private final JPasswordField confirmField = new JPasswordField();
    private final JLabel title = Theme.label("SIGN IN", 30, Font.BOLD, Color.WHITE);
    private final JLabel subtitle = Theme.label("Sign in to save your progress and scores", 13, Font.PLAIN, Theme.MUTED);
    private final JLabel error = Theme.label(" ", 12, Font.BOLD, Theme.RED);
    private final JLabel confirmLabel = Theme.label("CONFIRM PASSWORD", 11, Font.BOLD, Theme.CYAN);
    private final JButton submit = Theme.button("SIGN IN", true, 300, 44);
    private final JButton switchMode = new JButton();
    private boolean registerMode = false;

    public LoginPanel(GameFrame frame) {
        super(new GridBagLayout());
        this.frame = frame;

        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(new Color(17, 27, 59, 240));
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Theme.BORDER, 1), new EmptyBorder(28, 38, 24, 38)));

        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        error.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel userLabel = Theme.label("USERNAME", 11, Font.BOLD, Theme.CYAN);
        JLabel passLabel = Theme.label("PASSWORD", 11, Font.BOLD, Theme.CYAN);
        userLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        passLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        confirmLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        Theme.styleField(userField);
        Theme.styleField(passField);
        Theme.styleField(confirmField);

        submit.setAlignmentX(Component.LEFT_ALIGNMENT);
        submit.addActionListener(e -> submit());
        userField.addActionListener(e -> passField.requestFocusInWindow());
        passField.addActionListener(e -> { if (registerMode) confirmField.requestFocusInWindow(); else submit(); });
        confirmField.addActionListener(e -> submit());

        switchMode.setFont(new Font("SansSerif", Font.BOLD, 12));
        switchMode.setForeground(Theme.CYAN);
        switchMode.setContentAreaFilled(false);
        switchMode.setBorderPainted(false);
        switchMode.setFocusPainted(false);
        switchMode.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        switchMode.setHorizontalAlignment(SwingConstants.CENTER);
        switchMode.setMargin(new Insets(0, 0, 0, 0));
        switchMode.setBorder(BorderFactory.createEmptyBorder());
        switchMode.addActionListener(e -> setMode(!registerMode));

        JButton back = Theme.button("BACK", false, 300, 40);
        back.setAlignmentX(Component.LEFT_ALIGNMENT);
        back.addActionListener(e -> frame.showMenu());

        card.add(title);
        card.add(Box.createVerticalStrut(4));
        card.add(subtitle);
        card.add(Box.createVerticalStrut(20));
        card.add(userLabel);
        card.add(Box.createVerticalStrut(6));
        card.add(userField);
        card.add(Box.createVerticalStrut(14));
        card.add(passLabel);
        card.add(Box.createVerticalStrut(6));
        card.add(passField);
        card.add(Box.createVerticalStrut(14));
        card.add(confirmLabel);
        card.add(Box.createVerticalStrut(6));
        card.add(confirmField);
        card.add(Box.createVerticalStrut(10));
        card.add(error);
        card.add(Box.createVerticalStrut(8));
        card.add(submit);
        card.add(Box.createVerticalStrut(10));
        JPanel switchRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        switchRow.setOpaque(false);
        switchRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        switchRow.add(switchMode);
        card.add(switchRow);
        card.add(Box.createVerticalStrut(14));
        card.add(back);
        add(card);

        setMode(false);
    }

    private void setMode(boolean register) {
        registerMode = register;
        title.setText(register ? "CREATE ACCOUNT" : "SIGN IN");
        subtitle.setText(register ? "Pick a username and password" : "Sign in to save your progress and scores");
        submit.setText(register ? "CREATE ACCOUNT" : "SIGN IN");
        switchMode.setText(register ? "Already have an account?  Sign in" : "New player?  Create an account");
        confirmLabel.setVisible(register);
        confirmField.setVisible(register);
        error.setText(" ");
        revalidate();
        repaint();
    }

    /** Called each time the screen is shown. */
    public void reset() {
        userField.setText("");
        passField.setText("");
        confirmField.setText("");
        setMode(false);
        SwingUtilities.invokeLater(() -> userField.requestFocusInWindow());
    }

    private void submit() {
        String name = userField.getText().trim();
        String pw = new String(passField.getPassword());
        String err;
        if (registerMode && !pw.equals(new String(confirmField.getPassword()))) {
            err = "Passwords do not match";
        } else {
            err = registerMode ? UserManager.getInstance().register(name, pw)
                               : UserManager.getInstance().login(name, pw);
        }
        if (err != null) {
            error.setText(err);
            return;
        }
        frame.showLevelSelect();
    }
}
