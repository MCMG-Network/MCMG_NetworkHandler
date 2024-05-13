package mcmgnetwork.mcmg_networkhandler.listeners;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.*;
import mcmgnetwork.mcmg_networkhandler.MCMG_NetworkHandler;

public class MainHubInitializer
{
    @Subscribe
    public void onConnectionHandshakeEvent(ConnectionHandshakeEvent e)
    {
        MCMG_NetworkHandler.getLogger().info("ConnectionHandshakeEvent detected! {}", e.getIntent().toString());

    }

    @Subscribe
    public void onLoginEvent(LoginEvent e)
    {
        MCMG_NetworkHandler.getLogger().info("LoginEvent detected! {}", e.getResult().getReasonComponent().toString());

    }

    @Subscribe
    public void onPreLoginEvent(PreLoginEvent e)
    {
        MCMG_NetworkHandler.getLogger().info("PreLoginEvent detected! {}", e.getResult().getReasonComponent().toString());
    }

    @Subscribe
    public void onDisconnectEvent(DisconnectEvent e)
    {
        MCMG_NetworkHandler.getLogger().info("DisconnectEvent detected! {}", e.getLoginStatus().toString());

    }

}
