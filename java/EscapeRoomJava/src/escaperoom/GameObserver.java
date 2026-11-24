package escaperoom;

/**
 * Observer pattern - the OBSERVER interface.
 * Any class that wants to react when the game changes implements this.
 * GameModel (the subject) keeps a list of observers and calls
 * onGameChanged() on each one after every update, so observers never
 * have to keep checking the model themselves.
 * In this project the observer is GameWindow (the game screen).
 */
public interface GameObserver {
    /**
     * Called by GameModel every time any game data changes.
     * @param model the model that changed, so the observer can read the new values
     */
    void onGameChanged(GameModel model);
}
