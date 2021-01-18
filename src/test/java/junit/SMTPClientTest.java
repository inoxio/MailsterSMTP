package junit;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Properties;
import java.util.Random;

import jakarta.activation.DataHandler;
import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.util.ByteArrayDataSource;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.util.Client;
import junit.util.SocketUtils;
import wiser.Wiser;

/**
 * This class serves as a test case for both Wiser (since it is used
 * internally here) as well as harder to reach code within the SMTP
 * server that tests a roundtrip message through the DATA portion
 * of the SMTP spec.
 *
 * @author Jon Stevens
 * @author Jeff Schnitzer
 * @author De Oliveira Edouard &lt;doe_wanted@yahoo.fr&gt;
 * @author Ville Skyttä (contributed some encoding tests)
 */
public class SMTPClientTest extends TestCase {

    private Wiser wiser;
    private Session session;

    public SMTPClientTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(SMTPClientTest.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        wiser = new Wiser(SocketUtils.findAvailableTcpPort());
        wiser.start();

        var props = new Properties();
        props.setProperty("mail.smtp.host", "localhost");
        props.setProperty("mail.smtp.port", Integer.toString(wiser.getPort()));
        session = Session.getInstance(props);
    }

    @Override
    protected void tearDown() throws Exception {
        this.wiser.stop();
        super.tearDown();
    }

    public void testMultipleRecipients() throws Exception {

        // given
        var message = new MimeMessage(this.session);
        message.addRecipient(Message.RecipientType.TO, new InternetAddress("anyone@anywhere.com"));
        message.addRecipient(Message.RecipientType.TO, new InternetAddress("anyone2@anywhere.com"));
        message.setFrom(new InternetAddress("someone@somewhereelse.com"));
        message.setSubject("barf");
        message.setText("body");

        // when
        Transport.send(message);

        // then
        assertEquals(2, this.wiser.getMessages().size());
    }

    public void testLargeMessage() throws Exception {

        // given
        var message = new MimeMessage(this.session);
        message.addRecipient(Message.RecipientType.TO, new InternetAddress("anyone@anywhere.com"));
        message.addRecipient(Message.RecipientType.TO, new InternetAddress("anyone2@anywhere.com"));
        message.setFrom(new InternetAddress("someone@somewhereelse.com"));
        message.setSubject("barf");
        message.setText("bodyalksdjflkasldfkasjldfkjalskdfjlaskjdflaksdjflkjasdlfkjl");

        // when
        Transport.send(message);

        // then
        assertEquals(2, this.wiser.getMessages().size());
        assertEquals("barf", this.wiser.getMessages().get(0).getMimeMessage().getSubject());
        assertEquals("barf", this.wiser.getMessages().get(1).getMimeMessage().getSubject());
    }

    public void testUtf8EightBitMessage() throws Exception {

        // given
        var body = "\u00a4uro ma\u00f1ana";
        var message = new MimeMessage(this.session);
        message.addRecipient(Message.RecipientType.TO, new InternetAddress("anyone@anywhere.com"));
        message.setFrom(new InternetAddress("someone@somewhereelse.com"));
        message.setSubject("hello");
        message.setText(body, "UTF-8");
        message.setHeader("Content-Transfer-Encoding", "8bit");

        // when
        Transport.send(message);

        // then
        assertEquals(body, this.wiser.getMessages().get(0).getMimeMessage().getContent());
    }

    public void testUtf16EightBitMessage() throws Exception {

        // given
        var body = "\u3042\u3044\u3046\u3048\u304a";
        var message = new MimeMessage(this.session);
        message.addRecipient(Message.RecipientType.TO, new InternetAddress("anyone@anywhere.com"));
        message.setFrom(new InternetAddress("someone@somewhereelse.com"));
        message.setSubject("hello");
        message.setText(body, "UTF-16");
        message.setHeader("Content-Transfer-Encoding", "8bit");

        // when
        Transport.send(message);

        // then
        assertEquals(body, this.wiser.getMessages().get(0).getMimeMessage().getContent());
    }

