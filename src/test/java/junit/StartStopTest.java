package junit;

import junit.framework.TestCase;
import junit.util.SocketUtils;

import org.mailster.smtp.SMTPServer;

/**
 * This class attempts to quickly start/stop the server 10 times.
 * It makes sure that the socket bind address is correctly
 * shut down.
 *
 * @author De Oliveira Edouard &lt;doe_wanted@yahoo.fr&gt;
 */
public class StartStopTest extends TestCase {

    public StartStopTest(String name) {
        super(name);
    }

    public void testMultipleStartStop() {
        // given
        var counter = 0;

        // when
        for (var i = 0; i < 10; i++) {
            var server = new SMTPServer();
            server.setPort(SocketUtils.findAvailableTcpPort());

            server.start();
            server.stop();

            counter++;
        }

        // then
        assertEquals(10, counter);
    }

    public void testMultipleStartStopWithSameInstance() {
        var server = new SMTPServer();
        for (var i = 0; i < 10; i++) {
            server.setPort(SocketUtils.findAvailableTcpPort());
            server.start();
            server.stop();
        }
    }

    public void testShutdown() {
        var failed = false;
        var server = new SMTPServer();
        server.setPort(SocketUtils.findAvailableTcpPort());
        server.start();
        server.stop();
        server.shutdown();

        try {
            server.start();
        } catch (RuntimeException ex) {
            failed = true;
        }

        assertTrue(failed);
    }
}