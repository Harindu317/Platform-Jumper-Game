package platformjumper;

public class GameEvent {
    public enum Type { COIN_COLLECTED, OBSTACLE_HIT, PLAYER_FELL, LEVEL_COMPLETED }
    public final Type type;
    public final int points;
    public GameEvent(Type type, int points) { this.type=type; this.points=points; }
}