    public void testIso88591EightBitMessage() throws Exception {

        // given
        var body = "ma\u00f1ana";    // spanish ene (ie, n with diacritical tilde)
        var message = new MimeMessage(this.session);
        message.addRecipient(Message.RecipientType.TO, new InternetAddress("anyone@anywhere.com"));
        message.setFrom(new InternetAddress("someone@somewhereelse.com"));
        message.setSubject("hello");
        message.setText(body, "ISO-8859-1");
        message.setHeader("Content-Transfer-Encoding", "8bit");

        // when
        Transport.send(message);

        // then
        assertEquals(body, this.wiser.getMessages().get(0).getMimeMessage().getContent());
    }

    public void testIso885915EightBitMessage() throws Exception {

        // given
        var body = "\0xa4uro";    // should be the euro symbol
        var message = new MimeMessage(this.session);
        message.addRecipient(Message.RecipientType.TO, new InternetAddress("anyone@anywhere.com"));
        message.setFrom(new InternetAddress("someone@somewhereelse.com"));
        message.setSubject("hello");
        message.setText(body, "ISO-8859-15");
        message.setHeader("Content-Transfer-Encoding", "8bit");

        // when
        Transport.send(message);

        // then
        assertEquals(body, this.wiser.getMessages().get(0).getMimeMessage().getContent());
    }

    public void testIso2022JPEightBitMessage() throws Exception {

        // given
        var body = "\u3042\u3044\u3046\u3048\u304a"; // some Japanese letters
        var message = new MimeMessage(this.session);
        message.addRecipient(Message.RecipientType.TO, new InternetAddress("anyone@anywhere.com"));
        message.setFrom(new InternetAddress("someone@somewhereelse.com"));
        message.setSubject("hello");
        message.setText(body, "iso-2022-jp");
        message.setHeader("Content-Transfer-Encoding", "8bit");

        // when
        Transport.send(message);

        // then
        assertEquals(body, this.wiser.getMessages().get(0).getMimeMessage().getContent());
    }

    public void testPreservingCRLF() throws Exception {

        // given
        var body = "\n\nKeep these pesky carriage returns\n\n";

        // when
        testCRLFEncodingMessage(body);

        // then
        Thread.sleep(500);
        var received = this.wiser.getMessages().get(0).getMimeMessage().getContent().toString();
        assertEquals(body, received);
    }

    public void testPreservingCRLFHeavily() throws Exception {

        // given
        var body = "\r\n\r\nKeep these\r\npesky\r\n\r\ncarriage returns\r\n";

        // when
        testCRLFEncodingMessage(body);

        // then
        Thread.sleep(500);
        var received = this.wiser.getMessages().get(0).getMimeMessage().getContent().toString();
        assertEquals(body, received);
    }

    private void testCRLFEncodingMessage(String body) throws Exception {
        try (var client = new Client("localhost", wiser.getPort())) {

            client.expect("220");

            client.send("HELO foo.com");
            client.expect("250");

            client.send("MAIL FROM: someone@somewhereelse.com");
            client.expect("250");

            client.send("RCPT TO: anyone@anywhere.com");
            client.expect("250");

            client.send("DATA");
            client.expect("354 End data with <CR><LF>.<CR><LF>");

            client.send(new ByteArrayInputStream(("Subject: hello\n\n" + body).getBytes(StandardCharsets.ISO_8859_1)));
            client.expect("250");

            client.send("QUIT");
            client.expect("221 Bye");
        }
    }

    public void testBinaryEightBitMessage() throws Exception {
        var body = new byte[64];
        new Random().nextBytes(body);

        var message = new MimeMessage(this.session);
        message.addRecipient(Message.RecipientType.TO, new InternetAddress("anyone@anywhere.com"));
        message.setFrom(new InternetAddress("someone@somewhereelse.com"));
        message.setSubject("hello");
        message.setHeader("Content-Transfer-Encoding", "8bit");
        message.setDataHandler(new DataHandler(new ByteArrayDataSource(body, "application/octet-stream")));

        Transport.send(message);

        var in = this.wiser.getMessages().get(0).getMimeMessage().getInputStream();
        var tmp = new ByteArrayOutputStream();
        var buf = new byte[64];
        int n;
        while ((n = in.read(buf)) != -1) {
            tmp.write(buf, 0, n);
        }
        in.close();

        assertTrue(Arrays.equals(body, tmp.toByteArray()));
    }
}
