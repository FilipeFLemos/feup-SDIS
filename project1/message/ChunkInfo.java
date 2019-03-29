package message;

import java.io.Serializable;
import java.util.ArrayList;

public class ChunkInfo implements Comparable<ChunkInfo>, Serializable {

    private int desiredReplicationDegree;
    private int actualReplicationDegree;
    private ArrayList<Integer> peerList;

    /**
     * Instantiates a new Chunk info.
     *
     * @param desiredRepDegree the desired rep degree
     * @param actualRepDegree  the actual rep degree
     */
    public ChunkInfo(int desiredRepDegree, int actualRepDegree) {
        this.desiredReplicationDegree = desiredRepDegree;
        this.actualReplicationDegree = actualRepDegree;
        this.peerList = new ArrayList<>();
    }

    /**
     * Gets actual replication degree.
     *
     * @return the actual replication degree
     */
    public int getActualReplicationDegree() {
        return actualReplicationDegree;
    }

    /**
     * Gets desired replication degree.
     *
     * @return the desired replication degree
     */
    public int getDesiredReplicationDegree() {
        return desiredReplicationDegree;
    }

    /**
     * Increment actual replication degree.
     */
    public void incActualReplicationDegree() {
        actualReplicationDegree++;
    }

    /**
     * Decrement actual replication degree.
     */
    public void decActualReplicationDegree() {
        actualReplicationDegree--;
    }

    /**
     * Gets the chunk's degree satisfaction
     *
     * @return the degree satisfaction
     */
    public int getDegreeSatisfaction() {
        return actualReplicationDegree - desiredReplicationDegree;
    }

    /**
     * Checks if observed replication degree has reached the desired level.
     *
     * @return the boolean
     */
    public boolean isDegreeSatisfied() {
        return actualReplicationDegree >= desiredReplicationDegree;
    }

    /**
     * Checks if the chunk is being backed up by a particular peer.
     *
     * @param peerID the peer id
     * @return the boolean
     */
    public boolean isBackedUpByPeer(int peerID) {
        return peerList.contains(peerID);
    }

    /**
     * Add peer that backs up the chunk.
     *
     * @param peerID the peer id
     */
    public void addPeer(int peerID) {
        peerList.add(peerID);
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
