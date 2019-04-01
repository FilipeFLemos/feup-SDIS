package message;

import java.io.Serializable;
import java.util.ArrayList;

public class ChunkInfo implements Comparable<ChunkInfo>, Serializable {

    private static final long serialVersionUID = 1L;
    private int currentReplicationDeg;
    private int desiredReplicationDeg;
    private ArrayList<Integer> peersWithChunk;

    public ChunkInfo(int desiredReplicationDeg, int currentReplicationDeg) {
        this.desiredReplicationDeg = desiredReplicationDeg;
        this.currentReplicationDeg = currentReplicationDeg;
        this.peersWithChunk = new ArrayList<>();
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
     * Gets the chunk's degree satisfaction
     *
     * @return the degree satisfaction
     */
    private int getDegreeSatisfaction() {
        return currentReplicationDeg - desiredReplicationDeg;
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
     * Checks if the chunk is being backed up by a particular peer.
     *
     * @param peerID the peer id
     * @return the boolean
     */
    public boolean isBackedUpByPeer(int peerID) {
        return peersWithChunk.contains(peerID);
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
