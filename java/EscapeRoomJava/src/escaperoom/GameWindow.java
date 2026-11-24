package escaperoom;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;

/**
 * Observer pattern - an OBSERVER.
 * The game screen. It never polls: it redraws whenever the model notifies it.
 */
public class GameWindow extends JFrame implements GameObserver {
    // colors used in the window (dark theme)
    private static final Color BG = new Color(20, 20, 24);
    private static final Color PANEL = new Color(35, 35, 42);
    private static final Color TEXT = new Color(235, 235, 235);
    private static final Color RED = new Color(230, 60, 60);
    private static final Color GREEN = new Color(80, 210, 120);
    private static final Color AMBER = new Color(245, 190, 60);

    private final GameController controller;

    // top bar controls
    private final JComboBox<String> portBox = new JComboBox<>();
    private final JButton refreshBtn = new JButton("Refresh");
    private final JButton connectBtn = new JButton("Connect");
    private final JButton startBtn = new JButton("START GAME");
    private final JButton resetBtn = new JButton("Reset");
    private final JButton simBtn = new JButton("Simulate");

    // labels that show the game data (updated in onGameChanged)
    private final JLabel statusLabel = label("Not connected", 14, Font.PLAIN, RED);
    private final JLabel stateLabel = label("Idle", 16, Font.BOLD, AMBER);
    private final JLabel timerLabel = label("05:00", 72, Font.BOLD, RED);
    private final JLabel titleLabel = label("Wireless Escape Room", 28, Font.BOLD, TEXT);
    private final JLabel hintLabel = label("", 16, Font.PLAIN, TEXT);
    private final JLabel codeLabel = label("_ _ _ _", 44, Font.BOLD, GREEN);
    private final JLabel sensorLabel = label(" ", 16, Font.PLAIN, AMBER);
    private final JLabel mistakesLabel = label("Mistakes: 0", 14, Font.PLAIN, TEXT);
    private final JProgressBar progress = new JProgressBar(0, 6);   // levels completed out of 6
    private final JTextArea logArea = new JTextArea(8, 60);          // message log (PC > / MEGA <)
    private final JTextField manualField = new JTextField(12);       // type a command by hand for testing

    public GameWindow(GameModel model, GameController controller) {
        super("Wireless Escape Room - COE 421L Group 4");
        this.controller = controller;
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        getContentPane().setBackground(BG);
        setLayout(new BorderLayout(10, 10));

        // window layout: controls on top, game info in the middle, log at the bottom
        add(buildTopBar(), BorderLayout.NORTH);
        add(buildCenter(), BorderLayout.CENTER);
        add(buildLog(), BorderLayout.SOUTH);

        refreshPorts();
        wireButtons();
        onGameChanged(model);            // draw the starting screen
        setSize(900, 720);
        setLocationRelativeTo(null);
    }

    // ---------------- layout ----------------
    /** Top bar: port selection, connect, start, reset, simulate and connection status. */
    private JPanel buildTopBar() {
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        top.setBackground(PANEL);
        top.add(label("XBee port:", 14, Font.PLAIN, TEXT));
        portBox.setPreferredSize(new Dimension(260, 28));
        top.add(portBox);
        top.add(refreshBtn);
        top.add(connectBtn);
        top.add(Box.createHorizontalStrut(20));
        startBtn.setFont(startBtn.getFont().deriveFont(Font.BOLD, 14f));
        top.add(startBtn);
        top.add(resetBtn);
        top.add(simBtn);
        top.add(Box.createHorizontalStrut(20));
        top.add(statusLabel);
        return top;
    }

    /** Middle area: state, timer, level title, hint, progress, secret code, sensor value, mistakes. */
    private JPanel buildCenter() {
        JPanel c = new JPanel();
        c.setBackground(BG);
        c.setLayout(new BoxLayout(c, BoxLayout.Y_AXIS));
        c.setBorder(new EmptyBorder(10, 20, 10, 20));

        progress.setStringPainted(true);
        progress.setForeground(GREEN);
        progress.setMaximumSize(new Dimension(600, 24));

        JLabel codeCaption = label("SECRET CODE", 14, Font.PLAIN, TEXT);

        // stack all the labels vertically, centered, with a small gap between them
        for (JComponent comp : new JComponent[]{stateLabel, timerLabel, titleLabel, hintLabel,
                progress, codeCaption, codeLabel, sensorLabel, mistakesLabel}) {
            comp.setAlignmentX(Component.CENTER_ALIGNMENT);
            c.add(comp);
            c.add(Box.createVerticalStrut(10));
        }
        return c;
    }

