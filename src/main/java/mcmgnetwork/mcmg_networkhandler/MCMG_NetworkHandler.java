package mcmgnetwork.mcmg_networkhandler;

import com.google.inject.Inject;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import dev.dejvokep.boostedyaml.YamlDocument;
import dev.dejvokep.boostedyaml.dvs.versioning.BasicVersioning;
import dev.dejvokep.boostedyaml.settings.dumper.DumperSettings;
import dev.dejvokep.boostedyaml.settings.general.GeneralSettings;
import dev.dejvokep.boostedyaml.settings.loader.LoaderSettings;
import dev.dejvokep.boostedyaml.settings.updater.UpdaterSettings;
import lombok.Getter;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

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
    private static YamlDocument config;

    /**
     * Creates a new MCMG_NetworkHandler plugin instance, injecting a proxy server and logger for plugin capabilities
     * and console messages.
     * @param proxy The proxy server that this plugin operates on
     * @param logger The logger used to write info, warnings, etc. to the server console
     */
    @Inject
    public MCMG_NetworkHandler(ProxyServer proxy, Logger logger, @DataDirectory Path dataDirectory)
    {
        MCMG_NetworkHandler.proxy = proxy;
        MCMG_NetworkHandler.logger = logger;

        try
        {
            config = YamlDocument.create(new File(dataDirectory.toFile(), "config.yml"),
                    Objects.requireNonNull(getClass().getResourceAsStream("/config.yml")),  // Get config from resources folder & populate defaults into config file
                    GeneralSettings.DEFAULT,
                    LoaderSettings.builder().setAutoUpdate(true).build(), // Config file will update automatically without user interaction
                    DumperSettings.DEFAULT,
                    UpdaterSettings.builder().setVersioning(new BasicVersioning("file-version"))    // Set route to config.yml for automatic versioning
                            .setOptionSorting(UpdaterSettings.OptionSorting.SORT_BY_DEFAULTS).build()
            );

            config.update();
            config.save();
        } catch (IOException ex)
        {
            logger.error("Could not create/load plugin config! Plugin shutting down...");
            // Shutdown plugin executor
            Optional<PluginContainer> container = proxy.getPluginManager().getPlugin("mcmg-network-handler");
            container.ifPresent(pluginContainer -> pluginContainer.getExecutorService().shutdown());
        }
    }

    /**
     * Executed upon initialization of the proxy server running this plugin. Registers necessary plugin messaging
     * channels and announces successful initialization.
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
