package mcmgnetwork.mcmg_networkhandler.protocols;

/**
 * Description: <p>
 *  Stores plugin messaging channel message types for easy reference across the plugin.
 *
 *  <p>Author(s): Miles Bovero
 *  <p>Date Created: 5/7/24
 */
public class MessageTypes
{
    /**
     * Connects the specified player to the specified server.
     */
    public static final String TRANSFER = "ConnectOther";

    /**
     * DataOutput should contain SERVER_TRANSFER_REQUEST, [player to be transferred], [server type to transfer to]
     * <p>
     * Causes the proxy server to return a SERVER_TRANSFER_RESPONSE plugin message type.
     */
    public static final String SERVER_TRANSFER_REQUEST = "ServerTransferRequest";

    /**
     * The message sent by the proxy server in response to a SERVER_TRANSFER_REQUEST, confirming whether or not to
     * transfer the requesting player to another server.
     * <p>
     * Contains a boolean indicating whether or not an instance of the SERVER_TRANSFER_REQUEST's specified server type
     * is online, the player to be transferred, and - if any server of that type is online - the name of an online
     * server to transfer the player to.
     */
    public static final String SERVER_TRANSFER_RESPONSE = "ServerTransferResponse";
}
