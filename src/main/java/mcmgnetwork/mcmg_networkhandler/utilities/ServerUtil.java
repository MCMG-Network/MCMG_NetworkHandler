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
 *  A utility class holding static data and functions that handle specific actions and information regarding network
 *  servers.
 *
 *  <p>Author(s): Miles Bovero
 *  <p>Date Created: 5/9/24
 */
public class ServerUtil
{

    /**
     * A map of the names of active servers and the ServerInfoPackage corresponding to that server
     */
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


    public static String startNewServer(String serverType)
    {
        // Attempt to retrieve a new server instance's name
        String newServerName = getNewServerName(serverType);

        // If there is no room for a new server of the specified type, return early
        if (newServerName.isEmpty())
            return ServerStatuses.FULL;

        // Otherwise, create a new server with the new server name
        createNewServerFile(newServerName);

        //TODO change/remove
        return ServerStatuses.INITIALIZING;
    }

    /**
     * Checks if there is an available slot for a new server of the specified type and returns its name or an empty
     * string if no slot is available.
     * @param serverType The type of server to try to retrieve a new instance's name of
     * @return The new server instance's name; or an empty string if there is no room for a new server of the specified
     * type
     */
    private static String getNewServerName(String serverType)
    {
        // Find and store names of all active servers of the provided type
        Set<String> activeServerNames = new HashSet<>();
        for (String serverName : activeServerInfo.keySet())
            if (serverName.contains(serverType))
                activeServerNames.add(serverName);

        // Iterate over active servers to try and find an open slot for a new server to exist
        int maxServerTypeCount = ConfigManager.getMaxServerTypeCount(serverType);
        for (int i=0; i<maxServerTypeCount; i++)
        {
            String serverName = serverType + i;
            if (!activeServerNames.contains(serverName))
                return serverName;
        }

        // If all possible servers of the specified type are full, return empty string
        return "";
    }

    //TODO method header
    private static void createNewServerFile(String newServerName)
    {
        Path currentDirectory = Paths.get("").toAbsolutePath(); // Get the current working directory of the Java process
        MCMG_NetworkHandler.getLogger().info("Server creation file directory: " + currentDirectory.toString());
    }

}
