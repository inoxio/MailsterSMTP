package junit;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import junit.framework.TestCase;
import junit.util.SocketUtils;
import wiser.Wiser;

/**
 * A mail of a few megabytes with the default configuration. On Linux too small socket buffers made
 * the tcp window tiny, every piece waited for a delayed ack, a mail came in at about 10 KB/s.
 */
public class TransferSpeedTest extends TestCase {

    private static final int MAIL_SIZE = 4 * 1024 * 1024;

    // a few hundred milliseconds on loopback, about 400 s with the tiny buffers
    private static final int LIMIT_SECONDS = 20;

    private Wiser wiser;

    public TransferSpeedTest(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        wiser = new Wiser(SocketUtils.findAvailableTcpPort());
        wiser.setHostname("localhost");
        wiser.start();
    }

    @Override
    protected void tearDown() throws Exception {
        wiser.shutdown();
        super.tearDown();
    }

    public void testLargeMailWithDefaultBuffers() throws Exception {
        var executor = Executors.newSingleThreadExecutor();
        try (var socket = new Socket("localhost", wiser.getPort())) {
            var delivery = executor.submit(() -> deliver(socket));

            // the socket closes when the time is up, that also ends a blocked write
            delivery.get(LIMIT_SECONDS, TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
        }

        assertEquals(1, wiser.getMessages().size());
    }

    private Void deliver(Socket socket) throws Exception {
        var reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.US_ASCII));
        var out = socket.getOutputStream();
        expect(reader, "220");
        for (var command : new String[]{"HELO foo.com", "MAIL FROM: <a@example.org>", "RCPT TO: <b@example.org>"}) {
            out.write((command + "\r\n").getBytes(StandardCharsets.US_ASCII));
            out.flush();
            expect(reader, "250");
        }
        out.write("DATA\r\n".getBytes(StandardCharsets.US_ASCII));
        out.flush();
        expect(reader, "354");

        // lines of 76 characters like base64 in a mail
        var line = ("x".repeat(76) + "\r\n").getBytes(StandardCharsets.US_ASCII);
        out.write("Subject: big\r\n\r\n".getBytes(StandardCharsets.US_ASCII));
        for (var written = 0; written < MAIL_SIZE; written += line.length) {
            out.write(line);
        }
        out.write(".\r\n".getBytes(StandardCharsets.US_ASCII));
        out.flush();
        expect(reader, "250");
        return null;
    }

    private static void expect(BufferedReader reader, String code) throws Exception {
        var response = reader.readLine();
        assertNotNull("connection closed, expected " + code, response);
        assertTrue("got " + response + ", expected " + code, response.startsWith(code));
    }
}
