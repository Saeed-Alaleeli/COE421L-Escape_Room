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
        // the model holds all game data (Observer pattern subject)
        GameModel model = new GameModel();
        // the controller runs the game logic (State pattern context) and owns the XBee link
        GameController controller = new GameController(model);
        // Swing windows must be created on the Swing event thread
        SwingUtilities.invokeLater(() -> {
            GameWindow window = new GameWindow(model, controller);
            model.addObserver(window);          // register the window so it redraws on every change
            window.setVisible(true);
        });
    }
}
