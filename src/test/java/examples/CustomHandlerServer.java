package examples;

import java.io.InputStream;

import org.mailster.smtp.SMTPServer;
import org.mailster.smtp.api.MessageListenerAdapter;
import org.mailster.smtp.api.handler.SessionContext;

/**
 * @author De Oliveira Edouard &lt;doe_wanted@yahoo.fr&gt;
 */
public class CustomHandlerServer {

    public static void main(String[] args) {
        var server = new SMTPServer(new MessageListenerAdapter() {
            @Override
            public void deliver(SessionContext ctx, String from, String recipient, InputStream data) {
                System.out.println("New message received");
            }
        });

        server.getDeliveryHandlerFactory().setDeliveryHandlerImplClass(CustomDeliveryHandlerImpl.class);

        // Optionally you can set an auth factory
        // server.setAuthenticationHandlerFactory(...);
        server.start();
    }
}