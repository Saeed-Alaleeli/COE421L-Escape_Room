package escaperoom;

/** Observer pattern: anything that wants to react when the game changes. */
public interface GameObserver {
    void onGameChanged(GameModel model);
}
