package escaperoom;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Observer pattern - the SUBJECT.
 * Holds everything the screen shows. Every setter notifies all observers.
 * Methods are synchronized because the serial thread, the timer thread and
 * the GUI thread all touch the model.
 */
public class GameModel {
    public static final int GAME_SECONDS = 5 * 60;   // total game time

    private final List<GameObserver> observers = new CopyOnWriteArrayList<>();

    private String stateName = "Idle";
    private int level = 0;
    private String title = "Wireless Escape Room";
    private String hint = "Connect to the XBee port, then press START GAME.";
    private int secondsLeft = GAME_SECONDS;
    private final char[] digits = {'_', '_', '_', '_'};
    private int mistakes = 0;
    private String sensorText = "";
    private boolean connected = false;
    private final List<String> log = new ArrayList<>();

    // ---------------- observer management ----------------
    public void addObserver(GameObserver o) { observers.add(o); }
    public void removeObserver(GameObserver o) { observers.remove(o); }
    private void notifyObservers() { for (GameObserver o : observers) o.onGameChanged(this); }

    // ---------------- updates ----------------
    public void reset() {
        synchronized (this) {
            stateName = "Idle"; level = 0; secondsLeft = GAME_SECONDS; mistakes = 0; sensorText = "";
            for (int i = 0; i < 4; i++) digits[i] = '_';
        }
        notifyObservers();
    }

    public void setScreen(String stateName, int level, String title, String hint) {
        synchronized (this) {
            this.stateName = stateName; this.level = level; this.title = title; this.hint = hint; this.sensorText = "";
        }
        notifyObservers();
    }

    public void setSecondsLeft(int s) { synchronized (this) { secondsLeft = s; } notifyObservers(); }

    public void revealDigit(int index, char d) {
        synchronized (this) { if (index >= 0 && index < 4) digits[index] = d; }
        notifyObservers();
    }

    public void addMistake() { synchronized (this) { mistakes++; } notifyObservers(); }

    public void setSensorText(String t) { synchronized (this) { sensorText = t; } notifyObservers(); }

    public void setConnected(boolean c) { synchronized (this) { connected = c; } notifyObservers(); }

    public void addLog(String line) {
        synchronized (this) {
            log.add(line);
            if (log.size() > 200) log.remove(0);
        }
        notifyObservers();
    }

    // ---------------- getters ----------------
    public synchronized String getStateName() { return stateName; }
    public synchronized int getLevel() { return level; }
    public synchronized String getTitle() { return title; }
    public synchronized String getHint() { return hint; }
    public synchronized int getSecondsLeft() { return secondsLeft; }
    public synchronized String getDigits() { return digits[0] + " " + digits[1] + " " + digits[2] + " " + digits[3]; }
    public synchronized int getMistakes() { return mistakes; }
    public synchronized String getSensorText() { return sensorText; }
    public synchronized boolean isConnected() { return connected; }
    public synchronized List<String> getLog() { return new ArrayList<>(log); }
}
