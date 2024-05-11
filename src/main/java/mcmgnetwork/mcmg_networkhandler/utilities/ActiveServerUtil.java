package mcmgnetwork.mcmg_networkhandler.utilities;

import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerPing;
import lombok.Getter;
import mcmgnetwork.mcmg_networkhandler.ConfigManager;
import mcmgnetwork.mcmg_networkhandler.MCMG_NetworkHandler;
import mcmgnetwork.mcmg_networkhandler.protocols.ServerStatuses;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Description: <p>
 *  A utility class holding static data and functions that handle specific actions and information regarding active
 *  network servers.
 *
 *  <p>Author(s): Miles Bovero
 *  <p>Date Created: 5/9/24
 */
public class ActiveServerUtil
{

    /**
     * A map of the names of active servers and the ServerInfoPackage corresponding to that server
     */
    @Getter
    private static final HashMap<String, ServerInfoPackage> activeServerInfo = new HashMap<>();

    /**
     * Updates the ActiveServerUtil's activeServerInfo field by pinging all network servers, handling successful
     * and failed pings, and extracting/storing correlating information.
     * @return a CompletableFuture that completes when all ping operations have completed, allowing other methods
     * to wait on this method's completion
     */
    public static CompletableFuture<Void> getServerInfo()
    {
        // Initialize a list to hold/track all server ping results
        List<CompletableFuture<ServerPing>> pingResults = new ArrayList<>();

        // For every server on the network...
        for (RegisteredServer server : MCMG_NetworkHandler.getProxy().getAllServers())
        {
            // Retrieve and store the server's name
            String serverName = server.getServerInfo().getName();

            // Ping the server asynchronously
            CompletableFuture<ServerPing> futurePing = server.ping().thenApplyAsync((ServerPing ping) ->
            {
                // Successful ping -> store server information
                activeServerInfo.put(serverName, new ServerInfoPackage(ping, serverName));

                //TODO remove
                MCMG_NetworkHandler.getLogger().info("Pinged {}! The server has {} out of {} players online.", serverName,
                        activeServerInfo.get(serverName).getOnlinePlayerCount(), activeServerInfo.get(serverName).getMaximumPlayerCount());

                return ping;
            }).exceptionally((Throwable ex) ->
            {
                // Failed ping -> remove this server from active server list
                activeServerInfo.remove(serverName);

                MCMG_NetworkHandler.getLogger().warn("Failed to ping " + serverName + ": " + ex.getMessage());   //TODO remove
                return null;
            });

            // Store the server ping result
            pingResults.add(futurePing);
        }

        // Return a CompletableFuture that completes when all ping operations complete
        return CompletableFuture.allOf(pingResults.toArray(new CompletableFuture[0]));
    }

    /**
     * @param serverType The type of server to be targeted
     * @return The name of a server of the specified type (if one was found). If multiple valid servers are found, the
     * name of the server with the most online players (and room for more) is returned. If no valid servers are found,
     * an empty string is returned.
     */
    public static String findTransferableServerName(String serverType)
    {
        String targetServer = "";
        // Store count used to find available server with most active players
        int maxPlayerCount = -1;

        // Filter through active servers to identify target server for transferring
        for (ServerInfoPackage serverInfo : activeServerInfo.values())
        {
            // Only consider servers of the specified type
            if (!serverInfo.getServerName().contains(serverType)) continue;
            // Only consider servers with room for another player
            if (serverInfo.getOnlinePlayerCount() == serverInfo.getMaximumPlayerCount()) continue;

            // Only update the target server if this server has more players than the last target server
            if (serverInfo.getOnlinePlayerCount() > maxPlayerCount)
            {
                targetServer = serverInfo.getServerName();
                maxPlayerCount = serverInfo.getOnlinePlayerCount();
            }
        }

        return targetServer;
    }
}
