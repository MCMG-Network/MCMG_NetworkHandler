package mcmgnetwork.mcmg_networkhandler.protocols;

/**
 * Description: <p>
 *  Stores MCMG network server statuses recognized by the Velocity proxy server's MCMG_NetworkHandler plugin used for
 *  handling plugin messages.
 *
 *  <p>Author(s): Miles Bovero
 *  <p>Date Created: 5/10/24
 */
public class ServerStatuses
{
    /**
     * Indicates that all servers of a requested type are full
     */
    public static final String FULL = "full";

    /**
     * Indicates that an attempted server creation/initialization failed
     */
    public static final String FAILED_INITIALIZATION = "failed_initialization";

    /**
     * Indicates that a new server was successfully created and began initialization
     */
    public static final String BEGAN_INITIALIZATION = "began_initialization";

    /**
     * Indicates that a server is actively being initialized
     */
    public static final String INITIALIZING = "initializing";

    /**
     * Indicates that a server is able to be transferred to
     */
    public static final String TRANSFERABLE = "transferable";
}
