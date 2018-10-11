package junit;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.mail.Address;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.URLName;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import junit.framework.TestCase;
import junit.util.SocketUtils;

import com.sun.mail.smtp.SMTPTransport;

import wiser.Wiser;
import wiser.WiserMessage;

/**
 * This class tests the transfer speed of emails that carry
 * attached files.
 *
 * @author De Oliveira Edouard &lt;doe_wanted@yahoo.fr&gt;
 */
public class BigAttachmentTest extends TestCase {

    private static final int BUFFER_SIZE = 32768;
    private static final String BIGFILE_PATH = System.getProperty("java.home")
            .replace("\\", "/") + "/jmods/java.rmi.jmod";

    private Wiser wiser;

    public BigAttachmentTest(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        wiser = new Wiser(SocketUtils.findAvailableTcpPort());
        wiser.setReceiveBufferSize(BUFFER_SIZE);
        wiser.start();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        wiser.stop();
    }

    public void testAttachments() throws Exception {
        Properties props = System.getProperties();
        props.setProperty("mail.smtp.host", "localhost");
        props.setProperty("mail.smtp.port", String.valueOf(wiser.getPort()));
        Session session = Session.getInstance(props);

        MimeMessage baseMsg = new MimeMessage(session);
        MimeBodyPart bp1 = new MimeBodyPart();
        bp1.setHeader("Content-Type", "text/plain");
        bp1.setContent("Hello World!!!", "text/plain; charset=\"ISO-8859-1\"");

        FileDataSource fileAttachment = new FileDataSource(BIGFILE_PATH);

        // Can't test if file not found
        assertTrue(fileAttachment.getFile().exists());

        // Attach the file
        MimeBodyPart bp2 = new MimeBodyPart();
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
        try (WiserMessage msg = wiser.getMessages().get(0)) {

            assertEquals(1, wiser.getMessages().size());
            assertEquals("success@example.org", msg.getEnvelopeReceiver());
            FileOutputStream fos = new FileOutputStream(compareFile);
            ((MimeMultipart) msg.getMimeMessage().getContent()).getBodyPart(1).getDataHandler().writeTo(fos);
            fos.close();
            assertTrue(checkIntegrity(new File(BIGFILE_PATH), compareFile));

        } finally {
            compareFile.deleteOnExit();
        }
    }

    private boolean checkIntegrity(File src, File dest) throws IOException, NoSuchAlgorithmException {
        BufferedInputStream ins = new BufferedInputStream(new FileInputStream(src));
        BufferedInputStream ind = new BufferedInputStream(new FileInputStream(dest));
        MessageDigest md1 = MessageDigest.getInstance("MD5");
        MessageDigest md2 = MessageDigest.getInstance("MD5");

        int r = 0;
        byte[] buf1 = new byte[BUFFER_SIZE];
        byte[] buf2 = new byte[BUFFER_SIZE];

        while (r != -1) {
            r = ins.read(buf1);
            ind.read(buf2);

            md1.update(buf1);
            md2.update(buf2);
        }

        ins.close();
        ind.close();
        return MessageDigest.isEqual(md1.digest(), md2.digest());
    }
}