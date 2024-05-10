package mcmgnetwork.mcmg_networkhandler;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.google.inject.Inject;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import dev.dejvokep.boostedyaml.YamlDocument;
import dev.dejvokep.boostedyaml.dvs.versioning.BasicVersioning;
import dev.dejvokep.boostedyaml.settings.dumper.DumperSettings;
import dev.dejvokep.boostedyaml.settings.general.GeneralSettings;
import dev.dejvokep.boostedyaml.settings.loader.LoaderSettings;
import dev.dejvokep.boostedyaml.settings.updater.UpdaterSettings;
import lombok.Getter;
import mcmgnetwork.mcmg_networkhandler.protocols.ChannelNames;
import mcmgnetwork.mcmg_networkhandler.protocols.MessageTypes;
import mcmgnetwork.mcmg_networkhandler.utilities.ActiveServerUtil;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;

//TODO file header
@Plugin(
        id = "mcmg-network-handler",
        name = "MCMG_NetworkHandler",
        version = "1.0-SNAPSHOT"
)
public class MCMG_NetworkHandler {

    @Getter
    private static final MinecraftChannelIdentifier MCMG_IDENTIFIER = MinecraftChannelIdentifier.from(ChannelNames.MCMG);

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
        proxy.getChannelRegistrar().register(MCMG_IDENTIFIER);

        logger.info("The MCMG_NetworkHandler plugin has successfully started!");
    }

    //TODO add method header
    @Subscribe
    public void onPluginMessageFromPlugin(PluginMessageEvent e)
    {
        logger.info("The MCMG_NetworkHandler received a plugin message.");  //TODO remove

        // Only accept messages from servers (not players)
        if (!(e.getSource() instanceof ServerConnection)) return;
        // Only handle plugin messages on the MCMG channel
        if (e.getIdentifier() != MCMG_IDENTIFIER) return;

        // Read incoming message data/contents
        ByteArrayDataInput in = ByteStreams.newDataInput(e.getData());
        String subChannel = in.readUTF();
        String playerName = in.readUTF();
        String serverType = in.readUTF();

        // Only handle SERVER_TRANSFER_REQUESTs
        if (subChannel.equals(MessageTypes.LOBBY_TRANSFER_REQUEST))
        {
            // Get updated server information
            CompletableFuture<Void> serverInfoFuture = ActiveServerUtil.getServerInfo();

            // Wait for all server pings to complete, then run remaining code:
            serverInfoFuture.thenRun(() ->
            {
                // Attempt to identify a target server to transfer the player to
                String targetServer = ActiveServerUtil.findTargetServer(serverType);
                // Send a response to the network indicating whether or not a transferable server was found
                sendServerTransferResponse(playerName, targetServer);

                // If no transferable target server could be found, start one up
                //if (targetServer == null)
                {

                }

                logger.info("The MCMG_NetworkHandler is returning the requested server's status."); //TODO remove
            });
        }
    }



    /**
     * Sends a SERVER_TRANSFER_RESPONSE, based on the provided parameters, to the network using plugin messaging. If
     * the name of a valid server is entered, it is assumed to be online. If no target server could be found, the
     * provided targetServerName should be an empty string; this status will be relayed to the provided player.
     * @param playerName The name of the player to be transferred to the target server
     * @param targetServerName The name of the server that the provided player will be transferred to; an empty string
     *                         if no target server could be found
     */
    public static void sendServerTransferResponse(String playerName, String targetServerName)
    {
        // Initialize boolean storing whether or not a target server was found
        boolean isActive = !targetServerName.isEmpty();

        // Format return message
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF(MessageTypes.LOBBY_TRANSFER_RESPONSE);
        out.writeBoolean(isActive);
        out.writeUTF(playerName);
        out.writeUTF(targetServerName);
        // Send response message
        for (RegisteredServer server : MCMG_NetworkHandler.getProxy().getAllServers())
            server.sendPluginMessage(MCMG_NetworkHandler.getMCMG_IDENTIFIER(), out.toByteArray());
    }
}
