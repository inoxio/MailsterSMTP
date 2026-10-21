package junit;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Random;

import com.sun.mail.smtp.SMTPTransport;

import jakarta.activation.DataHandler;
import jakarta.activation.FileDataSource;
import jakarta.mail.Address;
import jakarta.mail.Message;
import jakarta.mail.Multipart;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.URLName;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import junit.framework.TestCase;
import junit.util.SocketUtils;
import wiser.Wiser;

/**
 * This class tests the transfer speed of emails that carry
 * attached files.
 *
 * @author De Oliveira Edouard &lt;doe_wanted@yahoo.fr&gt;
 */
public class BigAttachmentTest extends TestCase {

    private static final int BUFFER_SIZE = 32768;
    private static final int BIGFILE_SIZE = 5 * 1024 * 1024;

    private Wiser wiser;

    private File bigFile;

    public BigAttachmentTest(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        bigFile = createBigFile();
        wiser = new Wiser(SocketUtils.findAvailableTcpPort());
        wiser.setReceiveBufferSize(BUFFER_SIZE);
        wiser.start();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        wiser.stop();
        Files.deleteIfExists(bigFile.toPath());
    }

    /**
     * Creates a temporary file with random binary content, so the test does not
     * depend on files of the JDK installation (e.g. jmods, which newer JDKs no longer ship).
     */
    private static File createBigFile() throws IOException {
        var file = File.createTempFile("bigattachment", ".bin");
        var data = new byte[BIGFILE_SIZE];
        new Random(42).nextBytes(data);
        Files.write(file.toPath(), data);
        return file;
    }

    public void testAttachments() throws Exception {
        var props = System.getProperties();
        props.setProperty("mail.smtp.host", "localhost");
        props.setProperty("mail.smtp.port", String.valueOf(wiser.getPort()));
        var session = Session.getInstance(props);

        var baseMsg = new MimeMessage(session);
        var bp1 = new MimeBodyPart();
        bp1.setHeader("Content-Type", "text/plain");
        bp1.setContent("Hello World!!!", "text/plain; charset=\"ISO-8859-1\"");

        var fileAttachment = new FileDataSource(bigFile);

        // Can't test if file not found
        assertTrue(fileAttachment.getFile().exists());

        // Attach the file
        var bp2 = new MimeBodyPart();
        bp2.setDataHandler(new DataHandler(fileAttachment));
        bp2.setFileName(fileAttachment.getName());

        Multipart multipart = new MimeMultipart();
        multipart.addBodyPart(bp1);
        multipart.addBodyPart(bp2);

        baseMsg.setFrom(new InternetAddress("Ted <ted@home.com>"));
        baseMsg.setRecipient(Message.RecipientType.TO, new InternetAddress("success@example.org"));
        baseMsg.setSubject("Test Big attached file message");
        baseMsg.setContent(multipart);
        baseMsg.saveChanges();

        try (Transport transport = new SMTPTransport(session, new URLName("smtp://localhost:" + wiser.getPort()))) {
            transport.connect();
            transport.sendMessage(baseMsg, new Address[]{new InternetAddress("success@example.org")});
        }

        final var compareFile = File.createTempFile("attached", ".tmp");
        try (var msg = wiser.getMessages().get(0)) {

            assertEquals(1, wiser.getMessages().size());
            assertEquals("success@example.org", msg.getEnvelopeReceiver());
            var fos = new FileOutputStream(compareFile);
            ((MimeMultipart) msg.getMimeMessage().getContent()).getBodyPart(1).getDataHandler().writeTo(fos);
            fos.close();
            assertEquals(-1L, Files.mismatch(bigFile.toPath(), compareFile.toPath()));

        } finally {
            compareFile.deleteOnExit();
        }
    }
}
