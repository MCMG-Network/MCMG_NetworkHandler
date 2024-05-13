package mcmgnetwork.mcmg_networkhandler.listeners;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import lombok.Getter;
import mcmgnetwork.mcmg_networkhandler.MCMG_NetworkHandler;
import mcmgnetwork.mcmg_networkhandler.ServerTransferHandler;
import mcmgnetwork.mcmg_networkhandler.protocols.ChannelNames;
import mcmgnetwork.mcmg_networkhandler.protocols.MessageTypes;

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

        // Only handle specific sub-channels / message types
        if (subChannel.equals(MessageTypes.LOBBY_TRANSFER_REQUEST))
            ServerTransferHandler.handleLobbyTransferRequest(in);
    }
}
