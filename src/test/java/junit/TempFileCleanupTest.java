package junit;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.Random;

import com.sun.mail.smtp.SMTPTransport;

import jakarta.mail.Address;
import jakarta.mail.Session;
import jakarta.mail.URLName;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import junit.framework.TestCase;
import junit.util.SocketUtils;
import org.mailster.smtp.SMTPServer;
import org.mailster.smtp.SMTPServerConfig;
import org.mailster.smtp.api.MessageListener;
import org.mailster.smtp.api.handler.SessionContext;

/**
 * A message larger than the data deferred size is buffered in a temporary file. The file must be
 * gone once every recipient has closed its stream, without waiting for the garbage collector.
 */
public class TempFileCleanupTest extends TestCase {

    private static final int MESSAGE_SIZE = SMTPServerConfig.DEFAULT_DATA_DEFERRED_SIZE + 1024 * 1024;

    private final List<Integer> received = Collections.synchronizedList(new ArrayList<>());

    private SMTPServer server;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        server = new SMTPServer(new MessageListener() {
            @Override
            public boolean accept(SessionContext ctx, String from, String recipient) {
                return true;
            }

            @Override
            public void deliver(SessionContext ctx, String from, String recipient, InputStream data)
                    throws IOException {
                try (data) {
                    var out = new ByteArrayOutputStream();
                    data.transferTo(out);
                    received.add(out.size());
                }
            }
        });
        server.setPort(SocketUtils.findAvailableTcpPort());
        server.start();
    }

    @Override
    protected void tearDown() throws Exception {
        server.shutdown();
        super.tearDown();
    }

    public void testTempFileIsDeletedAfterDeliveryToAllRecipients() throws Exception {
        var before = tempFiles();

        var session = Session.getInstance(new Properties());
        var message = new MimeMessage(session);
        message.setFrom(new InternetAddress("ted@home.com"));
        message.setSubject("big");
        var content = new byte[MESSAGE_SIZE];
        new Random(42).nextBytes(content);
        message.setContent(Base64.getMimeEncoder().encodeToString(content), "text/plain");
        message.saveChanges();

        var recipients = new Address[]{new InternetAddress("a@example.org"), new InternetAddress("b@example.org")};
        try (var transport = new SMTPTransport(session, new URLName("smtp://localhost:" + server.getPort()))) {
            transport.connect();
            transport.sendMessage(message, recipients);
        }

        assertEquals(2, received.size());
        assertEquals(received.get(0), received.get(1));
        assertTrue(received.get(0) > MESSAGE_SIZE);
        assertEquals(before, tempFiles());
    }

    private static List<String> tempFiles() {
        var names = new File(System.getProperty("java.io.tmpdir")).list(
                (dir, name) -> name.startsWith("mailsterSmtp") && name.endsWith(".eml"));
        var list = new ArrayList<String>(names == null ? List.of() : List.of(names));
        Collections.sort(list);
        return list;
    }
}
