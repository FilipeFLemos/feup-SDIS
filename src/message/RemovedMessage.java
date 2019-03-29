package message;

public class RemovedMessage extends PackedMessage {
    /**
     * Instantiates a new Removed message.
     *
     * @param version    the version
     * @param peerID     the peer id
     * @param fileID     the file id
     * @param chunkIndex the chunk index
     */
    public RemovedMessage(String version, int peerID, String fileID, int chunkIndex) {
        super(version, peerID, fileID, null);
        this.type = MessageType.REMOVED;
        this.chunkNr = chunkIndex;
    }
}
