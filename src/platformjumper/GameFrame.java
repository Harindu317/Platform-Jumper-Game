package platformjumper;

import javax.swing.*;
import java.awt.*;

public class GameFrame extends JFrame {
    private final CardLayout cards = new CardLayout();
    private final JPanel root = new JPanel(cards);
    private final MenuPanel menu;
    private final GamePanel game;
    private final LeaderboardPanel leaderboard;
    private final LoginPanel login;
    private final LevelSelectPanel levels;

    public GameFrame() {
        super("Platform Jumper");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(960, 640);
        setMinimumSize(new Dimension(800, 560));
        setLocationRelativeTo(null);
        menu = new MenuPanel(this);
        game = new GamePanel(this);
        leaderboard = new LeaderboardPanel(this);
        login = new LoginPanel(this);
        levels = new LevelSelectPanel(this);
        root.add(menu, "menu");
        root.add(login, "login");
        root.add(levels, "levels");
        root.add(game, "game");
        root.add(leaderboard, "leaderboard");
        setContentPane(root);
    }

    public void showMenu() {
        game.stopGame();
        menu.refresh();
        cards.show(root, "menu");
    }

    /** START ADVENTURE: players must sign in before they can play. */
    public void startAdventure() {
        if (UserManager.getInstance().isLoggedIn()) showLevelSelect();
        else showLogin();
    }

    public void showLogin() {
        game.stopGame();
        login.reset();
        cards.show(root, "login");
    }

    public void showLevelSelect() {
        game.stopGame();
        levels.refresh();
        cards.show(root, "levels");
    }

    public void startLevel(int number) {
        cards.show(root, "game");
        game.startLevel(number);
        game.requestFocusInWindow();
    }

    public void showInstructions() {
        new HowToPlayDialog(this).setVisible(true);
    }

    public void showScores() {
        game.stopGame();
        leaderboard.refreshRows("");
        cards.show(root, "leaderboard");
    }
}
