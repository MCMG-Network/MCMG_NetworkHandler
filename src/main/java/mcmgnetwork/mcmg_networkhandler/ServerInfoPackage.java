package mcmgnetwork.mcmg_networkhandler;

import com.velocitypowered.api.proxy.server.ServerPing;

import java.util.Optional;

public class ServerInfoPackage
{
    private final ServerPing serverPing;
    private final String serverName;
    private int onlinePlayerCount;
    private int maximumPlayerCount;

    public ServerInfoPackage(ServerPing serverPing, String serverName)
    {
        this.serverPing = serverPing;
        this.serverName = serverName;

        // Initialize player-related fields
        Optional<ServerPing.Players> serverPlayers = serverPing.getPlayers();
        serverPlayers.ifPresent(players ->
        {
            onlinePlayerCount = players.getOnline();
            maximumPlayerCount = players.getMax();
        });
    }

    public ServerPing getServerPing() { return serverPing; }

    public String getServerName() { return serverName; }

    public int getOnlinePlayerCount() { return onlinePlayerCount; }

    public int getMaximumPlayerCount() { return maximumPlayerCount; }
}
