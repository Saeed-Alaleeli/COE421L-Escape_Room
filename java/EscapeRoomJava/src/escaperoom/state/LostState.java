package escaperoom.state;

import escaperoom.GameController;

/**
 * State pattern - CONCRETE STATE: Game Over.
 * Entered by GameController.tick() when the countdown reaches zero.
 */
public class LostState implements GameState {
    @Override public String getName() { return "Game Over"; }

    /** Stops the clock, shows the game over screen and tells the Mega to play the lose sound. */
    @Override
    public void enter(GameController c) {
        c.stopTimer();
        c.getModel().setScreen(getName(), 0, "TIME'S UP - GAME OVER",
                "Press START GAME to try again.");
        c.send("LOSE");
    }

    /** The game is over, so messages from the Mega are ignored. */
    @Override
    public void onMessage(GameController c, String[] msg) { }
}
