package examples;

import java.io.InputStream;

import org.mailster.smtp.SMTPServer;
import org.mailster.smtp.api.MessageListenerAdapter;
import org.mailster.smtp.api.handler.SessionContext;

/**
 * @author De Oliveira Edouard &lt;doe_wanted@yahoo.fr&gt;
 */
public class BasicServer {

    public static void main(String[] args) {
        var server = new SMTPServer(new MessageListenerAdapter() {
            @Override
            public void deliver(SessionContext ctx, String from, String recipient, InputStream data) {
                System.out.println("New message received");
            }
        });
		/*
		server.setAuthenticationHandlerFactory(new AllSchemesAuthenticationHandler(new LoginValidator() {
			public void login(String username, String password)
					throws LoginFailedException {
				System.out.println("username="+username);
				System.out.println("password="+password);
			}
		}));*/
        server.start();
    }
}