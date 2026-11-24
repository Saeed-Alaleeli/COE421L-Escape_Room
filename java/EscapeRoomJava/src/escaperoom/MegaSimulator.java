package escaperoom;

/**
 * Simulation mode (for testing without hardware).
 * Runs on its own thread and feeds the controller the same messages the
 * Arduino Mega would send during a full game, with realistic delays.
 */
public class MegaSimulator extends Thread {
    private final GameController controller;
    private volatile boolean running = true;

    public MegaSimulator(GameController controller) {
        super("Mega-Simulator");
        this.controller = controller;
        setDaemon(true);
    }

    /**
     * Plays one scripted game: starts the game, then sends the same messages
     * the Mega would send, including one wrong attempt in levels 1 and 5,
     * live sensor readings in levels 2-4, and the code digits 5, 3, 8, 2.
     */
    @Override
    public void run() {
        try {
            controller.startGame();
            // Level 1: one wrong attempt, then solved (digit 5)
            say("LEVEL 1", 1500);
            say("WRONG 1", 4000);
            say("SOLVED 1 5", 4000);

            // Level 2: tilt value rises to the target, then solved (digit 3)
            say("LEVEL 2", 2500);
            for (int v = 5; v <= 45; v += 10) say("SENSOR 2 " + v, 600);
            say("SOLVED 2 3", 1500);

            // Level 3: hand moves closer until it is inside 5-25 cm (digit 8)
            say("LEVEL 3", 2500);
            int[] cm = {60, 42, 31, 22, 18, 17};
            for (int v : cm) say("SENSOR 3 " + v, 600);
            say("SOLVED 3 8", 1500);

            // Level 4: light sensor gets darker until covered (digit 2)
            say("LEVEL 4", 2500);
            for (int v = 0; v <= 3; v++) say("SENSOR 4 " + v, 600);
            say("SOLVED 4 2", 1500);

            // Level 5: one wrong code, then the correct code
            say("LEVEL 5", 2500);
            say("WRONG 5", 5000);
            say("SOLVED 5", 4000);

            // Level 6: big red button pressed -> escaped
            say("LEVEL 6", 2500);
            say("SOLVED 6", 3000);
        } catch (InterruptedException e) {
            // stopped by Reset or a new simulation
        }
    }

    /** Waits, then delivers one message to the controller as if it came from the XBee. */
    private void say(String line, long waitMs) throws InterruptedException {
        Thread.sleep(waitMs);
        if (running) controller.injectMessage(line);
    }

    /** Ends the simulation early. */
    public void stopSimulation() {
        running = false;
        interrupt();
    }
}
