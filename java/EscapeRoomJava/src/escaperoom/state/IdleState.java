package escaperoom.state;

import escaperoom.GameController;

/** Waiting for START GAME. */
public class IdleState implements GameState {
    @Override public String getName() { return "Idle"; }

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
