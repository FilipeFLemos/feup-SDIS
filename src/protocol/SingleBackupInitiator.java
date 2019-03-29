package protocol;

import message.PackedMessage;
import message.MessageType;
import receiver.Receiver;
import peer.PeerController;
import utils.Globals;

public class SingleBackupInitiator implements Runnable {

    private PackedMessage packedMessage;
    private Receiver receiver;
    private PeerController controller;

    /**
      * Instantiates a new SingleBackupInitiator protocol
      *
      * @param controller the peer's controller
      * @param chunk the target chunk
      * @param replicationDegree the desired replication degree
      * @param receiver the helper receiver
      */
    public SingleBackupInitiator(PeerController controller, PackedMessage chunk, int replicationDegree, Receiver receiver) {
        //create putchunk packedMessage from chunk
        chunk.setReplicationDeg(replicationDegree);
        chunk.setType(MessageType.PUTCHUNK);

        packedMessage = chunk;
        this.controller = controller;
        this.receiver = receiver;
    }

    /**
      * Method to be executed when thread starts running. Executes the backup protocol for a specific chunk as the initiator peer
      */
    @Override
    public void run() {
        //if chunk degree was satisfied meanwhile, cancel
        if(controller.getBackedUpChunkRepDegree(packedMessage) >= packedMessage.getReplicationDeg()) {
            System.out.println("Chunk " + packedMessage.getChunkIndex() + " satisfied meanwhile, canceling");
            return;
        }

        // notify controller to listen for this chunk's stored messages
        controller.backedUpChunkListenForStored(packedMessage);

        int tries = 0;
        int waitTime = 500;

        do {
            receiver.sendMessage(packedMessage);
            tries++; waitTime *= 2;

            if(tries > Globals.MAX_PUTCHUNK_TRIES) {
                System.out.println("Aborting backup, attempt limit reached");
                return;
            }
        } while(!confirmStoredMessage(packedMessage, waitTime));
    }

    /**
      * Checks if the desired replication degree for the chunk has been met
      *
      * @param packedMessage packedMessage containing information about the chunk
      * @param waitTime max delay before checking
      * @return true if desired replication degree has been met, false otherwise
      */
    private boolean confirmStoredMessage(PackedMessage packedMessage, int waitTime) {
        try {
            //TODO: remove sleeps
            Thread.sleep(waitTime);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return controller.getBackedUpChunkRepDegree(packedMessage) >= packedMessage.getReplicationDeg();

    }
}
