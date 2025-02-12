package org.mailster.smtp.core.auth;

/**
 * Exception expected to be thrown by a validator (i.e LoginValidator)
 *
 * @author Marco Trevisan &lt;mrctrevisan@yahoo.it&gt;
 * @author De Oliveira Edouard &lt;doe_wanted@yahoo.fr&gt;
 */
public class LoginFailedException extends Exception {

    private static final long serialVersionUID = -2568432389605367270L;

    public LoginFailedException() {
        super("Authentication failed");
    }

    public LoginFailedException(String msg) {
        super(msg);
    }
}
