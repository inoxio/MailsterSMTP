package junit.util;

import java.io.IOException;

import org.mailster.smtp.api.handler.SessionContext;

import junit.framework.TestCase;
import wiser.InMemoryMessageDelivery;
import wiser.Wiser;

/**
 * A base class for testing the SMTP server at the raw protocol level.
 * Handles setting up and tearing down of the server.
 *
 * @author Jon Stevens
 * @author Jeff Schnitzer
 */
public abstract class ServerTestCase extends TestCase {

    protected Wiser wiser;

    protected Client client;

    public ServerTestCase(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        this.wiser = new Wiser(SocketUtils.findAvailableTcpPort(), new InMemoryMessageDelivery() {
            @Override
            public boolean accept(SessionContext ctx, String from, String recipient) {
                return !recipient.equals("failure@example.org");
            }
        });
        this.wiser.setHostname("localhost");
        this.wiser.start();

        this.client = new Client("localhost", wiser.getPort());
    }

    @Override
    protected void tearDown() throws Exception {
        wiser.shutdown();
        client.close();

        super.tearDown();
    }

    public void send(String msg) throws IOException {
        this.client.send(msg);
    }

    public void expect(String msg) throws Exception {
        this.client.expect(msg);
    }
}