    /** Bottom area: the message log and a box to send commands manually. */
    private JPanel buildLog() {
        JPanel p = new JPanel(new BorderLayout(5, 5));
        p.setBackground(PANEL);
        p.setBorder(new EmptyBorder(5, 8, 8, 8));
        logArea.setEditable(false);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        p.add(new JScrollPane(logArea), BorderLayout.CENTER);

        JPanel manual = new JPanel(new FlowLayout(FlowLayout.LEFT));
        manual.setBackground(PANEL);
        manual.add(label("Send command (testing):", 12, Font.PLAIN, TEXT));
        manual.add(manualField);
        JButton sendBtn = new JButton("Send");
        sendBtn.addActionListener(e -> sendManual());
        manualField.addActionListener(e -> sendManual());
        manual.add(sendBtn);
        p.add(manual, BorderLayout.SOUTH);
        return p;
    }

    /** Helper that makes a centered label with the given font size, style and color. */
    private static JLabel label(String text, int size, int style, Color color) {
        JLabel l = new JLabel(text, SwingConstants.CENTER);
        l.setFont(new Font(Font.SANS_SERIF, style, size));
        l.setForeground(color);
        return l;
    }

    // ---------------- buttons ----------------
    /** Connects each button to the matching controller action. */
    private void wireButtons() {
        refreshBtn.addActionListener(e -> refreshPorts());
        connectBtn.addActionListener(e -> {
            // the same button connects or disconnects depending on the current status
            if (controller.getModel().isConnected()) {
                controller.disconnect();
            } else if (portBox.getSelectedItem() != null) {
                if (!controller.connect((String) portBox.getSelectedItem())) {
                    JOptionPane.showMessageDialog(this,
                            "Could not open that port.\nClose the Arduino Serial Monitor / XCTU and try again.",
                            "Connection failed", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        startBtn.addActionListener(e -> controller.startGame());
        resetBtn.addActionListener(e -> controller.resetGame());
        simBtn.addActionListener(e -> controller.startSimulation());
    }

    /** Fills the drop-down with the COM ports currently plugged in. */
    private void refreshPorts() {
        portBox.removeAllItems();
        for (String p : SerialLink.listPorts()) portBox.addItem(p);
    }

    /** Sends whatever was typed in the manual command box. */
    private void sendManual() {
        String t = manualField.getText().trim();
        if (!t.isEmpty()) controller.send(t);
        manualField.setText("");
    }

    // ---------------- observer callback ----------------
    /**
     * Observer callback: called by GameModel after every change.
     * It may be called from the serial or timer thread, so the values are
     * copied first and the labels are updated on the Swing thread.
     */
    @Override
    public void onGameChanged(GameModel m) {
        // read the model on the calling thread, draw on the Swing thread
        final String state = m.getStateName();
        final int level = m.getLevel();
        final String title = m.getTitle();
        final String hint = m.getHint();
        final int secs = m.getSecondsLeft();
        final String digits = m.getDigits();
        final String sensor = m.getSensorText();
        final int mistakes = m.getMistakes();
        final boolean connected = m.isConnected();
        final List<String> log = m.getLog();

        SwingUtilities.invokeLater(() -> {
            stateLabel.setText(state.toUpperCase());
            timerLabel.setText(String.format("%02d:%02d", secs / 60, secs % 60));
            // timer color: green, then amber under 1:30, then red under 0:30
            timerLabel.setForeground(secs <= 30 ? RED : (secs <= 90 ? AMBER : GREEN));
            titleLabel.setText(title);
            hintLabel.setText("<html><div style='text-align:center;width:600px'>" + hint + "</div></html>");
            codeLabel.setText(digits);
            sensorLabel.setText(sensor.isEmpty() ? " " : sensor);
            mistakesLabel.setText("Mistakes: " + mistakes);
            // level 7 means escaped, so all 6 levels are done
            int done = Math.max(0, Math.min(6, level >= 7 ? 6 : level - 1));
            progress.setValue(done);
            progress.setString(done + " / 6 levels done");

            statusLabel.setText(connected ? "Connected" : "Not connected");
            statusLabel.setForeground(connected ? GREEN : RED);
            connectBtn.setText(connected ? "Disconnect" : "Connect");

            // rebuild the log text and scroll to the newest line
            StringBuilder sb = new StringBuilder();
            for (String line : log) sb.append(line).append('\n');
            logArea.setText(sb.toString());
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }
}
