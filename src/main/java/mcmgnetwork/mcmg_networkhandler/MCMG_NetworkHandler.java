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
import com.velocitypowered.api.proxy.server.ServerPing;
import mcmgnetwork.mcmg_networkhandler.protocols.ChannelNames;
import mcmgnetwork.mcmg_networkhandler.protocols.MessageTypes;
import mcmgnetwork.mcmg_networkhandler.protocols.ServerTypes;
import org.slf4j.Logger;

import javax.security.auth.callback.Callback;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Plugin(
        id = "mcmg-network-handler",
        name = "MCMG_NetworkHandler",
        version = "1.0-SNAPSHOT"
)
public class MCMG_NetworkHandler {

    public static final MinecraftChannelIdentifier MCMG_IDENTIFIER = MinecraftChannelIdentifier.from(ChannelNames.MCMG);

    private static HashMap<String, Boolean> serverStatuses = new HashMap<>();
    private static HashMap<String, Integer> serverPlayerCounts = new HashMap<>();

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

        // Read incoming message data/contents
        ByteArrayDataInput in = ByteStreams.newDataInput(e.getData());
        String subChannel = in.readUTF();

        // Only handle SERVER_TRANSFER_REQUESTs
        if (subChannel.equals(MessageTypes.SERVER_TRANSFER_REQUEST))
        {
            String playerName = in.readUTF();
            String serverType = in.readUTF();


            //TODO Somehow determine what server name to return

            getServerStatuses();


            //TODO remove this temp implementation
            boolean isActive = false;
            String serverName = "";

            if (serverType.equals(ServerTypes.KOTH_LOBBY))
            {
                isActive = true;
                serverName = "KOTH_lobby";
            }




            // Format return message
            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            out.writeUTF(MessageTypes.SERVER_TRANSFER_RESPONSE);
            out.writeBoolean(isActive);
            out.writeUTF(playerName);
            out.writeUTF(serverName);

            for (RegisteredServer server : proxy.getAllServers())
                server.sendPluginMessage(MCMG_IDENTIFIER, out.toByteArray());

            logger.info("The MCMG_NetworkHandler is returning the requested server's status.");
        }
    }

    private void getServerStatuses()
    {
        for (RegisteredServer server : proxy.getAllServers())
        {
            // Retrieve and store the server's name
            String serverName = server.getServerInfo().getName();

            // Ping the server asynchronously
            server.ping().thenApplyAsync((ServerPing ping) ->
            {
                serverStatuses.put(serverName, true);

                Optional<ServerPing.Players> serverPlayers = ping.getPlayers();
                serverPlayers.ifPresent(players -> serverPlayerCounts.put(serverName, players.getOnline()));

                logger.info("Pinged " + serverName + "! The server has " + serverPlayerCounts.get(serverName) + " players online.");

                return ping;
            }).exceptionally((Throwable ex) ->
            {
                serverStatuses.remove(serverName);
                logger.warn("Failed to ping " + serverName + ": " + ex.getMessage());
                return null;
            });

            // If the server is online...
            if (serverStatuses.containsKey(serverName))
            {

            }
        }
    }
}
