package wiser;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;

/**
 * This class wraps a received message and provides
 * a way to generate a JavaMail MimeMessage from the data.
 *
 * @author Jon Stevens
 * @author De Oliveira Edouard &lt;doe_wanted@yahoo.fr&gt;
 */
public class WiserMessage implements Closeable {

    private String envelopeSender;
    private String envelopeReceiver;
    private InputStream stream;
    private MimeMessage message = null;

    public WiserMessage(String envelopeSender, String envelopeReceiver, InputStream stream) {
        this.envelopeSender = envelopeSender;
        this.envelopeReceiver = envelopeReceiver;
        this.stream = stream;
    }

    /**
     * Generate a JavaMail MimeMessage.
     */
    public synchronized MimeMessage getMimeMessage() throws MessagingException {
        if (this.message == null) {
            this.message = new MimeMessage(Session.getDefaultInstance(new Properties()), this.stream);
        }
        return this.message;
    }

    /**
     * Get's the RCPT TO:
     */
    public String getEnvelopeReceiver() {
        return this.envelopeReceiver;
    }

    /**
     * Get's the MAIL FROM:
     */
    public String getEnvelopeSender() {
        return this.envelopeSender;
    }

    @Override
    public void close() throws IOException {
        stream.close();
    }
}
