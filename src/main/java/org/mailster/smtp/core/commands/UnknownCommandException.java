package org.mailster.smtp.core.commands;

/**
 * @author De Oliveira Edouard &lt;doe_wanted@yahoo.fr&gt;
 * @author Ian McFarland &lt;ian@neo.com&gt;
 */
public class UnknownCommandException extends CommandException {

    private static final long serialVersionUID = 6579786559432851561L;

    public UnknownCommandException() {
        super();
    }

    public UnknownCommandException(String string) {
        super(string);
    }

    public UnknownCommandException(String string, Throwable throwable) {
        super(string, throwable);
    }

    public UnknownCommandException(Throwable throwable) {
        super(throwable);
    }
}
