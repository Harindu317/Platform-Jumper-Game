package platformjumper;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;
import java.util.Random;

/** Polished neon arcade-style main menu, rendered with Java2D (no external assets). */
public class MenuPanel extends JPanel {
    private final GameFrame frame;
    private final Star[] stars = new Star[95];
    private final Timer animation;
    private float glow = 0;
    private final JButton userLink = new JButton();

    private static class Star {
        float x, y, r, speed;
        Star(Random random) { x=random.nextFloat(); y=random.nextFloat(); r=1+random.nextFloat()*2.2f; speed=.15f+random.nextFloat()*.55f; }
    }

    public MenuPanel(GameFrame frame) {
        this.frame = frame;
        setLayout(new GridBagLayout());
        setOpaque(false);
        Random random = new Random(24);
        for (int i=0;i<stars.length;i++) stars[i]=new Star(random);

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(BorderFactory.createEmptyBorder(20, 30, 16, 30));

        JLabel title = new JLabel("PLATFORM");
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        title.setFont(new Font("SansSerif", Font.BOLD, 48));
        title.setForeground(Color.WHITE);
        JLabel title2 = new JLabel("JUMPER");
        title2.setAlignmentX(Component.CENTER_ALIGNMENT);
        title2.setFont(new Font("SansSerif", Font.BOLD, 54));
        title2.setForeground(new Color(255, 207, 77));
        JLabel subtitle = new JLabel("RUN • JUMP • COLLECT • CONQUER");
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        subtitle.setFont(new Font("SansSerif", Font.BOLD, 13));
        subtitle.setForeground(new Color(183, 204, 238));

                content.add(title); content.add(title2); content.add(Box.createVerticalStrut(3)); content.add(subtitle);
        content.add(Box.createVerticalStrut(14));
        userLink.setAlignmentX(Component.CENTER_ALIGNMENT);
        userLink.setFont(new Font("SansSerif", Font.BOLD, 12));
        userLink.setForeground(new Color(93, 239, 255));
        userLink.setContentAreaFilled(false);
        userLink.setBorderPainted(false);
        userLink.setFocusPainted(false);
        userLink.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        userLink.addActionListener(e -> {
            if (UserManager.getInstance().isLoggedIn()) { UserManager.getInstance().logout(); refresh(); }
            else frame.showLogin();
        });
        content.add(userLink);
        content.add(Box.createVerticalStrut(10));
        content.add(makeButton("START ADVENTURE", true, frame::startAdventure));
        content.add(Box.createVerticalStrut(10));
        content.add(makeButton("LEADERBOARD", false, frame::showScores));
        content.add(Box.createVerticalStrut(10));
        content.add(makeButton("HOW TO PLAY", false, frame::showInstructions));
        content.add(Box.createVerticalStrut(10));
        content.add(makeButton("EXIT GAME", false, () -> System.exit(0)));
        content.add(Box.createVerticalStrut(19));
        JLabel footer = new JLabel("FIFTEEN LEVELS   •   ONE LEGEND   •   YOUR HIGH SCORE", SwingConstants.CENTER);
        footer.setAlignmentX(Component.CENTER_ALIGNMENT);
        footer.setFont(new Font("SansSerif", Font.BOLD, 10));
        footer.setForeground(new Color(124, 151, 192));
        content.add(footer);

        GridBagConstraints c = new GridBagConstraints();
        c.gridx=0; c.gridy=0; c.insets=new Insets(10,10,10,10); c.anchor=GridBagConstraints.CENTER;
        add(content,c);
        animation = new Timer(45, e -> { glow += .045f; for (Star s:stars) { s.y += s.speed/900f; if(s.y>1) {s.y=0; s.x=(s.x+.37f)%1f;} } repaint(); });
        animation.start();
        refresh();
    }

    /** Updates the sign-in line under the title. */
    public void refresh() {
        UserManager um = UserManager.getInstance();
        userLink.setText(um.isLoggedIn() ? "Sign Out, " + um.getCurrentName() : "Sign in");
        repaint();
    }

