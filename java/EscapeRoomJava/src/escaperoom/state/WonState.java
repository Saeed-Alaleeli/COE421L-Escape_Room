package escaperoom.state;

import escaperoom.GameController;

/** The players escaped. */
public class WonState implements GameState {
    @Override public String getName() { return "Escaped"; }

    @Override
    public void enter(GameController c) {
        c.stopTimer();
        int s = c.getModel().getSecondsLeft();
        c.getModel().setScreen(getName(), 7, "YOU ESCAPED!",
                String.format("Escaped with %d:%02d left and %d mistakes.", s / 60, s % 60,
                        c.getModel().getMistakes()));
        new Thread(() -> {
            try { Thread.sleep(1500); } catch (InterruptedException e) { return; }
            c.send("WIN");
        }, "Win-Delay").start();
    }

    @Override
    public void onMessage(GameController c, String[] msg) { }
}
