package mcmgnetwork.mcmg_networkhandler.listeners;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.*;
import mcmgnetwork.mcmg_networkhandler.protocols.ServerTypes;
import mcmgnetwork.mcmg_networkhandler.utilities.ActiveServerUtil;
import mcmgnetwork.mcmg_networkhandler.utilities.ServerInitializeUtil;

import java.util.concurrent.CompletableFuture;

/**
 * Description: <p>
 *  Handles the initialization of new main_hub servers when all main_hub server instances are currently inactive or full.
 *
 *  <p>Author(s): Miles Bovero
 *  <p>Date Created: 5/12/24
 */
public class MainHubInitializer
{

    /**
     * Listens for DisconnectEvents, and when an event with a PRE_SERVER_JOIN LoginStatus is detected, checks if a new
     * main_hub server instance should be made.
     * @param e The DisconnectEvent to be handled
     */
    @Subscribe
    public void onDisconnectEvent(DisconnectEvent e)
    {
        if (e.getLoginStatus().equals(DisconnectEvent.LoginStatus.PRE_SERVER_JOIN))
            initializeNewMainHubServer();
    }

    /**
     * Pings network servers to see if there are no active main_hub servers or if all main_hub servers are full. If so,
     * an attempt to start a new main_hub server instance is made.
     */
    private void initializeNewMainHubServer()
    {
        // Get updated information on all network servers
        CompletableFuture<Void> serverInfoFuture = ActiveServerUtil.getServerInfoFuture();

        // Wait for all server pings to complete, then run remaining code:
        serverInfoFuture.thenRun(() ->
        {
            // Check for any operational main hub servers
            String serverName = ActiveServerUtil.findTransferableServerName(ServerTypes.MAIN_HUB);

            // If no transferable server could be found, attempt to start a new one
            if (serverName.isEmpty())
                ServerInitializeUtil.startNewServer(ServerTypes.MAIN_HUB);
        });
    }
}
