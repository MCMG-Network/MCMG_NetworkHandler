package mcmgnetwork.mcmg_networkhandler.listeners;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.*;
import mcmgnetwork.mcmg_networkhandler.MCMG_NetworkHandler;

public class MainHubInitializer
{
    @Subscribe
    public void onConnectionHandshakeEvent(ConnectionHandshakeEvent e)
    {
        MCMG_NetworkHandler.getLogger().info("ConnectionHandshakeEvent detected!");
    }

    @Subscribe
    public void onLoginEvent(LoginEvent e)
    {
        MCMG_NetworkHandler.getLogger().info("LoginEvent detected!");
    }

    @Subscribe
    public void onPostLoginEvent(PostLoginEvent e)
    {
        MCMG_NetworkHandler.getLogger().info("PostLoginEvent detected!");
    }

    @Subscribe
    public void onPreLoginEvent(PreLoginEvent e)
    {
        MCMG_NetworkHandler.getLogger().info("PreLoginEvent detected!");
    }

    @Subscribe
    public void onDisconnectEvent(DisconnectEvent e)
    {
        MCMG_NetworkHandler.getLogger().info("DisconnectEvent detected!");
    }

}
