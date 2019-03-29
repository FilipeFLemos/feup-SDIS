package message;

public class GetChunkMessage extends Message {

    /**
     * Instantiates a new Get chunk message.
     *
     * @param version    the version
     * @param peerID     the peer id
     * @param fileID     the file id
     * @param chunkIndex the chunk index
     */
    public GetChunkMessage(String version, int peerID, String fileID, int chunkIndex) {
        super(version, peerID, fileID, null);

        this.type = MessageType.GETCHUNK;
        this.chunkNr = chunkIndex;
    }
}
