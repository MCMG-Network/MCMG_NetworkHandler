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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Plugin(
        id = "mcmg-network-handler",
        name = "MCMG_NetworkHandler",
        version = "1.0-SNAPSHOT"
)
public class MCMG_NetworkHandler {

    public static final MinecraftChannelIdentifier MCMG_IDENTIFIER = MinecraftChannelIdentifier.from(ChannelNames.MCMG);

    private final HashMap<String, ServerInfoPackage> activeServerInfo = new HashMap<>();

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
        String playerName = in.readUTF();
        String serverType = in.readUTF();

        // Only handle SERVER_TRANSFER_REQUESTs
        if (subChannel.equals(MessageTypes.SERVER_TRANSFER_REQUEST))
        {
            // Get updated server information
            CompletableFuture<Void> serverInfoFuture = getServerInfo();

            // Wait for all server pings to complete, then run remaining code:
            serverInfoFuture.thenRun(() ->
            {
//                //TODO remove this temp implementation
//                boolean isActive = false;
//                String serverName = "";
//
//                if (serverType.equals(ServerTypes.KOTH_LOBBY))
//                {
//                    isActive = true;
//                    serverName = "KOTH_lobby";
//                }

                // Attempt to identify a target server to transfer the player to
                String targetServer = findTargetServer(serverType);
                // Send a response to the network indicating whether or not a transferable server was found
                sendServerTransferResponse(playerName, targetServer);

                // If no transferable target server could be found, start one up
                //if (targetServer == null)
                {

                }

                logger.info("The MCMG_NetworkHandler is returning the requested server's status.");
            });
        }
    }

    /**
     * Updates the serverStatuses and serverPlayerCounts hashmaps by pinging all network servers, handling successful
     * and failed pings, and extracting/storing correlating information.
     * @return a CompletableFuture that completes when all ping operations have completed, allowing other methods
     * to wait on this method's completion
     */
    private CompletableFuture<Void> getServerInfo()
    {
        // Initialize a list to hold/track all server ping results
        List<CompletableFuture<ServerPing>> pingResults = new ArrayList<>();

        for (RegisteredServer server : proxy.getAllServers())
        {
            // Retrieve and store the server's name
            String serverName = server.getServerInfo().getName();

            // Ping the server asynchronously
            CompletableFuture<ServerPing> futurePing = server.ping().thenApplyAsync((ServerPing ping) ->
            {
                // Successful ping -> store server information
                activeServerInfo.put(serverName, new ServerInfoPackage(ping, serverName));

                //TODO remove
                logger.info("Pinged {}! The server has {} out of {} players online.", serverName,
                        activeServerInfo.get(serverName).getOnlinePlayerCount(), activeServerInfo.get(serverName).getMaximumPlayerCount());

                return ping;
            }).exceptionally((Throwable ex) ->
            {
                // Failed ping -> remove this server from active server list
                activeServerInfo.remove(serverName);

                logger.warn("Failed to ping " + serverName + ": " + ex.getMessage());   //TODO remove
                return null;
            });

            // Store the server ping result
            pingResults.add(futurePing);
        }

        // Return a CompletableFuture that completes when all ping operations complete
        return CompletableFuture.allOf(pingResults.toArray(new CompletableFuture[0]));
    }

    /**
     * @param serverType The type of server to be targeted
     * @return The name of a server of the specified type (if one was found). If multiple valid servers are found, the
     * name of the server with the most online players (and room for more) is returned. If no valid servers are found,
     * an empty string is returned.
     */
    private String findTargetServer(String serverType)
    {
        String targetServer = "";
        // Store count used to find available server with most active players
        int maxPlayerCount = -1;

        // Filter through active servers to identify target server for transferring
        for (ServerInfoPackage server : activeServerInfo.values())
        {
            // Only consider servers of the specified type
            if (!server.getServerName().contains(serverType)) continue;
            // Only consider servers with room for another player
            if (server.getOnlinePlayerCount() == server.getMaximumPlayerCount()) continue;

            // Only update the target server if this server has more players than the last target server
            if (server.getOnlinePlayerCount() > maxPlayerCount)
            {
                targetServer = server.getServerName();
                maxPlayerCount = server.getOnlinePlayerCount();
            }
        }

        return targetServer;
    }

    /**
     * Sends a SERVER_TRANSFER_RESPONSE, based on the provided parameters, to the network using plugin messaging. If
     * the name of a valid server is entered, it is assumed to be online. If no target server could be found, the
     * provided targetServerName should be an empty string; this status will be relayed to the provided player.
     * @param playerName The name of the player to be transferred to the target server
     * @param targetServerName The name of the server that the provided player will be transferred to; an empty string
     *                         if no target server could be found
     */
    private void sendServerTransferResponse(String playerName, String targetServerName)
    {
        // Initialize boolean storing whether or not a target server was found
        boolean isActive = !targetServerName.isEmpty();

        // Format return message
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF(MessageTypes.SERVER_TRANSFER_RESPONSE);
        out.writeBoolean(isActive);
        out.writeUTF(playerName);
        out.writeUTF(targetServerName);
        // Send response message
        for (RegisteredServer server : proxy.getAllServers())
            server.sendPluginMessage(MCMG_IDENTIFIER, out.toByteArray());
    }
}
