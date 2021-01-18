package junit;

import java.io.File;
import java.io.FileOutputStream;

import org.mailster.smtp.util.SharedTmpFileInputStream;

import junit.framework.TestCase;
import wiser.WiserMessage;

/**
 * This class tests that {@link SharedTmpFileInputStream} will
 * handle private streams independently and will delete temporary
 * files when all references are closed.
 *
 * @author De Oliveira Edouard &lt;doe_wanted@yahoo.fr&gt;
 */
public class TempFilesTest extends TestCase {

    public void testTmpFileStreams() throws Exception {
        var f = new File("file.tmp");
        var fos = new FileOutputStream(f);
        byte data[] = "Hello this is test data\r\n".getBytes();
        fos.write(data);
        fos.flush();
        fos.close();

        assertTrue(f.exists());

        var st = new SharedTmpFileInputStream(f);
        var in1 = st.newStream(0, -1);
        var in2 = st.newStream(0, -1);
        var in3 = st.newStream(2, -1);

        // test basic access to stream
        assertEquals(data[0], st.read());
        assertEquals(data[1], st.read());

        // close master stream and test if the file is deleted
        st.close();
        assertTrue(f.exists());

        // test independency between streams
        assertEquals(data[0], in2.read());
        var l = new byte[3];
        assertEquals(3, in2.read(l));

        for (var i = 0; i < 3; i++) {
            assertEquals(data[i + 1], l[i]);
        }

        assertEquals(data[0], in1.read());

        // test reference count
        in2.close();
        assertTrue(f.exists());

        // Test the offset
        assertEquals(data[2], in3.read());
        in3.close();
        assertTrue(f.exists());

        // Test if file is finally closed when no more references are open
        var msg = new WiserMessage("sender", "receiver", in1);
        msg.close();
        assertFalse(f.exists());
    }
}
