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
    private final GameModel model;       // game data shown on screen
    private final SerialLink serial;     // XBee connection
    private GameState state;             // current state (Idle, a level, Won or Lost)
    private CountdownTimer timer;        // game clock thread (null when not running)
    private MegaSimulator simulator;     // simulation thread (null when not simulating)

    public GameController(GameModel model) {
        this.model = model;
        this.serial = new SerialLink(this::onSerialLine);   // runs on the reader thread
        this.state = new IdleState();
        this.state.enter(this);
    }

    // ---------------- state pattern ----------------
    /**
     * Switches to a new state: lets the old state clean up, logs the change,
     * then lets the new state set up the screen and send its command.
     * synchronized because the serial, timer and GUI threads can all cause changes.
     */
    public synchronized void setState(GameState next) {
        state.exit(this);
        state = next;
        model.addLog("# state -> " + next.getName());
        state.enter(this);
    }

    public synchronized GameState getState() { return state; }

    // ---------------- connection ----------------
    /** Opens the chosen COM port and sends PING to check the Mega answers. */
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

    /** Sends a command to the Mega and records it in the log. */
    public void send(String message) {
        model.addLog("PC   > " + message);
        serial.send(message);
    }

    // ---------------- game control (buttons in the window) ----------------
    /** START GAME: resets everything, tells the Mega to start, starts the clock and goes to level 1. */
    public synchronized void startGame() {
        stopTimer();                     // in case a previous game is still running
        model.reset();
        send("START");
        timer = new CountdownTimer(this);
        timer.start();
        setState(new LevelState(Level.L1));
    }

    /** Reset: stops everything and returns to the idle screen. */
    public synchronized void resetGame() {
        stopSimulation();
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
        // time ran out while still playing a level -> game over
        if (left <= 0 && state instanceof LevelState) {
            setState(new LostState());
        }
    }

    // ---------------- simulation mode (no hardware) ----------------
    /** Plays a fake game: MegaSimulator sends the messages the Mega would send. */
    public synchronized void startSimulation() {
        stopSimulation();
        model.addLog("# SIMULATION MODE (no hardware connected)");
        simulator = new MegaSimulator(this);
        simulator.start();
    }

    public synchronized void stopSimulation() {
        if (simulator != null) { simulator.stopSimulation(); simulator = null; }
    }

    /** Lets the simulator deliver a message as if it came from the XBee. */
    public void injectMessage(String line) {
        handleLine(line, "SIM  < ");
    }

    // ---------------- messages from the Mega ----------------
    /** Called on the reader thread for every line received from the XBee. */
    private void onSerialLine(String line) {
        handleLine(line, "MEGA < ");
    }

    /**
     * Handles one message from the Mega (or the simulator).
     * SENSOR and WRONG are treated the same in every state; everything else
     * is passed to the current state object, which decides what to do.
     */
    private synchronized void handleLine(String line, String logPrefix) {
        if (line.isEmpty()) return;
        model.addLog(logPrefix + line);
        String[] p = line.trim().split("\\s+");    // e.g. "SOLVED 1 5" -> ["SOLVED", "1", "5"]

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

    /** Turns a raw sensor value into a readable line for the screen, depending on the level. */
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
