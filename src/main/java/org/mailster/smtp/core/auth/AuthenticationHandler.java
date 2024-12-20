package org.mailster.smtp.core.auth;

import java.util.List;

import org.mailster.smtp.core.SMTPContext;

/**
 * The interface that enables challenge-response communication necessary for SMTP AUTH.<p>
 * Since the authentication process can be stateful, an instance of this class can be stateful too.<br>
 * Do not share a single instance of this interface if you don't explicitly need to do so.
 *
 * @author Marco Trevisan &lt;mrctrevisan@yahoo.it&gt;
 * @author De Oliveira Edouard &lt;doe_wanted@yahoo.fr&gt;
 */
public interface AuthenticationHandler {

    /**
     * If your handler supports RFC 2554 at some degree, then it must return all the supported mechanisms here. <br>
     * The order you use to populate the list will be preserved in the output of the EHLO command. <br>
     * If your handler does not support RFC 2554 at all, return an empty list.
     *
     * @return the supported authentication mechanisms as List.
     */
    List<String> getAuthenticationMechanisms();

    /**
     * Initially called using an input string in the RFC2554 form: "AUTH &lt;mechanism&gt; [initial-response]". <br>
     * This method must provide the correct reply (by filling the <code>response</code> parameter) at each <code>clientInput</code>.
     * <p>
     * Depending on the authentication mechanism, the handshaking process may require
     * many request-response passes. This method will return <code>true</code> only when the authentication process is finished <br>
     *
     * @param clientInput The client's input.
     * @param response    a buffer filled with your response to the client input.
     * @param ctx         the connection context filled with the credential of the user if authentication succeeds.
     * @return <code>true</code> if the authentication process is finished, <code>false</code> otherwise.
     * @throws LoginFailedException if authentication fails.
     */
    boolean auth(String clientInput, StringBuilder response, SMTPContext ctx) throws LoginFailedException;

    /**
     * Since a so-designed handler has its own state, it seems reasonable to enable resetting
     * its state. This can be done, for example, after a "*" client response during the AUTH command
     * processing.
     */
    void resetState();
}
