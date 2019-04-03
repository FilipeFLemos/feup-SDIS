package storage;

import java.io.Serializable;
import java.util.ArrayList;

public class ChunkInfo implements Serializable {

    private static final long serialVersionUID = 1L;
    private int currentReplicationDeg;
    private int desiredReplicationDeg;
    private int size = 0;
    private ArrayList<Integer> peersWithChunk;

    public ChunkInfo(int desiredReplicationDeg, int currentReplicationDeg) {
        this.desiredReplicationDeg = desiredReplicationDeg;
        this.currentReplicationDeg = currentReplicationDeg;
        this.peersWithChunk = new ArrayList<>();
    }

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

    /**
     * Computes the difference between the current and the desired replication degree.
     * @return The difference value
     */
    public int getReplicationDegDifference(){
        return currentReplicationDeg - desiredReplicationDeg;
    }

    public int getSize() {
        return size;
    }

    /**
     * Add peer that backs up the chunk.
     *
     * @param peerId the peer id
     */
    public void addPeer(int peerId) {
        peersWithChunk.add(peerId);
    }

    public void removePeer(Integer peerId) {
        peersWithChunk.remove(peerId);
    }

    /**
     * Checks if the chunk is being backed up by a particular peer.
     *
     * @param peerID the peer id
     * @return the boolean
     */
    public boolean isBackedUpByPeer(int peerID) {
        return peersWithChunk.contains(peerID);
    }

}