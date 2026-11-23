package escaperoom;

import javax.swing.SwingUtilities;

/**
 * Wireless Escape Room - COE 421L Group 4.
 * Entry point: builds the model, controller and window, then shows the window.
 *
 * Design patterns:
 *  - State pattern   : escaperoom.state.* (Idle, Level 1-6, Won, Lost)
 *  - Observer pattern: GameModel (subject) notifies GameObserver objects (GameWindow)
 * Multithreading:
 *  - SerialLink reader thread (listens to the XBee all the time)
 *  - CountdownTimer thread (game clock)
 *  - Swing event thread (GUI)
 */
public class EscapeRoomApp {
    public static void main(String[] args) {
        GameModel model = new GameModel();
        GameController controller = new GameController(model);
        SwingUtilities.invokeLater(() -> {
            GameWindow window = new GameWindow(model, controller);
            model.addObserver(window);
            window.setVisible(true);
        });
    }
}
