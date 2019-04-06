package storage;

import java.io.Serializable;
import java.util.ArrayList;

public class ChunkInfo implements Serializable {

    private static final long serialVersionUID = 1L;
    private int currentReplicationDeg;
    private int desiredReplicationDeg;
    private int size = 0;
    private ArrayList<Integer> peersWithChunk;
    private byte[] body = null;

    /**
     * Constructor for saving the desired and the current Replication Degree
     *
     * @param desiredReplicationDeg - the desired replication Degree
     * @param currentReplicationDeg - the currentReplication Degree
     */
    public ChunkInfo(int desiredReplicationDeg, int currentReplicationDeg) {
        this.desiredReplicationDeg = desiredReplicationDeg;
        this.currentReplicationDeg = currentReplicationDeg;
        this.peersWithChunk = new ArrayList<>();
    }

    /**
     * Constructor for providing the size of the chunk.
     *
     * @param desiredReplicationDeg - the desired replication Degree
     * @param currentReplicationDeg - the currentReplication Degree
     * @param size
     */
    public ChunkInfo(int desiredReplicationDeg, int currentReplicationDeg, int size) {
        this(desiredReplicationDeg, currentReplicationDeg);
        this.size = size;
    }

    public int getCurrentReplicationDeg() {
        return currentReplicationDeg;
    }

    public int getDesiredReplicationDeg() {
        return desiredReplicationDeg;
    }

    public void increaseCurrentRepDeg() {
        currentReplicationDeg++;
    }

    public void decreaseCurrentRepDeg() {
        currentReplicationDeg--;
    }

    public boolean achievedDesiredRepDeg() {
        return currentReplicationDeg >= desiredReplicationDeg;
    }

    public byte[] getBody() {
        return body;
    }

    public void setBody(byte[] body) {
        this.body = body;
    }

    /**
     * Computes the difference between the current and the desired replication degree.
     *
     * @return The difference value
     */
    public int getReplicationDegDifference() {
        return currentReplicationDeg - desiredReplicationDeg;
    }

    public int getSize() {
        return size;
    }

    /**
     * Adds a peer that backs up the chunk.
     *
     * @param peerId - the peer id
     */
    public void addPeer(int peerId) {
        peersWithChunk.add(peerId);
    }

    /**
     * Removes a peer that stopped backing up the chunk.
     *
     * @param peerId - the peer id
     */
    public void removePeer(Integer peerId) {
        peersWithChunk.remove(peerId);
    }

    /**
     * Checks if the chunk is being backed up by a peer.
     *
     * @param peerId - the peer id
     * @return true if positive and false otherwise
     */
    public boolean isBackedUpByPeer(int peerId) {
        return peersWithChunk.contains(peerId);
    }

}
