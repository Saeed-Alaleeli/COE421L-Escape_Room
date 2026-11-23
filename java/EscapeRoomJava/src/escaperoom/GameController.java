package escaperoom;

import escaperoom.state.GameState;
import escaperoom.state.IdleState;
import escaperoom.state.Level;
import escaperoom.state.LevelState;
import escaperoom.state.LostState;

/**
 * State pattern - the CONTEXT.
 * Holds the current GameState and forwards every XBee message to it.
 * Also owns the serial link and the countdown thread.
 */
public class GameController {
    private final GameModel model;
    private final SerialLink serial;
    private GameState state;
    private CountdownTimer timer;

    public GameController(GameModel model) {
        this.model = model;
        this.serial = new SerialLink(this::onSerialLine);   // runs on the reader thread
        this.state = new IdleState();
        this.state.enter(this);
    }

    // ---------------- state pattern ----------------
    public synchronized void setState(GameState next) {
        state.exit(this);
        state = next;
        model.addLog("# state -> " + next.getName());
        state.enter(this);
    }

    public synchronized GameState getState() { return state; }

    // ---------------- connection ----------------
    public boolean connect(String portLabel) {
        String portName = portLabel.split(" ")[0];          // "COM5 - ..." -> "COM5"
        boolean ok = serial.open(portName);
        model.setConnected(ok);
        model.addLog(ok ? "# connected to " + portName : "# could not open " + portName);
        if (ok) send("PING");
        return ok;
    }

    public void disconnect() {
        serial.close();
        model.setConnected(false);
        model.addLog("# disconnected");
    }

    public void send(String message) {
        model.addLog("PC   > " + message);
        serial.send(message);
    }

    // ---------------- game control (buttons in the window) ----------------
    public synchronized void startGame() {
        stopTimer();
        model.reset();
        send("START");
        timer = new CountdownTimer(this);
        timer.start();
        setState(new LevelState(Level.L1));
    }

    public synchronized void resetGame() {
        stopTimer();
        send("RESET");
        model.reset();
        setState(new IdleState());
    }

    public synchronized void stopTimer() {
        if (timer != null) { timer.stopTimer(); timer = null; }
    }

    /** Called once per second by the CountdownTimer thread. */
    public synchronized void tick() {
        int left = model.getSecondsLeft() - 1;
        model.setSecondsLeft(Math.max(left, 0));
        if (left <= 0 && state instanceof LevelState) {
            setState(new LostState());
        }
    }

    // ---------------- messages from the Mega ----------------
    private synchronized void onSerialLine(String line) {
        if (line.isEmpty()) return;
        model.addLog("MEGA < " + line);
        String[] p = line.trim().split("\\s+");

        // messages handled the same way in every state
        if (p[0].equals("SENSOR") && p.length >= 3) {
            model.setSensorText(sensorText(p[1], p[2]));
            return;
        }
        if (p[0].equals("WRONG")) {
            model.addMistake();
        }
        // everything else depends on the current state
        state.onMessage(this, p);
    }

    private String sensorText(String level, String value) {
        switch (level) {
            case "2": return "Tilt: " + value + " / 45";
            case "3": return "Distance: " + value + " cm (target 5-25 cm)";
            case "4": return "Darkness: " + value + " / 2";
            default:  return value;
        }
    }

    public GameModel getModel() { return model; }
}
