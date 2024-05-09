package mcmgnetwork.mcmg_networkhandler;

import com.velocitypowered.api.proxy.server.ServerPing;
import lombok.Getter;

import java.util.Optional;

/**
 * Description: <p>
 *  A wrapper class for a ServerPing object instance and a correlating server name. Provides easy and compact access
 *  to a server's name and player count information.
 *
 *  <p>Author(s): Miles Bovero
 *  <p>Date Created: 5/7/24
 */
@Getter
public class ServerInfoPackage
{
    /**
     * The ServerPing object that this ServerInfoPackage is backed by
     */
    private final ServerPing serverPing;

    /**
     * The name of the server that this ServerInfoPackage was constructed from
     */
    private final String serverName;

    /**
     * The number of players online the server that this ServerInfoPackage was constructed from
     */
    private int onlinePlayerCount;

    /**
     * The maximum number of players that can be online the server that this ServerInfoPackage was constructed from
     */
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

}
