package wiser;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.mailster.smtp.SMTPServer;
import org.mailster.smtp.api.MessageListener;
import org.mailster.smtp.api.handler.SessionContext;

/**
 * Wiser is a smart mail testing application.
 *
 * @author Jon Stevens
 * @author De Oliveira Edouard &lt;doe_wanted@yahoo.fr&gt;
 */
public class Wiser implements MessageListener {

    private final SMTPServer server;
    private final List<WiserMessage> messages = Collections.synchronizedList(new ArrayList<>());

    /**
     * Create a new SMTP server with this class as the listener.
     * The default port is set to 25. Call setPort()/setHostname() before
     * calling start().
     */
    public Wiser(final int port) {

        Collection<MessageListener> listeners = new ArrayList<>(1);
        listeners.add(this);

        this.server = new SMTPServer(listeners);
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
     * Always accept everything
     */
    @Override
    public boolean accept(SessionContext ctx, String from, String recipient) {
        return true;
    }

    /**
     * Cache the messages in memory. Now avoids unnecessary memory copying.
     */
    @Override
    public void deliver(SessionContext ctx, String from, String recipient, InputStream data) throws IOException {
        var msg = new WiserMessage(from, recipient, data);
        this.queueMessage(msg);
    }

    /**
     * deliver() calls queueMessage to store the message in an internal
     * List&lt;WiserMessage&gt. You can extend Wiser and override this method if
     * you want to store it in a different location instead.
     */
    private void queueMessage(WiserMessage message) {
        this.messages.add(message);
    }

    /**
     * @return the list of WiserMessages
     */
    public List<WiserMessage> getMessages() {
        return this.messages;
    }

    /**
     * @return an instance of the SMTPServer object
     */
    public SMTPServer getServer() {
        return this.server;
    }
}