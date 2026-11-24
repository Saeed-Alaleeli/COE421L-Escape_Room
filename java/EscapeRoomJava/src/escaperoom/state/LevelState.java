package escaperoom.state;

import escaperoom.GameController;

/**
 * One state per level. When the Mega reports "SOLVED n", this state reveals
 * the code digit (levels 1-4) and moves the game to the next level, or to
 * WonState after level 6.
 */
public class LevelState implements GameState {
    private final Level level;   // which level this state represents

    public LevelState(Level level) { this.level = level; }

    public Level getLevel() { return level; }

    @Override public String getName() { return "Level " + level.number; }

    /** Shows the level title and hint, then tells the Mega to start this level. */
    @Override
    public void enter(GameController c) {
        c.getModel().setScreen(getName(), level.number, level.title, level.hint);
        if (level.number == 1) {
            c.send("LEVEL 1");
        } else {
            // give players 2 seconds to see "level solved" on the LCD first
            new Thread(() -> {
                try { Thread.sleep(2000); } catch (InterruptedException e) { return; }
                // only send if the game is still on this level (not reset in the meantime)
                if (c.getState() == this) c.send("LEVEL " + level.number);
            }, "Next-Level-Delay").start();
        }
    }

    /** Reacts to "SOLVED n [digit]" for this level; other messages are ignored here. */
    @Override
    public void onMessage(GameController c, String[] msg) {
        if (msg[0].equals("SOLVED") && msg.length >= 2 && msg[1].equals(String.valueOf(level.number))) {
            // levels 1-4 each reveal one digit of the secret code
            if (msg.length >= 3 && level.number <= 4) {
                c.getModel().revealDigit(level.number - 1, msg[2].charAt(0));
            }
            // move to the next level, or to the win screen after level 6
            Level next = level.next();
            c.setState(next == null ? new WonState() : new LevelState(next));
        }
    }
}
