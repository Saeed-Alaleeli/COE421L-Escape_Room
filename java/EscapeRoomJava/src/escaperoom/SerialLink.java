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
    private SerialPort port;
    private Thread readerThread;
    private volatile boolean running = false;
    private final Consumer<String> onLine;

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

    public synchronized boolean open(String portName) {
        close();
        port = SerialPort.getCommPort(portName);
        port.setComPortParameters(9600, 8, SerialPort.ONE_STOP_BIT, SerialPort.NO_PARITY);
        if (!port.openPort()) { port = null; return false; }

        running = true;
        readerThread = new Thread(this::readLoop, "XBee-Reader");
        readerThread.setDaemon(true);
        readerThread.start();
        return true;
    }

    private void readLoop() {
        StringBuilder line = new StringBuilder();
        while (running) {
            try {
                SerialPort p = port;
                if (p == null) break;
                int n = p.bytesAvailable();
                if (n <= 0) { Thread.sleep(20); continue; }
                byte[] buf = new byte[n];
                p.readBytes(buf, n);
                for (byte b : buf) {
                    char c = (char) (b & 0xFF);
                    if (c == '\n' || c == '\r') {
                        if (line.length() > 0) { onLine.accept(line.toString().trim()); line.setLength(0); }
                    } else {
                        line.append(c);
                    }
                }
            } catch (InterruptedException e) {
                break;
            } catch (Exception e) {
                onLine.accept("# serial error: " + e.getMessage());
                try { Thread.sleep(200); } catch (InterruptedException ie) { break; }
            }
        }
    }

    public synchronized void send(String message) {
        if (port == null) return;
        byte[] data = (message + "\n").getBytes(StandardCharsets.US_ASCII);
        port.writeBytes(data, data.length);
    }

    public synchronized boolean isOpen() { return port != null; }

    public synchronized void close() {
        running = false;
        if (readerThread != null) readerThread.interrupt();
        if (port != null) { port.closePort(); port = null; }
    }
}
