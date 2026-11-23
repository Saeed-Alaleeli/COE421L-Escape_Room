package escaperoom.state;

import escaperoom.GameController;

/**
 * State pattern - the STATE interface.
 * Every phase of the game (idle, each level, won, lost) is its own class
 * that decides what happens when it starts and when a message arrives.
 */
public interface GameState {
    String getName();

    /** Called when the game switches INTO this state. */
    void enter(GameController c);

    /** Called when the game leaves this state. */
    default void exit(GameController c) { }

    /** Called for every message from the Mega (already split into words). */
    void onMessage(GameController c, String[] msg);
}
