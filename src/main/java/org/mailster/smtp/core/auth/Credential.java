package org.mailster.smtp.core.auth;

/**
 * Holds the identity of a logged in user.
 *
 * @author De Oliveira Edouard &lt;doe_wanted@yahoo.fr&gt;
 */
public class Credential {

    private String id;

    public Credential(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
