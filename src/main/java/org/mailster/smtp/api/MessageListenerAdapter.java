package org.mailster.smtp.api;

import java.io.IOException;
import java.io.InputStream;

import org.mailster.smtp.api.handler.SessionContext;

/**
 * An adapter to the {@link MessageListener} interface.
 *
 * @author De Oliveira Edouard &lt;doe_wanted@yahoo.fr&gt;
 */
public class MessageListenerAdapter implements MessageListener {

    @Override
    public boolean accept(SessionContext ctx, String from, String recipient) {
        return true;
    }

    @Override
    public void deliver(SessionContext ctx, String from, String recipient, InputStream data) {
    }
}
