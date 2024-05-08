package mcmgnetwork.mcmg_networkhandler;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.google.inject.Inject;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import org.slf4j.Logger;

@Plugin(
        id = "mcmg-network-handler",
        name = "MCMG_NetworkHandler",
        version = "1.0-SNAPSHOT"
)
public class MCMG_NetworkHandler {

    public static final MinecraftChannelIdentifier MCMG_IDENTIFIER = MinecraftChannelIdentifier.from("mcmg:main");

    private final ProxyServer proxy;
    private final Logger logger;

    @Inject
    public MCMG_NetworkHandler(ProxyServer proxy, Logger logger)
    {
        this.proxy = proxy;
        this.logger = logger;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event)
    {
        // Register the MCMG plugin messaging channel
        proxy.getChannelRegistrar().register(MCMG_IDENTIFIER);

        logger.info("The MCMG_NetworkHandler plugin has successfully started!");
    }

    @Subscribe
    public void onPluginMessageFromPlugin(PluginMessageEvent e)
    {
        logger.info("The MCMG_NetworkHandler received a plugin message.");

        // Only accept messages from servers (not players)
        if (!(e.getSource() instanceof ServerConnection)) return;
        // Only handle plugin messages on the MCMG channel
        if (e.getIdentifier() != MCMG_IDENTIFIER) return;

        //TODO REMOVE Store reference to the server that sent the request
        //RegisteredServer prevServer = ((ServerConnection) e.getSource()).getPreviousServer().get();


        // Read incoming message data/contents
        ByteArrayDataInput in = ByteStreams.newDataInput(e.getData());
        String subChannel = in.readUTF();

        //
        if (subChannel.equals("ServerTransferRequest"))
        {
            String playerName = in.readUTF();
            String serverType = in.readUTF();


            //TODO Somehow determine what server name to return


            //TODO remove this temp implementation
            boolean isActive = false;
            String serverName = "";

            if (serverType.equals("KOTH"))
            {
                isActive = true;
                serverName = "KOTH_lobby";
            }


            // Format return message
            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            out.writeUTF("ServerTransferResponse");
            out.writeBoolean(isActive);
            out.writeUTF(playerName);
            out.writeUTF(serverName);

            for (RegisteredServer server : proxy.getAllServers())
                server.sendPluginMessage(MCMG_IDENTIFIER, out.toByteArray());

            logger.info("The MCMG_NetworkHandler is returning the requested server's status.");
        }
    }
}
