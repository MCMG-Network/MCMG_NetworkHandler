package mcmgnetwork.mcmg_networkhandler;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import lombok.Getter;
import mcmgnetwork.mcmg_networkhandler.protocols.ChannelNames;
import mcmgnetwork.mcmg_networkhandler.protocols.MessageTypes;
import mcmgnetwork.mcmg_networkhandler.protocols.ServerStatuses;
import mcmgnetwork.mcmg_networkhandler.utilities.ServerUtil;

import java.util.concurrent.CompletableFuture;

/**
 * Description: <p>
 *  Handles all incoming and outgoing plugin messages for the network.
 *
 *  <p>Author(s): Miles Bovero
 *  <p>Date Created: 5/9/24
 */
public class PluginMessageHandler
{
    @Getter
    private static final MinecraftChannelIdentifier MCMG_IDENTIFIER = MinecraftChannelIdentifier.from(ChannelNames.MCMG);

    /**
     * Listens for incoming plugin messages, identifies recognized messages, and handles them.
     * @param e The event indicating an incoming plugin message; contains plugin message data
     */
    @Subscribe
    public void onPluginMessageFromPlugin(PluginMessageEvent e)
    {
        MCMG_NetworkHandler.getLogger().info("The MCMG_NetworkHandler received a plugin message.");  //TODO remove

        // Only accept messages from servers (not players)
        if (!(e.getSource() instanceof ServerConnection)) return;
        // Only handle plugin messages on the MCMG channel
        if (e.getIdentifier() != MCMG_IDENTIFIER) return;

        // Read incoming message data/contents
        ByteArrayDataInput in = ByteStreams.newDataInput(e.getData());
        String subChannel = in.readUTF();

        // Only handle SERVER_TRANSFER_REQUESTs
        if (subChannel.equals(MessageTypes.LOBBY_TRANSFER_REQUEST))
            handleLobbyTransferRequest(in);
    }

    //TODO add method header
    private static void handleLobbyTransferRequest(ByteArrayDataInput in) {
        // Read/store remaining plugin message data
        String playerName = in.readUTF();
        String serverType = in.readUTF();

        // Get updated server information
        CompletableFuture<Void> serverInfoFuture = ServerUtil.getServerInfo();

        // Wait for all server pings to complete, then run remaining code:
        serverInfoFuture.thenRun(() ->
        {
            // Track server status (initially assumed to be online & transferable)
            String serverStatus = ServerStatuses.TRANSFERABLE;

            // Attempt to identify a target server to transfer the player to
            String serverName = ServerUtil.findTransferableServerName(serverType);

            //TODO remove debug tool
            MCMG_NetworkHandler.getLogger().info("findTransferableServerName result: " + serverName);

            // If no transferable target server could be found, attempt to start a new one
            if (serverName.isEmpty())
            {
                serverStatus = ServerUtil.startNewServer(serverType);   // Store updated server status
                //TODO remove debug tool
                MCMG_NetworkHandler.getLogger().info("startNewServer result: " + serverStatus);
            }

            // Send a response to the network
            sendLobbyTransferResponse(serverStatus, playerName, serverName);
            //TODO remove debug tool
            MCMG_NetworkHandler.getLogger().info("sendLobbyTransferResponse contents: " + serverStatus + ", " + playerName +
                    ", " + serverName);

        });
    }

    /**
     * Sends a LOBBY_TRANSFER_RESPONSE with the provided parameters to the network using plugin messaging.
     * @param serverStatus The ServerStatus of the lobby server type provided in the request
     * @param playerName The name of the player requested to be transferred
     * @param serverName The server instance name to transfer the specified player to
     */
    private static void sendLobbyTransferResponse(String serverStatus, String playerName, String serverName)
    {
        // Format return message
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF(MessageTypes.LOBBY_TRANSFER_RESPONSE);
        out.writeUTF(serverStatus);
        out.writeUTF(playerName);
        out.writeUTF(serverName);
        // Send response message
        for (RegisteredServer server : MCMG_NetworkHandler.getProxy().getAllServers())
            server.sendPluginMessage(MCMG_IDENTIFIER, out.toByteArray());

        MCMG_NetworkHandler.getLogger().info("The MCMG_NetworkHandler is returning the requested server's status."); //TODO remove
    }
}
