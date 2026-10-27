package platformjumper;

import javax.swing.*;
import java.awt.*;

public class GameFrame extends JFrame {
    private final CardLayout cards = new CardLayout();
    private final JPanel root = new JPanel(cards);
    private final MenuPanel menu;
    private final GamePanel game;
    private final LevelSelectPanel levels;

    public GameFrame() {
        super("JUMPBRO");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(960, 640);
        setMinimumSize(new Dimension(800, 560));
        setLocationRelativeTo(null);
        menu = new MenuPanel(this);
        game = new GamePanel(this);
        levels = new LevelSelectPanel(this);
        root.add(menu, "menu");
        root.add(levels, "levels");
        root.add(game, "game");
        setContentPane(root);
    }

    public void showMenu() {
        game.stopGame();
        menu.refresh();
        cards.show(root, "menu");
    }

    /** START ADVENTURE: opens level select immediately (no account required). */
    public void startAdventure() {
        showLevelSelect();
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
}
