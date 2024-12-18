package wiser;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.mailster.smtp.api.MessageListener;
import org.mailster.smtp.api.handler.SessionContext;

public class InMemoryMessageDelivery implements MessageListener {

    private final List<WiserMessage> messages = Collections.synchronizedList(new ArrayList<>());

    /**
     * Always accept everything
     */
    @Override
    public boolean accept(SessionContext ctx, String from, String recipient) {
        return true;
    }

    /**
     * Cache the messages in memory. Now avoids unnecessary memory copying.
     */
    @Override
    public void deliver(SessionContext ctx, String from, String recipient, InputStream data) throws IOException {
        messages.add(new WiserMessage(from, recipient, data));
    }

    public List<WiserMessage> getMessages() {
        return messages;
    }
}
