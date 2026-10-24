package platformjumper;
import java.awt.Rectangle;
public class Coin {
    public final int x, y, size = 16;
    public boolean collected;
    public Coin(int x, int y) { this.x=x; this.y=y; }
    public Rectangle bounds() { return new Rectangle(x,y,size,size); }
}
