package junit;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Date;
import java.util.Iterator;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import junit.framework.TestCase;
import junit.util.SocketUtils;

import org.mailster.smtp.api.MessageListener;
import org.mailster.smtp.api.MessageListenerAdapter;
import org.mailster.smtp.api.handler.SessionContext;

import wiser.Wiser;
import wiser.WiserMessage;

/**
 * @author De Oliveira Edouard &lt;doe_wanted@yahoo.fr&gt;
 */
public class WiserFailuresTest extends TestCase {

    private final static String FROM_ADDRESS = "from-addr@localhost";
    private final static String HOST_NAME = "localhost";
    private final static String TO_ADDRESS = "to-addr@localhost";
    private BufferedReader input;
    private PrintWriter output;
    private Wiser wiser;
    private Socket socket;

    private int counter;

    public WiserFailuresTest(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {

        super.setUp();
        wiser = new Wiser(SocketUtils.findAvailableTcpPort());
        wiser.start();

        socket = new Socket(HOST_NAME, wiser.getPort());
        input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        output = new PrintWriter(socket.getOutputStream(), true);

        counter = 0;
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        try {
            input.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            output.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            socket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            wiser.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * See http://sourceforge.net/tracker/index.php?func=detail&aid=1474700&group_id=78413&atid=553186 for discussion
     * about this bug
     */
    public void testMailFromAfterReset() throws IOException, MessagingException {
        assertConnect();
        sendExtendedHello();
        sendMailFrom();
        sendReceiptTo();
        sendReset();
        sendMailFrom();
        sendReceiptTo();
        sendDataStart();
        send("");
        send("Body");
        sendDataEnd();
        sendQuit();

        assertEquals(1, wiser.getMessages().size());
        var emailIter = wiser.getMessages().iterator();
        var email = emailIter.next();
        assertEquals("Body", email.getMimeMessage().getContent().toString());
    }

    /**
     * See http://sourceforge.net/tracker/index.php?func=detail&aid=1474700&group_id=78413&atid=553186 for discussion
     * about this bug
     */
    public void testMailFromWithInitialReset() throws IOException, MessagingException {
        assertConnect();
        sendReset();
        sendMailFrom();
        sendReceiptTo();
        sendDataStart();
        send("");
        send("Body");
        sendDataEnd();
        sendQuit();

        assertEquals(1, wiser.getMessages().size());
        var emailIter = wiser.getMessages().iterator();
        var email = emailIter.next();
        assertEquals("Body", email.getMimeMessage().getContent().toString());
    }

    public void testSendEncodedMessage() throws IOException, MessagingException {
        var body = "\u3042\u3044\u3046\u3048\u304a"; // some Japanese letters
        var charset = "iso-2022-jp";

        // when
        sendMessage("EncodedMessage", body, charset);

        // then
        assertEquals(1, wiser.getMessages().size());
        var emailIter = wiser.getMessages().iterator();
        var email = emailIter.next();
        assertEquals(body, email.getMimeMessage().getContent().toString());
    }

    public void testSendMessageWithCarriageReturn() throws IOException, MessagingException {
        var bodyWithCR = "\r\n\r\nKeep these\r\npesky\r\n\r\ncarriage returns\r\n";

        // when
        sendMessage("CRTest", bodyWithCR, null);

        // then
        assertEquals(1, wiser.getMessages().size());
        var emailIter = wiser.getMessages().iterator();
        var email = emailIter.next();
        var received = email.getMimeMessage().getContent().toString();

        // last \r\n will be treated as part of the data termination by javamail
        // Transport ... so we compare the body without the last 2 chars
        assertEquals(bodyWithCR.substring(0, bodyWithCR.length() - 2), received);
    }

    public void testListenersAndContextAttributes() throws MessagingException {

        var mimeMessages = new MimeMessage[2];
        var mailProps = getMailProperties(wiser.getPort());
        var session = Session.getInstance(mailProps, null);
        session.setDebug(true);

        mimeMessages[0] = createMessage(session, "Doodle1", "Bug1", null);
        mimeMessages[1] = createMessage(session, "Doodle2", "Bug2", null);

        wiser.getServer().getDeliveryHandlerFactory().addListener(new MessageListenerAdapter() {
            @Override
            public boolean accept(SessionContext ctx, String from, String recipient) {
                ctx.addAttribute("prop2", "prop2_value");
                return true;
            }
        });

        var transport = session.getTransport("smtp");
        transport.connect("localhost", wiser.getPort(), null, null);
        transport.sendMessage(mimeMessages[0], mimeMessages[0].getAllRecipients());
        transport.close();

        wiser.getServer().getDeliveryHandlerFactory().addListener(new MessageListener() {
            @Override
            public void deliver(SessionContext ctx, String from, String recipient, InputStream data) {
                counter++;
                assertEquals("prop1_value", (String) ctx.getAttribute("prop1"));
                assertEquals("prop2_value", (String) ctx.getAttribute("prop2"));
            }

            @Override
            public boolean accept(SessionContext ctx, String from, String recipient) {
                ctx.addAttribute("prop1", "prop1_value");
                return true;
            }
        });

        transport.connect("localhost", wiser.getPort(), null, null);
        transport.sendMessage(mimeMessages[1], mimeMessages[1].getAllRecipients());
        transport.close();

        assertEquals(1, counter);
        assertEquals(2, wiser.getMessages().size());
    }

    public void testSendTwoMessagesSameConnection() throws MessagingException {

        var mimeMessages = new MimeMessage[2];
        var mailProps = getMailProperties(wiser.getPort());
        var session = Session.getInstance(mailProps, null);
        session.setDebug(true);

        mimeMessages[0] = createMessage(session, "Doodle1", "Bug1", null);
        mimeMessages[1] = createMessage(session, "Doodle2", "Bug2", null);

        try (var transport = session.getTransport("smtp")) {
            transport.connect("localhost", wiser.getPort(), null, null);

            for (var mimeMessage : mimeMessages) {
                transport.sendMessage(mimeMessage, mimeMessage.getAllRecipients());
            }
        }
        assertEquals(2, wiser.getMessages().size());
    }

    public void testSendTwoMsgsWithLogin() throws MessagingException, IOException {

        var From = "sender@here.com";
        var To = "receiver@there.com";
        var Subject = "Test";
        var body = "Test Body";

        var session = Session.getInstance(getMailProperties(wiser.getPort()), null);
        Message msg = new MimeMessage(session);
        msg.setFrom(new InternetAddress(From));
        msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(To, false));
        msg.setSubject(Subject);

        msg.setText(body);
        msg.setHeader("X-Mailer", "here");
        msg.setSentDate(new Date());

        try (var transport = session.getTransport("smtp")) {
            transport.connect(HOST_NAME, wiser.getPort(), "ddd", "ddd");
            assertEquals(0, wiser.getMessages().size());
            transport.sendMessage(msg, InternetAddress.parse(To, false));
            assertEquals(1, wiser.getMessages().size());
            transport.sendMessage(msg, InternetAddress.parse("receivingagain@there.com", false));
            assertEquals(2, wiser.getMessages().size());
        }

        var emailIter = wiser.getMessages().iterator();
        var email = emailIter.next();
        var mime = email.getMimeMessage();
        assertEquals("Test", mime.getHeader("Subject")[0]);
        assertEquals("Test Body", mime.getContent().toString());
    }

    private Properties getMailProperties(int port) {
        var mailProps = new Properties();
        mailProps.setProperty("mail.smtp.host", "localhost");
        mailProps.setProperty("mail.smtp.port", "" + port);
        mailProps.setProperty("mail.smtp.sendpartial", "true");
        return mailProps;
    }

    private void sendMessage(String subject, String body, String charset) throws MessagingException {

        var mailProps = getMailProperties(wiser.getPort());
        var session = Session.getInstance(mailProps, null);

        var msg = createMessage(session, subject, body, charset);
        Transport.send(msg);
    }

    private MimeMessage createMessage(Session session, String subject, String body, String charset) throws MessagingException {

        var msg = new MimeMessage(session);
        msg.setFrom(new InternetAddress("sender@whatever.com"));
        msg.setSubject(subject);
        msg.setSentDate(new Date());
        if (charset != null) {
            msg.setText(body, charset);
            msg.setHeader("Content-Transfer-Encoding", "7bit");
        } else {
            msg.setText(body);
        }

        msg.setRecipient(Message.RecipientType.TO, new InternetAddress("receiver@home.com"));
        return msg;
    }

    private void assertConnect() throws IOException {
        var response = readInput();
        assertTrue(response, response.startsWith("220"));
    }

    private void sendDataEnd() throws IOException {
        send(".");
        var response = readInput();
        assertTrue(response, response.startsWith("250"));
    }

    private void sendDataStart() throws IOException {
        send("DATA");
        var response = readInput();
        assertTrue(response, response.startsWith("354"));
    }

    private void sendExtendedHello() throws IOException {
        send("EHLO " + WiserFailuresTest.HOST_NAME);
        var response = readInput();
        assertTrue(response, response.startsWith("250"));
    }

    private void sendMailFrom() throws IOException {
        send("MAIL FROM:<" + WiserFailuresTest.FROM_ADDRESS + ">");
        var response = readInput();
        assertTrue(response, response.startsWith("250"));
    }

    private void sendQuit() throws IOException {
        send("QUIT");
        var response = readInput();
        assertTrue(response, response.startsWith("221"));
    }

    private void sendReceiptTo() throws IOException {
        send("RCPT TO:<" + WiserFailuresTest.TO_ADDRESS + ">");
        var response = readInput();
        assertTrue(response, response.startsWith("250"));
    }

    private void sendReset() throws IOException {
        send("RSET");
        var response = readInput();
        assertTrue(response, response.startsWith("250"));
    }

    private void send(String msg) {
        // Force \r\n since println() behaves differently on different platforms
        output.print(msg + "\r\n");
        output.flush();
    }

    private String readInput() throws IOException {
        var sb = new StringBuilder();
        do {
            sb.append(input.readLine()).append("\n");
        } while (input.ready());

        return sb.toString();
    }
}
