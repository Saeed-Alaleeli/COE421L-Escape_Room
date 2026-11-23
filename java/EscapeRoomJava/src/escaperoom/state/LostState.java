package escaperoom.state;

import escaperoom.GameController;

/** Time ran out. */
public class LostState implements GameState {
    @Override public String getName() { return "Game Over"; }

    @Override
    public void enter(GameController c) {
        c.stopTimer();
        c.getModel().setScreen(getName(), 0, "TIME'S UP - GAME OVER",
                "Press START GAME to try again.");
        c.send("LOSE");
    }

    @Override
    public void onMessage(GameController c, String[] msg) { }
}
