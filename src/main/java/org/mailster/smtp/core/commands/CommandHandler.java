package org.mailster.smtp.core.commands;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.mina.core.session.IoSession;
import org.mailster.smtp.core.SMTPConnectionHandler;
import org.mailster.smtp.core.SMTPContext;
import org.mailster.smtp.core.commands.impl.AuthCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class manages execution of a SMTP command.
 *
 * @author Jon Stevens
 * @author Edouard De Oliveira &lt;doe_wanted@yahoo.fr&gt;
 */
public class CommandHandler {

    private static final Logger LOG = LoggerFactory.getLogger(CommandHandler.class);
    private Map<String, Command> commandMap = new HashMap<>();

    /**
     * Populates a default set of commands based with the BuiltinCommandRegistry commands.
     */
    public CommandHandler() {
        for (var entry : BuiltinCommandRegistry.values()) {
            addCommand(entry.getCommand());
        }
    }

    /**
     * Adds a new command to the map.
     *
     * @param cmd the command to add
     */
    public void addCommand(Command cmd) {
        LOG.debug("Added command {}", cmd.getName());

        if (cmd instanceof AbstractCommand) {
            ((AbstractCommand) cmd).setCommandHandler(this);
        }

        this.commandMap.put(cmd.getName(), cmd);
    }

    /**
     * Does the map contain the named command?
     *
     * @return true if the command exists
     */
    public boolean containsCommand(String cmd) {
        return this.commandMap.containsKey(cmd);
    }

    /**
     * Calls the execute method on a command.
     */
    public void handleCommand(String cmdString, IoSession session, SMTPContext ctx) throws IOException {
        try {
            var cmd = getCommandFromString(cmdString);
            cmd.execute(cmdString, session, ctx);
        } catch (CommandException e) {
            SMTPConnectionHandler.sendResponse(session, "500 " + e.getMessage());
        }
    }

    /**
     * Executes an auth command.
     */
    public void handleAuthChallenge(String commandString, IoSession session, SMTPContext ctx) throws SocketTimeoutException, IOException {
        var cmd = this.commandMap.get(AuthCommand.VERB);
        cmd.execute(commandString, session, ctx);
    }

    /**
     * Given a string, find the Command object.
     *
     * @return The command object.
     */
    public Command getCommandFromString(String cmdString) throws UnknownCommandException, InvalidCommandNameException {
        var verb = toVerb(cmdString);

        var cmd = this.commandMap.get(verb);
        if (cmd == null) {
            throw new UnknownCommandException("Command not implemented");
        }

        return cmd;
    }

    private String toVerb(String cmd) throws InvalidCommandNameException {
        if (cmd == null || cmd.length() < 4) {
            throw new InvalidCommandNameException("Syntax error");
        }

        var st = new StringTokenizer(cmd);

        if (!st.hasMoreTokens()) {
            throw new InvalidCommandNameException("Syntax error");
        }

        return st.nextToken().toUpperCase();
    }
}