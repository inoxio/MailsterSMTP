package org.mailster.smtp.core.auth.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;

import org.mailster.smtp.core.SMTPContext;
import org.mailster.smtp.core.auth.AuthenticationHandler;
import org.mailster.smtp.core.auth.Credential;
import org.mailster.smtp.core.auth.LoginFailedException;
import org.mailster.smtp.core.auth.LoginValidator;
import org.mailster.smtp.util.Base64;

/**
 * Implements the SMTP AUTH PLAIN mechanism.<br>
 * You are only required to plug your LoginValidator implementation
 * for username and password validation to take effect.
 *
 * @author Marco Trevisan <mrctrevisan@yahoo.it>
 * @author De Oliveira Edouard &lt;doe_wanted@yahoo.fr&gt;
 */
public class PlainAuthenticationHandler implements AuthenticationHandler {

    private LoginValidator helper;

    private List<String> authentificationMechanisms;

    public PlainAuthenticationHandler(LoginValidator helper) {
        this.helper = helper;

        authentificationMechanisms = new ArrayList<>(1);
        authentificationMechanisms.add("PLAIN");
    }

    @Override
    public List<String> getAuthenticationMechanisms() {
        return Collections.unmodifiableList(authentificationMechanisms);
    }

    @Override
    public boolean auth(String clientInput, StringBuilder response, SMTPContext ctx) throws LoginFailedException {
        var stk = new StringTokenizer(clientInput);
        var secret = stk.nextToken();
        if (secret.trim().equalsIgnoreCase("AUTH")) {
            // Let's read the RFC2554 "initial-response" parameter
            // The line could be in the form of "AUTH PLAIN <base64Secret>"
            if (!stk.nextToken().trim().equalsIgnoreCase("PLAIN")) {
                // Mechanism mismatch
                response.append("504 AUTH mechanism mismatch");
                return true;
            }
            if (stk.hasMoreTokens()) {
                // the client submitted an initial response
                secret = stk.nextToken();
            } else {
                // the client did not submit an initial response
                response.append("334 Ok");
                return false;
            }
        }

        var decodedSecret = Base64.decode(secret);
        if (decodedSecret == null) {
            throw new LoginFailedException();
        }

        var usernameStop = -1;
        for (var i = 1; i < decodedSecret.length && usernameStop < 0; i++) {
            if (decodedSecret[i] == 0) {
                usernameStop = i;
            }
        }

        var username = new String(decodedSecret, 1, usernameStop - 1);
        var password = new String(decodedSecret, usernameStop + 1, decodedSecret.length - usernameStop - 1);
        try {
            helper.login(username, password);
            resetState();
        } catch (LoginFailedException lfe) {
            resetState();
            throw lfe;
        }

        ctx.setCredential(new Credential(username));
        return true;
    }

    @Override
    public void resetState() {
    }
}
