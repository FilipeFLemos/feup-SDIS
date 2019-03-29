package message;

public class PutChunkMessage extends PackedMessage {

    /**
     * Instantiates a new Put chunk message.
     *
     * @param version           the version
     * @param buf               the buf
     * @param fileID            the file id
     * @param chunkIndex        the chunk index
     * @param replicationDegree the replication degree
     * @param peerID            the peer id
     */
    public PutChunkMessage(String version, byte[] buf, String fileID, int chunkIndex, int replicationDegree, int peerID) {
        super(version, peerID, fileID, buf);

        this.type = MessageType.PUTCHUNK;
        this.repDegree = replicationDegree;
        this.chunkNr = chunkIndex;
    }
}