    /** Boxed menu button (text only, no icons). */
    private JButton makeButton(String text, boolean primary, Runnable action) {
        JButton b = new JButton(text) {
            private boolean hover;
            { addMouseListener(new java.awt.event.MouseAdapter() { public void mouseEntered(java.awt.event.MouseEvent e){hover=true;repaint();} public void mouseExited(java.awt.event.MouseEvent e){hover=false;repaint();} }); }
            @Override protected void paintComponent(Graphics graphics) {
                Graphics2D g=(Graphics2D)graphics.create(); g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                boolean active=getModel().isPressed() || hover;
                Color top=primary?new Color(0,211,255):new Color(36,58,100);
                Color bottom=primary?new Color(55,107,255):new Color(24,38,70);
                if(active){top=primary?new Color(89,236,255):new Color(54,81,132);bottom=primary?new Color(73,127,255):new Color(35,55,94);}
                g.setPaint(new GradientPaint(0,0,top,0,getHeight(),bottom));
                g.fillRoundRect(0,0,getWidth(),getHeight(),18,18);
                g.setColor(primary?new Color(119,245,255,180):new Color(108,153,221,100)); g.drawRoundRect(0,0,getWidth()-1,getHeight()-1,18,18);
                g.dispose(); super.paintComponent(graphics);
            }
        };
        b.setFont(new Font("SansSerif",Font.BOLD,14)); b.setForeground(Color.WHITE); b.setHorizontalAlignment(SwingConstants.CENTER);
        Dimension size = new Dimension(220, 40);
        b.setPreferredSize(size); b.setMaximumSize(size); b.setMinimumSize(size);
        b.setContentAreaFilled(false); b.setOpaque(false); b.setBorderPainted(false); b.setFocusPainted(false); b.setBorder(BorderFactory.createEmptyBorder(8,14,8,14));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setAlignmentX(Component.CENTER_ALIGNMENT);
        b.addActionListener(e -> action.run()); return b;
    }

    @Override protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics); Graphics2D g=(Graphics2D)graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        int w=getWidth(),h=getHeight();
        g.setPaint(new GradientPaint(0,0,new Color(8,13,39),w,h,new Color(26,31,83))); g.fillRect(0,0,w,h);
        // Soft neon atmosphere
        g.setPaint(new RadialGradientPaint(new Point2D.Float(w*.5f,h*.42f),Math.max(w,h)*.55f,new float[]{0f,1f},new Color[]{new Color(51,76,169,105),new Color(7,12,35,0)})); g.fillRect(0,0,w,h);
        for(Star s:stars){g.setColor(new Color(180,222,255,90+(int)(90*(.5+.5*Math.sin(glow+s.x*9)))));g.fillOval((int)(s.x*w),(int)(s.y*h),(int)s.r,(int)s.r);}
        // distant angular mountains
        g.setColor(new Color(26,38,87)); int[] mx={0,w/8,w/4,w*2/5,w*3/5,w*4/5,w}; int[] my={h*4/5,h*2/3,h*4/5,h*3/5,h*4/5,h*2/3,h*4/5}; g.fillPolygon(mx,my,mx.length);
        g.setColor(new Color(21,28,67)); g.fillRect(0,h*4/5,w,h/5);
        // floating platforms with neon edges
        for(int i=0;i<5;i++){int px=(i*197+35)%Math.max(1,w); int py=h*4/5-18-(i%3)*42; g.setColor(new Color(30,51,101,210));g.fillRoundRect(px,py,82,13,7,7);g.setColor(new Color(40,229,235,180));g.fillRoundRect(px,py,82,3,3,3);}
        // glowing pixel-style coin decoration
        for(int i=0;i<3;i++){int cx=w-105-i*38,cy=h/3+(int)(Math.sin(glow+i)*5);g.setColor(new Color(255,191,46,45));g.fillOval(cx-5,cy-5,27,27);g.setColor(new Color(255,211,78));g.fillOval(cx,cy,17,17);g.setColor(new Color(255,246,179));g.drawOval(cx+4,cy+3,7,10);}
        g.dispose();
    }
}
