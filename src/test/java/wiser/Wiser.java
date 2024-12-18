package wiser;

import java.util.Collection;
import java.util.List;

import org.mailster.smtp.SMTPServer;

/**
 * Wiser is a smart mail testing application.
 *
 * @author Jon Stevens
 * @author De Oliveira Edouard &lt;doe_wanted@yahoo.fr&gt;
 */
public class Wiser {

    private final SMTPServer server;
    private final InMemoryMessageDelivery inMemoryMessageDelivery;

    /**
     * Create a new SMTP server with this class as the listener.
     * The default port is set to 25. Call setPort()/setHostname() before
     * calling start().
     */
    public Wiser(final int port) {
        this(port, new InMemoryMessageDelivery());
    }

    public Wiser(final int port, final InMemoryMessageDelivery listener) {

        this.inMemoryMessageDelivery = listener;

        this.server = new SMTPServer(listener);
        this.server.setPort(port);

        // Set max connections much higher since we use NIO now.
        this.server.getConfig().setMaxConnections(30000);
    }

    /**
     * A main() for this class. Starts up the server.
     */
    public static void main(String[] args) {
        var wiser = new Wiser(25);
        wiser.start();
    }

    /**
     * Returns the port the server is running on.
     */
    public int getPort() {
        return server.getPort();
    }

    /**
     * The port that the server should listen on.
     */
    public void setPort(int port) {
        this.server.setPort(port);
    }

    /**
     * Set the size at which the mail will be temporary
     * stored on disk.
     */
    public void setDataDeferredSize(int dataDeferredSize) {
        this.server.getConfig().setDataDeferredSize(dataDeferredSize);
    }

    /**
     * Set the receive buffer size.
     */
    public void setReceiveBufferSize(int size) {
        this.server.getConfig().setReceiveBufferSize(size);
    }

    /**
     * The hostname that the server should listen on.
     */
    public void setHostname(String hostname) {
        this.server.getConfig().setHostName(hostname);
    }

    /**
     * Starts the SMTP Server
     */
    public void start() {
        this.server.start();
    }

    /**
     * Stops the SMTP Server
     */
    public void stop() {
        this.server.stop();
    }

    /**
     * Shutdowns the SMTP Server
     */
    public void shutdown() {
        this.server.shutdown();
    }

    /**
     * @return an instance of the SMTPServer object
     */
    public SMTPServer getServer() {
        return this.server;
    }

    public List<WiserMessage> getMessages() {
        return this.inMemoryMessageDelivery.getMessages();
    }
}
