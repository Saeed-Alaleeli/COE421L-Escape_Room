package escaperoom;

import com.fazecast.jSerialComm.SerialPort;

import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

/**
 * Talks to the XBee (red USB board) through a COM port.
 * A background READER THREAD listens all the time and passes every full
 * line it receives to the listener, so the GUI never freezes.
 */
public class SerialLink {
    private SerialPort port;                 // the open COM port (null when closed)
    private Thread readerThread;             // background thread that reads from the XBee
    private volatile boolean running = false;// tells the reader thread when to stop
    private final Consumer<String> onLine;   // called with every complete line received

    /** @param onLine what to do with each received line (the controller passes its handler) */
    public SerialLink(Consumer<String> onLine) { this.onLine = onLine; }

    /** Names like "COM5 - USB Serial Port" for the drop-down. */
    public static String[] listPorts() {
        SerialPort[] ports = SerialPort.getCommPorts();
        String[] names = new String[ports.length];
        for (int i = 0; i < ports.length; i++) {
            names[i] = ports[i].getSystemPortName() + " - " + ports[i].getDescriptivePortName();
        }
        return names;
    }

    /**
     * Opens the COM port at 9600 baud (same as the XBees and the Mega)
     * and starts the reader thread. Returns false if the port is busy.
     */
    public synchronized boolean open(String portName) {
        close();                             // close any port that was already open
        port = SerialPort.getCommPort(portName);
        port.setComPortParameters(9600, 8, SerialPort.ONE_STOP_BIT, SerialPort.NO_PARITY);
        if (!port.openPort()) { port = null; return false; }

        running = true;
        readerThread = new Thread(this::readLoop, "XBee-Reader");
        readerThread.setDaemon(true);
        readerThread.start();
        return true;
    }

    /**
     * Reader thread body. Polls the port for new bytes and builds them into
     * lines. Every message from the Mega ends with a newline, so when a
     * newline arrives the finished line is passed to onLine.
     */
    private void readLoop() {
        StringBuilder line = new StringBuilder();   // characters of the line being received
        while (running) {
            try {
                SerialPort p = port;
                if (p == null) break;
                int n = p.bytesAvailable();
                if (n <= 0) { Thread.sleep(20); continue; }   // nothing yet: wait 20 ms so we don't use 100% CPU
                byte[] buf = new byte[n];
                p.readBytes(buf, n);
                for (byte b : buf) {
                    char c = (char) (b & 0xFF);
                    if (c == '\n' || c == '\r') {             // end of a message
                        if (line.length() > 0) { onLine.accept(line.toString().trim()); line.setLength(0); }
                    } else {
                        line.append(c);                       // still inside a message
                    }
                }
            } catch (InterruptedException e) {
                break;
            } catch (Exception e) {
                // report the error in the log and keep listening
                onLine.accept("# serial error: " + e.getMessage());
                try { Thread.sleep(200); } catch (InterruptedException ie) { break; }
            }
        }
    }

    /** Sends one command to the Mega through the XBee, ending it with a newline. */
    public synchronized void send(String message) {
        if (port == null) return;            // not connected: the command is only logged
        byte[] data = (message + "\n").getBytes(StandardCharsets.US_ASCII);
        port.writeBytes(data, data.length);
    }

    public synchronized boolean isOpen() { return port != null; }

    /** Stops the reader thread and releases the COM port. */
    public synchronized void close() {
        running = false;
        if (readerThread != null) readerThread.interrupt();
        if (port != null) { port.closePort(); port = null; }
    }
}
