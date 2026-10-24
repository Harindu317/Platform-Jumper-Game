package platformjumper;
import java.awt.Rectangle;
public class Platform {
    public final int x, y, width, height;
    public Platform(int x, int y, int width, int height) { this.x=x; this.y=y; this.width=width; this.height=height; }
    public Rectangle bounds() { return new Rectangle(x,y,width,height); }
}
