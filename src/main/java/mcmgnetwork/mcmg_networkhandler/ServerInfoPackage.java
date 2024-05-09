package mcmgnetwork.mcmg_networkhandler;

import com.velocitypowered.api.proxy.server.ServerPing;

import java.util.Optional;

/**
 * Description: <p>
 *  A wrapper class for a ServerPing object instance and a correlating server name. Provides easy and compact access
 *  to a server's name and player count information.
 *
 *  <p>Author(s): Miles Bovero
 *  <p>Date Created: 5/7/24
 */
public class ServerInfoPackage
{
    private final ServerPing serverPing;
    private final String serverName;
    private int onlinePlayerCount;
    private int maximumPlayerCount;

    /**
     * Creates a new ServerInfoPackage instance based off of the provided ServerPing correlating to the provided
     * server name.
     * @param serverPing The ServerPing result obtained by pinging a RegisteredServer
     * @param serverName The name of the server that was pinged
     */
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

    /**
     * @return The ServerPing object that this ServerInfoPackage is backed by
     */
    public ServerPing getServerPing() { return serverPing; }

    /**
     * @return The name of the server that this ServerInfoPackage was constructed from
     */
    public String getServerName() { return serverName; }

    /**
     * @return The number of players online the server that this ServerInfoPackage was constructed from
     */
    public int getOnlinePlayerCount() { return onlinePlayerCount; }

    /**
     * @return The maximum number of players that can be online the server that this ServerInfoPackage was constructed
     * from
     */
    public int getMaximumPlayerCount() { return maximumPlayerCount; }
}
