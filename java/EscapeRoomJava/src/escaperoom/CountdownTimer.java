package escaperoom;

/**
 * Game clock running on its own THREAD.
 * Calls controller.tick() once per second until stopped.
 */
public class CountdownTimer extends Thread {
    private final GameController controller;
    private volatile boolean running = true;

    public CountdownTimer(GameController controller) {
        super("Countdown-Timer");
        this.controller = controller;
        setDaemon(true);
    }

    @Override
    public void run() {
        while (running) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                break;
            }
            if (running) controller.tick();
        }
    }

    public void stopTimer() {
        running = false;
        interrupt();
    }
}
