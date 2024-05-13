package mcmgnetwork.mcmg_networkhandler;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import mcmgnetwork.mcmg_networkhandler.listeners.PluginMessageHandler;
import mcmgnetwork.mcmg_networkhandler.protocols.MessageTypes;
import mcmgnetwork.mcmg_networkhandler.protocols.ServerStatuses;
import mcmgnetwork.mcmg_networkhandler.utilities.ActiveServerUtil;
import mcmgnetwork.mcmg_networkhandler.utilities.ServerInitializeUtil;

import java.util.concurrent.CompletableFuture;

/**
 * Description: <p>
 *  Handles the requested transfers of a player across servers in the MCMG network. Accounts for offline vs. online
 *  server instances, potential initialization of new servers, and updates the requesting player on the status of the
 *  server transfer.
 *
 *  <p>Author(s): Miles Bovero
 *  <p>Date Created: 5/12/24
 */
public class ServerTransferHandler
{
    /**
     * Provided data containing a player name and server type, this method determines if there is a transferable lobby
     * server of the specified type. If one exists and is operating, the specified player is sent there. If no such
     * server instance is active, an attempt to start a new server is made. In any case, a Lobby Transfer Response is
     * sent to back to the network to update the requesting player on the status of the requested transfer.
     * @param in The ByteArrayDataInput containing a player name and server type (recognized by the Velocity proxy
     *           server's MCMG_NetworkHandler plugin) that the specified player may be transferred to
     */
    public static void handleLobbyTransferRequest(ByteArrayDataInput in) {
        // Read/store remaining plugin message data
        String playerName = in.readUTF();
        String serverType = in.readUTF();

        // Get updated information on all network servers
        CompletableFuture<Void> serverInfoFuture = ActiveServerUtil.getServerInfo();

        // Wait for all server pings to complete, then run remaining code:
        serverInfoFuture.thenRun(() ->
        {
            // Track server status (initially assumed to be online & transferable)
            String serverStatus = ServerStatuses.TRANSFERABLE;

            // Attempt to identify a target server to transfer the player to
            String serverName = ActiveServerUtil.findTransferableServerName(serverType);

            // If no transferable server could be found, attempt to start a new one
            if (serverName.isEmpty())
                serverStatus = ServerInitializeUtil.startNewServer(serverType);   // Store updated server status

            // Send a response to the network
            sendLobbyTransferResponse(serverStatus, playerName, serverName);
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
            server.sendPluginMessage(PluginMessageHandler.getMCMG_IDENTIFIER(), out.toByteArray());

        MCMG_NetworkHandler.getLogger().info("The MCMG_NetworkHandler is returning the requested server's status."); //TODO remove
    }
}
