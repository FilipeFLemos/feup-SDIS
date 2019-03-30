package protocol;

import peer.Peer;

public class ReclaimInitiator implements Runnable{

    private long space;
    private Peer peer;

    /**
     * Instantiates a new Reclaim initiator.
     *
     * @param peer  the peer
     * @param space the space
     */
    public ReclaimInitiator(Peer peer, long space) {
        this.peer = peer;
        this.space = space;
    }

    /**
      * Method to be executed when thred starts running. Executes the reclaim protocol as an initiator peer
      */
    @Override
    public void run() {
        if(peer.getController().reclaimSpace(space))
            System.out.println("Successfully reclaimed down to " + space + " kB");
        else
            System.out.println("Couldn't reclaim down to " + space + " kB");
    }
}
