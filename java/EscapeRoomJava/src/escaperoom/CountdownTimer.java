package escaperoom;

/**
 * Game clock running on its own THREAD.
 * Calls controller.tick() once per second until stopped.
 */
public class CountdownTimer extends Thread {
    private final GameController controller;
    // volatile: set by the GUI thread, read by this thread
    private volatile boolean running = true;

    public CountdownTimer(GameController controller) {
        super("Countdown-Timer");
        this.controller = controller;
        setDaemon(true);   // daemon thread: closes automatically when the app exits
    }

    /** Thread body: wait one second, then tell the controller a second has passed. */
    @Override
    public void run() {
        while (running) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                break;                      // stopTimer() interrupts the sleep to end quickly
            }
            if (running) controller.tick(); // updates the time left and checks for game over
        }
    }

    /** Stops the clock (game won, lost or reset). */
    public void stopTimer() {
        running = false;
        interrupt();
    }
}
