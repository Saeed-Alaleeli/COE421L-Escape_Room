package escaperoom.state;

import escaperoom.GameController;

/**
 * State pattern - CONCRETE STATE: Escaped.
 * Entered by LevelState after level 6 (the big red button) is solved.
 */
public class WonState implements GameState {
    @Override public String getName() { return "Escaped"; }

    /** Stops the clock, shows the time left and mistakes, then tells the Mega to celebrate. */
    @Override
    public void enter(GameController c) {
        c.stopTimer();
        int s = c.getModel().getSecondsLeft();
        c.getModel().setScreen(getName(), 7, "YOU ESCAPED!",
                String.format("Escaped with %d:%02d left and %d mistakes.", s / 60, s % 60,
                        c.getModel().getMistakes()));
        // wait 1.5 s so the Mega finishes its "level solved" beep before the win tune
        new Thread(() -> {
            try { Thread.sleep(1500); } catch (InterruptedException e) { return; }
            c.send("WIN");
        }, "Win-Delay").start();
    }

    /** The game is over, so messages from the Mega are ignored. */
    @Override
    public void onMessage(GameController c, String[] msg) { }
}
