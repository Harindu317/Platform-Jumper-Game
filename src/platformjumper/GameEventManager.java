package platformjumper;

import java.util.LinkedList;
import java.util.Queue;

/** FIFO queue: events are processed in the order they are added. */
public class GameEventManager {
    private final Queue<GameEvent> events = new LinkedList<>();

    public void enqueue(GameEvent event) { events.offer(event); }

    public void process(GamePanel game) {
        while (!events.isEmpty()) {
            GameEvent event = events.poll();
            if (event == null) continue;
            switch (event.type) {
                case COIN_COLLECTED: game.applyCoinEvent(event.points); break;
                case OBSTACLE_HIT:
                case PLAYER_FELL: game.applyDamageEvent(); break;
                case LEVEL_COMPLETED: game.applyLevelCompleteEvent(); break;
                default: break;
            }
        }
    }

    public void clear() { events.clear(); }
}
