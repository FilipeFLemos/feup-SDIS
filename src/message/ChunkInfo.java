package message;

import java.io.Serializable;
import java.util.ArrayList;

public class ChunkInfo implements Comparable<ChunkInfo>, Serializable {

    private static final long serialVersionUID = 1L;
    
    private int currentReplicationDeg;
    private int desiredReplicationDeg;
    private ArrayList<Integer> peersWithChunk;

    /**
     * Instantiates a new Chunk info.
     *
     * @param desiredReplicationDeg the desired rep degree
     * @param currentReplicationDeg  the actual rep degree
     */
    public ChunkInfo(int desiredReplicationDeg, int currentReplicationDeg) {
        this.desiredReplicationDeg = desiredReplicationDeg;
        this.currentReplicationDeg = currentReplicationDeg;
        this.peersWithChunk = new ArrayList<>();
    }

    /**
     * Gets actual replication degree.
     *
     * @return the actual replication degree
     */
    public int getCurrentReplicationDeg() {
        return currentReplicationDeg;
    }

    /**
     * Gets desired replication degree.
     *
     * @return the desired replication degree
     */
    public int getDesiredReplicationDeg() {
        return desiredReplicationDeg;
    }

    /**
     * Increment actual replication degree.
     */
    public void incActualReplicationDegree() {
        currentReplicationDeg++;
    }

    /**
     * Decrement actual replication degree.
     */
    public void decreaseCurrentReplicationDeg() {
        currentReplicationDeg--;
    }

    /**
     * Gets the chunk's degree satisfaction
     *
     * @return the degree satisfaction
     */
    public int getDegreeSatisfaction() {
        return currentReplicationDeg - desiredReplicationDeg;
    }

    /**
     * Checks if observed replication degree has reached the desired level.
     *
     * @return the boolean
     */
    public boolean isDegreeSatisfied() {
        return currentReplicationDeg >= desiredReplicationDeg;
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

    /**
     * Add peer that backs up the chunk.
     *
     * @param peerID the peer id
     */
    public void addPeer(int peerID) {
        peersWithChunk.add(peerID);
    }

    /**
      * Compares two ChunkInfo objects by their satisfaction degree (difference between actual and desired replication degree)
      * @param o object to compare to
      * @return difference between the two satisfaction degrees
      */
    @Override
    public int compareTo(ChunkInfo o) {
        return this.getDegreeSatisfaction() - o.getDegreeSatisfaction();
    }

}
