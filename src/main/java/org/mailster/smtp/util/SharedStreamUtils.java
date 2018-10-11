package org.mailster.smtp.util;

import java.io.InputStream;

import javax.mail.util.SharedByteArrayInputStream;


/**
 * Shared streams utility methods.
 *
 * @author De Oliveira Edouard &lt;doe_wanted@yahoo.fr&gt;
 */
public class SharedStreamUtils {

    public final static char[] SMTP_TERMINATOR = {'\r', '\n', '.', '\r', '\n'};

    /**
     * Calls @link {@link #getPrivateInputStream(boolean, InputStream)}
     * with the useCopy parameter set to true.
     */
    public static InputStream getPrivateInputStream(InputStream data) {
        return getPrivateInputStream(true, data);
    }

    /**
     * Provides a private unstuffed {@link InputStream} for each invocation unless
     * <code>useCopy</code> is false in which case the <code>data</code> stream
     * is unstuffed and returned. Unstuffing is made by encapsulating the stream within
     * special streams.
     *
     * @see org.mailster.smtp.util.CharTerminatedInputStream
     * @see org.mailster.smtp.util.DotUnstuffingInputStream
     */
    public static InputStream getPrivateInputStream(boolean useCopy, InputStream data) {
        InputStream in = data;

        if (useCopy) {
            if (data instanceof SharedByteArrayInputStream) {
				in = ((SharedByteArrayInputStream) data).newStream(0, -1);
			} else if (data instanceof SharedTmpFileInputStream) {
				in = ((SharedTmpFileInputStream) data).newStream(0, -1);
			} else {
				throw new IllegalArgumentException("Unexpected data stream type : " + data.getClass().getName());
			}
        }

        in = new CharTerminatedInputStream(in, SMTP_TERMINATOR);
        in = new DotUnstuffingInputStream(in);

        return in;
    }
}
