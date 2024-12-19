package org.mailster.smtp.core.auth;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.mailster.smtp.core.SMTPContext;

/**
 * This handler makes it possible to sum the capabilities of two or more handlers types.<br>
 * Hence, you can design a single handler for each authentication mechanism and decide how many
 * authentication mechanisms to support by simply plugging them here.
 *
 * @author Marco Trevisan &lt;mrctrevisan@yahoo.it&gt;
 * @author De Oliveira Edouard &lt;doe_wanted@yahoo.fr&gt;
 */
public class PluginAuthenticationHandler implements AuthenticationHandler {

    private List<AuthenticationHandler> plugins;

    private AuthenticationHandler activeHandler;

    public PluginAuthenticationHandler() {
        this.plugins = new ArrayList<>();
    }

    @Override
    public List<String> getAuthenticationMechanisms() {
        List<String> ret = new ArrayList<>();
        for (var plugin : plugins) {
            ret.addAll(plugin.getAuthenticationMechanisms());
        }
        return ret;
    }

    @Override
    public boolean auth(String clientInput, StringBuilder response, SMTPContext ctx) throws LoginFailedException {
        var stk = new StringTokenizer(clientInput);
        if (stk.nextToken().equalsIgnoreCase("AUTH")) {
            resetState();
            activateHandler(stk.nextToken().toUpperCase());
        }
        return getActiveHandler().auth(clientInput, response, ctx);
    }

    @Override
    public void resetState() {
        if (getActiveHandler() != null) {
            getActiveHandler().resetState();
        }
        setActiveHandler(null);
    }

    private void activateHandler(final String mechanism) {
        for (var plugin : plugins) {
            if (plugin.getAuthenticationMechanisms().contains(mechanism)) {
                setActiveHandler(plugin);
                return;
            }
        }
    }

    public List<AuthenticationHandler> getPlugins() {
        return plugins;
    }

    public void setPlugins(List<AuthenticationHandler> plugins) {
        this.plugins = plugins;
    }

    public void addPlugin(AuthenticationHandler plugin) {
        plugins.add(plugin);
    }

    private AuthenticationHandler getActiveHandler() {
        return activeHandler;
    }

    private void setActiveHandler(AuthenticationHandler activeHandler) {
        this.activeHandler = activeHandler;
    }
}
