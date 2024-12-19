package org.mailster.smtp.core.auth;

/**
 * Factory that creates {@link AuthenticationHandler}.
 *
 * @author Marco Trevisan &lt;mrctrevisan@yahoo.it&gt;
 * @author De Oliveira Edouard &lt;doe_wanted@yahoo.fr&gt;
 */
public interface AuthenticationHandlerFactory {

    AuthenticationHandler create();
}
