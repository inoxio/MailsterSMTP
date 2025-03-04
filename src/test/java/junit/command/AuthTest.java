package junit.command;

import org.mailster.smtp.AllSchemesAuthenticationHandler;
import org.mailster.smtp.core.auth.LoginFailedException;
import org.mailster.smtp.core.auth.LoginValidator;
import org.mailster.smtp.util.Base64;

import junit.util.ServerTestCase;

/**
 * @author Marco Trevisan &lt;mrctrevisan@yahoo.it&gt;
 * @author De Oliveira Edouard &lt;doe_wanted@yahoo.fr&gt;
 */
public class AuthTest extends ServerTestCase {

    private static final String REQUIRED_USERNAME = "myUserName";
    private static final String REQUIRED_PASSWORD = "mySecret01";

    private LoginValidator validator = (username, password) -> {
        if (!username.equals(REQUIRED_USERNAME) || !password.equals(REQUIRED_PASSWORD)) {
            throw new LoginFailedException();
        }
    };

    public AuthTest(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        wiser.getServer().setAuthenticationHandlerFactory(new AllSchemesAuthenticationHandler(validator));
    }

    /**
     * Test method for AUTH PLAIN.
     * The sequence under test is as follows:
     * <ol>
     * <li>HELO test</li>
     * <li>User starts AUTH PLAIN</li>
     * <li>User sends username+password</li>
     * <li>We expect login to be successful. Also the Base64 transformations are tested.</li>
     * <li>User issues another AUTH command</li>
     * <li>We expect an error message</li>
     * </ol>
     */
    public void testAuthPlain() throws Exception {
        expect("220");

        send("HELO foo.com");
        expect("250");

        send("AUTH PLAIN");
        expect("334");

        var authString = new String(new byte[]{0}) + REQUIRED_USERNAME + new String(
                new byte[]{0}) + REQUIRED_PASSWORD;

        var enc_authString = Base64.encodeToString(authString.getBytes(), false);
        send(enc_authString);
        expect("235");

        send("AUTH PLAIN");
        expect("503");
    }

    /**
     * Test method for AUTH LOGIN.
     * The sequence under test is as follows:
     * <ol>
     * <li>HELO test</li>
     * <li>User starts AUTH LOGIN</li>
     * <li>User sends username</li>
     * <li>User cancels authentication by sending "*"</li>
     * <li>User restarts AUTH LOGIN</li>
     * <li>User sends username</li>
     * <li>User sends password</li>
     * <li>We expect login to be successful. Also the Base64 transformations are tested.</li>
     * <li>User issues another AUTH command</li>
     * <li>We expect an error message</li>
     * </ol>
     */
    public void testAuthLogin() throws Exception {
        expect("220");

        send("HELO foo.com");
        expect("250");

        send("AUTH LOGIN");
        expect("334");

        var enc_username = Base64.encodeToString(REQUIRED_USERNAME.getBytes(), false);
        send(enc_username);
        expect("334");

        send("*");
        expect("501");

        send("AUTH LOGIN");
        expect("334");

        send(enc_username);
        expect("334");

        var enc_pwd = Base64.encodeToString(REQUIRED_PASSWORD.getBytes(), false);
        send(enc_pwd);
        expect("235");

        send("AUTH LOGIN");
        expect("503");
    }
}
