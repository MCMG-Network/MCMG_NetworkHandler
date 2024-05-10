package mcmgnetwork.mcmg_networkhandler;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import mcmgnetwork.mcmg_networkhandler.protocols.ChannelNames;
import mcmgnetwork.mcmg_networkhandler.protocols.MessageTypes;
import mcmgnetwork.mcmg_networkhandler.utilities.ActiveServerUtil;

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

            MCMG_NetworkHandler.getLogger().info("The MCMG_NetworkHandler is returning the requested server's status."); //TODO remove
        });
    }

    /**
     * Sends a SERVER_TRANSFER_RESPONSE, based on the provided parameters, to the network using plugin messaging. If
     * the name of a valid server is entered, it is assumed to be online. If no target server could be found, the
     * provided targetServerName should be an empty string; this status will be relayed to the provided player.
     * @param playerName The name of the player to be transferred to the target server
     * @param targetServerName The name of the server that the provided player will be transferred to; an empty string
     *                         if no target server could be found
     */
    private static void sendServerTransferResponse(String playerName, String targetServerName)
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
            server.sendPluginMessage(MCMG_IDENTIFIER, out.toByteArray());
    }
}
