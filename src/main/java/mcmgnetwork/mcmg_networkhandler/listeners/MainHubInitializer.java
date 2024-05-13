package mcmgnetwork.mcmg_networkhandler.listeners;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.*;
import mcmgnetwork.mcmg_networkhandler.protocols.ServerTypes;
import mcmgnetwork.mcmg_networkhandler.utilities.ActiveServerUtil;
import mcmgnetwork.mcmg_networkhandler.utilities.ServerInitializeUtil;

import java.util.concurrent.CompletableFuture;

public class MainHubInitializer
{

    @Subscribe
    public void onDisconnectEvent(DisconnectEvent e)
    {
        if (e.getLoginStatus().equals(DisconnectEvent.LoginStatus.PRE_SERVER_JOIN))
            initializeNewMainHubServer();
    }


    private void initializeNewMainHubServer()
    {
        // Get updated information on all network servers
        CompletableFuture<Void> serverInfoFuture = ActiveServerUtil.getServerInfo();

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
