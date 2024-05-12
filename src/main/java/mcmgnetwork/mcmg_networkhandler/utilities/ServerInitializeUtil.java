package mcmgnetwork.mcmg_networkhandler.utilities;

import mcmgnetwork.mcmg_networkhandler.ConfigManager;
import mcmgnetwork.mcmg_networkhandler.MCMG_NetworkHandler;
import mcmgnetwork.mcmg_networkhandler.protocols.ServerStatuses;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Description: <p>
 *  A utility class holding static data and functions that handle specific actions and information regarding the
 *  creation/initialization of new network servers.
 *
 *  <p>Author(s): Miles Bovero
 *  <p>Date Created: 5/11/24
 */
public class ServerInitializeUtil
{

    /**
     * A set of names of server types that are actively being initialized; used to prevent initialization overlap/spam
     */
    private static final Set<String> initializingServers = new HashSet<>();

    /**
     * Handles the tracking of initializing servers and the time allotted for new server initialization
     */
    private static final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    /**
     * The number of seconds given for a server to initialize; any duplicate server creation requests are ignored
     * during this time
     */
    private static final int initializationTime = 10;

    //TODO comment header
    public static String startNewServer(String serverType)
    {
        // If the requested server type already has a new server being initialized, return status early
        if (initializingServers.contains(serverType))
            return ServerStatuses.INITIALIZING;

        // Beginning new server initialization; track it to prevent duplicate start requests
        initializingServers.add(serverType);
        executor.schedule(() -> initializingServers.remove(serverType), initializationTime, TimeUnit.SECONDS);

        // Attempt to retrieve a new server instance's name
        String newServerName = getNewServerName(serverType);
        // If there is no room for a new server of the specified type, return early
        if (newServerName.isEmpty())
            return ServerStatuses.FULL;

        // Otherwise, attempt to initialize a new server
        boolean successfulStart = initializeNewServer(serverType, newServerName);

        if (successfulStart)
            return ServerStatuses.BEGAN_INITIALIZATION;
        else
            return ServerStatuses.FAILED_INITIALIZATION;
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
        for (String serverName : ActiveServerUtil.getActiveServerInfo().keySet())
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

    /**
     * Creates a new server directory with the necessary files, updates its information according to the provided
     * parameters, and starts the new server.
     * @param serverType The type of server to be created and initialized
     * @param newServerName The name of the new server
     * @return Whether or not the new server initialization completed without IOExceptions
     */
    private static boolean initializeNewServer(String serverType, String newServerName)
    {
        MCMG_NetworkHandler.getLogger().info("A new server, " + newServerName + ", is being created...");

        try
        {
            Path serverTypePath = Paths.get("server-instances", serverType);

            copyServerTemplateFolder(serverTypePath, newServerName);
            updateNewServerProperties(serverTypePath, newServerName);
            runNewServer(serverTypePath, newServerName);
        } catch (IOException ex)
        {
            // Get the stack trace info as a string
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            ex.printStackTrace(pw);

            MCMG_NetworkHandler.getLogger().error("An exception occurred while creating a new server: {}", sw);
            return false;
        }

        MCMG_NetworkHandler.getLogger().info("A new server, " + newServerName + ", was successfully created and started!");
        return true;
    }

    /**
     * Copies contents of an existing server template folder (at the specified serverTypePath) to another folder (given
     * the name of the specified newServerName) within the "active-servers" directory.
     * @param serverTypePath The path, ending in the requested serverType, that leads to a subdirectory containing
     *                       startup files for that serverType
     * @param newServerName The name of the new server to be created; will be the name of the new server folder
     * @throws IOException Indicates an I/O error occurred while accessing/copying files
     */
    private static void copyServerTemplateFolder(Path serverTypePath, String newServerName) throws IOException
    {
        // Initialize paths to the server template folder and the new server folder
        Path source = serverTypePath.resolve("template");
        Path destination = serverTypePath.resolve("active-servers").resolve(newServerName);

        // Copy the contents of the source folder to the destination folder
        try { copyDirectory(source, destination); }
        catch (IOException ex) { throw new IOException(ex); }
    }

    //TODO comment header
    public static void copyDirectory(Path source, Path target) throws IOException
    {
        Files.walkFileTree(source, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Path targetDir = target.resolve(source.relativize(dir));
                Files.createDirectories(targetDir);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.copy(file, target.resolve(source.relativize(file)), StandardCopyOption.REPLACE_EXISTING);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    /**
     * Updates the "server.properties" file of the newly created server to operate on the correct port.
     * <p>
     * Requires that copyTemplateFolder() has been successfully executed with the same provided parameters.
     * @param serverTypePath The path, ending in the requested serverType, that leads to a subdirectory containing
     *                       startup files for that serverType
     * @param newServerName The name of the newly created server
     * @throws IOException Indicates an I/O error occurred while accessing/editing files
     */
    private static void updateNewServerProperties(Path serverTypePath, String newServerName) throws IOException
    {
        // Initialize path to new server's "server.properties" file
        Path serverPropertiesFile = serverTypePath.resolve("active-servers")
                .resolve(newServerName)
                .resolve("server.properties");
        // Update server port properties
        Properties properties = new Properties();
        properties.load(Files.newInputStream(serverPropertiesFile));
        properties.setProperty("server-port", "25510"); //TODO change these to read from config
        properties.setProperty("query.port", "25510");
        properties.store(Files.newOutputStream(serverPropertiesFile), null);
    }

    /**
     * Creates and executes a batch file that runs the server .jar file in the specified path.
     * <p>
     * Requires that both copyTemplateFolder() and updateNewServerProperties() have been successfully executed with the
     * same provided parameters.
     * @param serverTypePath The path, ending in the requested serverType, that leads to a subdirectory containing
     *                       startup files for that serverType
     * @param newServerName The name of the newly created server
     * @throws IOException Indicates an I/O error occurred while accessing/executing files
     */
    private static void runNewServer(Path serverTypePath, String newServerName) throws IOException
    {
        // Define the content of the batch file
        String batchContent = "@setlocal enableextensions\n" +
                "@cd /d \"%~dp0\"\n" +
                "@echo off\n" +
                "java -Xmx1024M -Xms512M -jar paper.jar --nogui\n" +
                "PAUSE";

        // Construct the path to the batch file
        Path batchFilePath = serverTypePath.resolve("active-servers")
                .resolve(newServerName)
                .resolve("run.bat");

        // Write the batch content to the file
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(batchFilePath.toFile())))
        { writer.write(batchContent); }

        // Execute the batch file
        ProcessBuilder builder = new ProcessBuilder("cmd", "/c", "start", "cmd", "/c", batchFilePath.toString());
        builder.start();
    }
}
