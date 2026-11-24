package escaperoom.state;

import escaperoom.GameController;

/**
 * State pattern - CONCRETE STATE: Idle.
 * The starting state, before START GAME is pressed and after a reset.
 * Shows the welcome screen and ignores messages from the Mega.
 */
public class IdleState implements GameState {
    @Override public String getName() { return "Idle"; }

    /** Shows the welcome title and instructions. */
    @Override
    public void enter(GameController c) {
        c.getModel().setScreen(getName(), 0, "Wireless Escape Room",
                "Connect to the XBee port, then press START GAME.");
    }

    @Override
    public void onMessage(GameController c, String[] msg) {
        // nothing to do while idle (the log already shows the message)
    }
}
