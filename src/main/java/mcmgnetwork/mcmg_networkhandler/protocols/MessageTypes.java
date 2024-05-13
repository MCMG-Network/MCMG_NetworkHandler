package mcmgnetwork.mcmg_networkhandler.protocols;

/**
 * Description: <p>
 *  Stores plugin messaging channel message types for easy reference.
 *
 *  <p>Author(s): Miles Bovero
 *  <p>Date Created: 5/7/24
 */
public class MessageTypes
{
    /**
     * A Paper message type; connects the specified player to the specified server.
     */
    public static final String TRANSFER = "ConnectOther";

    /**
     * DataOutput should contain LOBBY_TRANSFER_REQUEST, [player to be transferred], [lobby server type to transfer to]
     * <p>
     * Causes the proxy server to return a SERVER_TRANSFER_RESPONSE plugin message type.
     */
    public static final String LOBBY_TRANSFER_REQUEST = "LobbyTransferRequest";

    /**
     * The message sent by the proxy server in response to a LOBBY_TRANSFER_REQUEST, providing further information and
     * instructions to the requesting server plugin.
     * <p>
     * Contains the status of the LOBBY_TRANSFER_REQUEST's specified server type ("transferable", "initializing", etc.),
     * the name of the player to be transferred, and - if any server of that type is online/transferable - the name of
     * an online server to transfer the player to.
     */
    public static final String LOBBY_TRANSFER_RESPONSE = "LobbyTransferResponse";
}
