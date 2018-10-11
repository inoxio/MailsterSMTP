package junit.util;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;


/**
 * A crude telnet client that can be used to send SMTP messages and test
 * the responses.
 *
 * @author De Oliveira Edouard &lt;doe_wanted@yahoo.fr&gt;
 * @author Jeff Schnitzer
 * @author Jon Stevens
 */
public class Client implements Closeable {

    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;

    /**
     * Establishes a connection to host and port.
     */
    public Client(String host, int port) throws IOException {
        socket = new Socket(host, port);
        writer = new PrintWriter(socket.getOutputStream(), true);
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    /**
     * Sends a message to the server, ie "HELO foo.example.com". A newline will
     * be appended to the message.
     */
    public void send(String msg) {
        // Force \r\n since println() behaves differently on different platforms
        writer.print(msg + "\r\n");
        writer.flush();
    }

    /**
     * Throws an exception if the response does not start with
     * the specified string.
     */
    public void expect(String expect) throws Exception {
        String response = this.readResponse();
        if (!response.startsWith(expect)) {
            throw new Exception("Got: " + response + " Expected: " + expect);
        }
    }

    /**
     * Get the complete response, including a multiline response.
     * Newlines are included.
     */
    private String readResponse() throws Exception {
        StringBuilder builder = new StringBuilder();
        boolean done = false;
        while (!done) {
            String line = this.reader.readLine();

            if (line == null) {
                break;
            }

            if (line.charAt(3) != '-') {
                done = true;
            }

            builder.append(line);
            builder.append('\n');
        }

        return builder.toString();
    }

    @Override
    public void close() throws IOException {
        socket.close();
    }

}
