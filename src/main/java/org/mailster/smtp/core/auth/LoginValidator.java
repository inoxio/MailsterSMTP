package org.mailster.smtp.core.auth;

/**
 * Use this when your authentication scheme uses a username and a password.
 *
 * @author De Oliveira Edouard &lt;doe_wanted@yahoo.fr&gt;
 * @author Marco Trevisan &lt;mrctrevisan@yahoo.it&gt;
 */
public interface LoginValidator {

    void login(final String username, final String password) throws LoginFailedException;
}
