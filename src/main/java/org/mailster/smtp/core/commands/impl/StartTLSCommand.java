package org.mailster.smtp.core.commands.impl;

import java.io.IOException;

import org.apache.mina.core.future.DefaultWriteFuture;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.DefaultWriteRequest;
import org.apache.mina.filter.ssl.DisableEncryptWriteRequest;
import org.apache.mina.filter.ssl.SslFilter;
import org.mailster.smtp.core.SMTPContext;
import org.mailster.smtp.core.commands.AbstractCommand;
import org.mailster.smtp.util.DummySSLSocketFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The STARTLS command implementation.
 *
 * @author De Oliveira Edouard &lt;doe_wanted@yahoo.fr&gt;
 */
public class StartTLSCommand extends AbstractCommand {

    private static final Logger LOG = LoggerFactory.getLogger(StartTLSCommand.class);

    private static SslFilter sslFilter;

    static {
        try {
            var socketFactory = new DummySSLSocketFactory();
            sslFilter = new SslFilter(socketFactory.getSSLContext());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public StartTLSCommand() {
        super("STARTTLS", "The STARTTLS command starts a secured connection for the current\nSMTP session");
    }

    /**
     * Ability to override the SSLFilter
     */
    public static void setSSLFilter(SslFilter filter) {
        if (filter == null) {
            throw new IllegalArgumentException("filter argument can't be null");
        }

        sslFilter = filter;
    }

    @Override
    public void execute(String commandString, IoSession ioSession, SMTPContext ctx) throws IOException {
        if (commandString.trim().indexOf(" ") > 0) {
            sendResponse(ioSession, "501 Syntax error (no parameters allowed)");
            return;
        }

        try {
            if (ioSession.isSecured() || ioSession.getFilterChain().contains(SslFilter.class)) {
                sendResponse(ioSession, "454 TLS not available due to temporary reason: TLS already active");
                return;
            }

            // Insert SSLFilter to get ready for handshaking
            ioSession.getFilterChain().addFirst("SSLfilter", sslFilter);

            // Write StartTLSResponse which won't be encrypted.
            // Since MINA 2.2 SslFilter.DISABLE_ENCRYPTION_ONCE is gone; a write request
            // implementing DisableEncryptWriteRequest is passed through the SslFilter unencrypted.
            sendUnencryptedResponse(ioSession, "220 Ready to start TLS");

            ctx.getSMTPState().resetAll(); // clean state
            ctx.getDeliveryHandler().resetMessageState();
        } catch (Exception e) {
            LOG.debug("startTLS() failed: {}", e.getMessage());
            LOG.trace("startTLS() failure stack trace", e);
        }
    }

    private static void sendUnencryptedResponse(IoSession ioSession, String response) {
        LOG.debug("S: {}", response);
        var writeRequest = new UnencryptedWriteRequest(response, new DefaultWriteFuture(ioSession));
        ioSession.getFilterChain().fireFilterWrite(writeRequest);
    }

    /**
     * Write request that bypasses the {@link SslFilter} (replacement for the removed
     * {@code SslFilter.DISABLE_ENCRYPTION_ONCE} attribute of MINA 2.1).
     */
    private static final class UnencryptedWriteRequest extends DefaultWriteRequest implements DisableEncryptWriteRequest {

        UnencryptedWriteRequest(Object message, DefaultWriteFuture future) {
            super(message, future);
        }
    }
}
