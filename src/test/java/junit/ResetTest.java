package junit;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.junit.Test;
import org.mailster.smtp.api.handler.SessionContext;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import junit.util.Client;
import junit.util.SocketUtils;
import wiser.Wiser;

/**
 * This class tests if reset between two mails in the same session
 * is well done.
 *
 * @author De Oliveira Edouard &lt;doe_wanted@yahoo.fr&gt;
 */
public class ResetTest {

    @Test
    public void testReset() throws Exception {
        Wiser wiser = new Wiser(SocketUtils.findAvailableTcpPort()) {
            int receivedMailsCount;

            @Override
            public void deliver(SessionContext ctx, String from, String recipient, InputStream data) throws IOException {
                super.deliver(ctx, from, recipient, data);
                receivedMailsCount++;
                var bos = new ByteArrayOutputStream();
                var array = new byte[10000];
                var size = 0;
                for (var i = data.read(array); i != -1; i = data.read(array)) {
                    bos.write(array, 0, i);
                    size += i;
                }

                if (receivedMailsCount == 1) {
                    assertEquals("test1From@example.com", from);
                    assertTrue(size > 500);
                } else {
                    assertEquals("test2From@example.com", from);
                    assertTrue(size < 500);
                }

            }
        };
        wiser.start();
        wiser.setDataDeferredSize(2 * 1024);

        try (var c = new Client("localhost", wiser.getPort())) {
            c.expect("220");
            c.send("HELO foo.com");
            c.expect("250");

            // send first mail that causes deferred data
            c.send("MAIL FROM:<test1From@example.com>");
            c.expect("250");
            c.send("RCPT TO:<test1To@example.com>");
            c.expect("250");
            c.send("DATA");
            c.expect("354");
            c.send(generateMailContent("BIG MAIL", 5 * 1024, 'A'));
            c.expect("250");

            // send reset
            c.send("RSET");
            c.expect("250");

            // send second mail which is small and shouldn't be written to a temp file
            c.send("MAIL FROM:<test2From@example.com>");
            c.expect("250");
            c.send("RCPT TO:<test2To@example.com>");
            c.expect("250");
            c.send("DATA");
            c.expect("354");
            c.send(generateMailContent("SHORT MAIL", 100, 'B'));
            c.expect("250");
            c.send("QUIT");
            c.expect("221");
        }

        wiser.stop();
    }

    private String generateMailContent(String subject, int size, char c) throws IOException {
        var bos = new ByteArrayOutputStream();
        bos.write(("Subject: " + subject + "\r\n\r\n").getBytes());

        for (var i = 0; i < size; i++) {
            if ((i % 70) == 69) {
                bos.write("\r\n".getBytes());
            }
            bos.write(c);
        }
        bos.write("\r\n.".getBytes());

        return new String(bos.toByteArray());
    }
}
