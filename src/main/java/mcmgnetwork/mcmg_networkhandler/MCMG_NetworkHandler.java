package mcmgnetwork.mcmg_networkhandler;

import com.google.inject.Inject;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import lombok.Getter;
import mcmgnetwork.mcmg_networkhandler.listeners.PluginMessageHandler;
import mcmgnetwork.mcmg_networkhandler.utilities.ConfigUtil;
import org.slf4j.Logger;

import java.nio.file.Path;

//TODO file header
@Plugin(
        id = "mcmg-network-handler",
        name = "MCMG_NetworkHandler",
        version = "1.0-SNAPSHOT"
)
public class MCMG_NetworkHandler {

    @Getter
    private static ProxyServer proxy;
    @Getter
    private static Logger logger;

    /**
     * Creates a new MCMG_NetworkHandler plugin instance, initializing key components of the proxy plugin.
     * @param proxy The proxy server that this plugin operates on
     * @param logger The logger used to write info, warnings, etc. to the server console
     */
    @Inject
    public MCMG_NetworkHandler(ProxyServer proxy, Logger logger, @DataDirectory Path dataDirectory)
    {
        MCMG_NetworkHandler.proxy = proxy;
        MCMG_NetworkHandler.logger = logger;

        ConfigUtil.initializeConfig(dataDirectory);
    }

    /**
     * Executed upon initialization of the proxy server running this plugin. Registers necessary plugin components and
     * announces successful initialization.
     * @param event Ignore
     */
    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event)
    {
        // Register the MCMG plugin messaging channel
        proxy.getChannelRegistrar().register(PluginMessageHandler.getMCMG_IDENTIFIER());

        // Register event listeners
        proxy.getEventManager().register(this, new PluginMessageHandler());

        logger.info("The MCMG_NetworkHandler plugin has successfully started!");
    }

}
