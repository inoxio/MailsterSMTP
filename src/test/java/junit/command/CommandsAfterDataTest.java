package junit.command;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import org.mailster.smtp.api.handler.SessionContext;

import junit.framework.TestCase;
import junit.util.SocketUtils;
import wiser.InMemoryMessageDelivery;
import wiser.Wiser;

/**
 * Commands a client sends right after the end of data, before the server answered with 250.
 * The delivery is slow on purpose, so the commands arrive while the message is still delivered.
 */
public class CommandsAfterDataTest extends TestCase {

    private static final int DELIVERY_MILLIS = 500;

    // long enough for the server to read the end of data on its own, well before the delivery is done
    private static final int GAP_MILLIS = 100;

    // far below the 60s idle timeout of the server, a hanging session fails the test quickly
    private static final int READ_TIMEOUT_MILLIS = 5000;

    private Wiser wiser;

    private Socket socket;

    private BufferedReader reader;

    private OutputStream out;

    public CommandsAfterDataTest(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        wiser = new Wiser(SocketUtils.findAvailableTcpPort(), new InMemoryMessageDelivery() {
            @Override
            public void deliver(SessionContext ctx, String from, String recipient, InputStream data) throws IOException {
                try {
                    Thread.sleep(DELIVERY_MILLIS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                super.deliver(ctx, from, recipient, data);
            }
        });
        wiser.setHostname("localhost");
        wiser.start();

        socket = new Socket("localhost", wiser.getPort());
        socket.setTcpNoDelay(true);
        socket.setSoTimeout(READ_TIMEOUT_MILLIS);
        out = socket.getOutputStream();
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.US_ASCII));
    }

    @Override
    protected void tearDown() throws Exception {
        socket.close();
        wiser.shutdown();

        super.tearDown();
    }

    public void testQuitInSamePacketAsEndOfData() throws Exception {
        startData();

        write("Subject: test\r\n\r\nbody\r\n.\r\nQUIT\r\n");

        expect("250");
        expect("221");
        assertEquals(1, wiser.getMessages().size());
    }

    public void testQuitInOwnPacketBeforeReply() throws Exception {
        startData();

        write("Subject: test\r\n\r\nbody\r\n.\r\n");
        Thread.sleep(GAP_MILLIS);
        write("QUIT\r\n");

        expect("250");
        expect("221");
        assertEquals(1, wiser.getMessages().size());
    }

    public void testNextTransactionBeforeReply() throws Exception {
        startData();

        write("Subject: first\r\n\r\nbody\r\n.\r\n");
        Thread.sleep(GAP_MILLIS);
        write("MAIL FROM: <second@example.org>\r\n");
        write("RCPT TO: <success@example.org>\r\n");
        write("DATA\r\n");

        expect("250");
        expect("250");
        expect("250");
        expect("354");

        write("Subject: second\r\n\r\nbody\r\n.\r\n");
        expect("250");

        write("QUIT\r\n");
        expect("221");
        assertEquals(2, wiser.getMessages().size());
    }

    private void startData() throws Exception {
        expect("220");
        write("HELO foo.com\r\n");
        expect("250");
        write("MAIL FROM: <first@example.org>\r\n");
        expect("250");
        write("RCPT TO: <success@example.org>\r\n");
        expect("250");
        write("DATA\r\n");
        expect("354");
    }

    private void write(String data) throws IOException {
        out.write(data.getBytes(StandardCharsets.US_ASCII));
        out.flush();
    }

    private void expect(String code) throws IOException {
        var line = reader.readLine();
        assertNotNull("connection closed, expected " + code, line);
        assertTrue("got " + line + ", expected " + code, line.startsWith(code));
    }
}
