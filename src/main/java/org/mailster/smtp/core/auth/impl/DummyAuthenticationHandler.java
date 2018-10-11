package org.mailster.smtp.core.auth.impl;

import java.util.ArrayList;
import java.util.List;

import org.mailster.smtp.core.SMTPContext;
import org.mailster.smtp.core.auth.AuthenticationHandler;

/**
 * Implements a dummy AUTH mechanism.<br />
 * Will always allow to login without asking for any parameter.
 * This is a test purpose handler only and should not be used
 * in production environemnts.
 *
 * @author De Oliveira Edouard &lt;doe_wanted@yahoo.fr&gt;
 * @author Jeff Schnitzer
 */
public class DummyAuthenticationHandler implements AuthenticationHandler {

    @Override
    public List<String> getAuthenticationMechanisms() {
        return new ArrayList<>();
    }

    @Override
    public boolean auth(String clientInput, StringBuilder response, SMTPContext ctx) {
        return true;
    }

    @Override
    public void resetState() {
    }
}