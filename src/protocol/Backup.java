package protocol;

import message.Message;
import message.MessageType;
import receiver.Receiver;
import peer.PeerController;
import utils.Globals;

public class Backup implements Runnable {

    private Message message;
    private Receiver receiver;
    private PeerController peerController;

    /**
      * Instantiates a new Backup protocol
      *
      * @param peerController the peer's peerController
      * @param chunk the target chunk
      * @param replicationDegree the desired replication degree
      * @param receiver the helper receiver
      */
    public Backup(PeerController peerController, Message chunk, int replicationDegree, Receiver receiver) {
        //create putchunk message from chunk
        chunk.setReplicationDeg(replicationDegree);
        chunk.setType(MessageType.PUTCHUNK);

        message = chunk;
        this.peerController = peerController;
        this.receiver = receiver;
    }

    /**
      * Method to be executed when thread starts running. Executes the backup protocol for a specific chunk as the initiator peer
      */
    @Override
    public void run() {
        //if chunk degree was satisfied meanwhile, cancel
        if(peerController.getBackedUpChunkRepDegree(message) >= message.getReplicationDeg()) {
            System.out.println("Chunk " + message.getChunkIndex() + " satisfied meanwhile, canceling");
            return;
        }

        // notify peerController to listen for this chunk's stored messages
        peerController.backedUpChunkListenForStored(message);

        int tries = 0;
        int waitTime = 500;

        do {
            receiver.sendMessage(message);
            tries++; waitTime *= 2;

            if(tries > Globals.MAX_PUTCHUNK_TRIES) {
                System.out.println("Aborting backup, attempt limit reached");
                return;
            }
        } while(!confirmStoredMessage(message, waitTime));
    }

    /**
      * Checks if the desired replication degree for the chunk has been met
      *
      * @param message message containing information about the chunk
      * @param waitTime max delay before checking
      * @return true if desired replication degree has been met, false otherwise
      */
    private boolean confirmStoredMessage(Message message, int waitTime) {
        try {
            //TODO: remove sleeps
            Thread.sleep(waitTime);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return peerController.getBackedUpChunkRepDegree(message) >= message.getReplicationDeg();

    }
}
