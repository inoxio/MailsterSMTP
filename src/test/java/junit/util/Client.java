package junit.util;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;


/**
 * A crude telnet client that can be used to send SMTP messages and test
 * the responses.
 *
 * @author De Oliveira Edouard &lt;doe_wanted@yahoo.fr&gt;
 * @author Jeff Schnitzer
 * @author Jon Stevens
 */
public class Client implements Closeable {

    private static final byte[] STOPWORD = new byte[]{13, 10, 46, 13, 10};

    private Socket socket;
    private BufferedReader reader;
    private OutputStream out;

    /**
     * Establishes a connection to host and port.
     */
    public Client(String host, int port) throws IOException {
        socket = new Socket(host, port);
        out = socket.getOutputStream();
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    /**
     * Sends a message to the server, ie "HELO foo.example.com". A newline will
     * be appended to the message.
     */
    public void send(String msg) throws IOException {
        out.write(msg.getBytes());
        out.write(13);
        out.write(10);
        out.flush();
    }

    public void send(InputStream inputStream) throws IOException {
        var data = new StopWordSafeInputStream(inputStream);
        int b;
        while((b = data.read()) != -1) {
            out.write(b);
        }
        out.write(STOPWORD);
    }

    /**
     * Throws an exception if the response does not start with
     * the specified string.
     */
    public void expect(String expect) throws Exception {
        var response = this.readResponse();
        if (!response.startsWith(expect)) {
            throw new Exception("Got: " + response + " Expected: " + expect);
        }
    }

    /**
     * Get the complete response, including a multiline response.
     * Newlines are included.
     */
    private String readResponse() throws Exception {
        var builder = new StringBuilder();
        var done = false;
        while (!done) {
            var line = this.reader.readLine();

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
