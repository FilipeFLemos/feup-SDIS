package protocol;

import message.Message;
import receiver.Channel;
import peer.PeerController;
import utils.Globals;

public class BackupChunk implements Runnable {

    private Message message;
    private Channel channel;
    private PeerController peerController;

    /**
      * Instantiates a new BackupChunk protocol
      *
      * @param peerController the peer's peerController
      * @param chunk the target chunk
      * @param replicationDeg the desired replication degree
      * @param channel the helper channel
      */
    public BackupChunk(PeerController peerController, Message chunk, int replicationDeg, Channel channel) {
        this.peerController = peerController;
        this.channel = channel;
        createPUTCHANK(chunk, replicationDeg);
    }

    /**
     * Create PUTCHUNK message from REMOVED message.
     * @param chunk
     * @param replicationDeg
     */
    private void createPUTCHANK(Message chunk, int replicationDeg){
        message = chunk;
        message.setMessageType(Message.MessageType.PUTCHUNK);
        message.setReplicationDeg(replicationDeg);
    }

    /**
      * Method to be executed when thread starts running. Executes the backup protocol for a specific chunk as the initiator peer
      */
    @Override
    public void run() {
        //if chunk degree was satisfied meanwhile, cancel
        if(peerController.getBackedUpChunkRepDegree(message) >= message.getReplicationDeg()) {
            System.out.println("Chunk " + message.getChunkNo() + " satisfied meanwhile, canceling");
            return;
        }

        peerController.listenForSTORED(message);

        int tries = 0;
        int waitTime = 500;

        do {
            tries++; waitTime *= 2;

            if(tries > Globals.MAX_PUTCHUNK_TRIES) {
                System.out.println("Aborting backup, attempt limit reached");
                break;
            }
            channel.sendMessage(message);
        } while(!hasDesiredReplicationDeg(waitTime));
    }

    /**
      * Checks if the desired replication degree for the chunk has been met
      *
      * @param waitTime max delay before checking
      * @return true if desired replication degree has been met, false otherwise
      */
    private boolean hasDesiredReplicationDeg(int waitTime) {
        try {
            Thread.sleep(waitTime);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return peerController.getBackedUpChunkRepDegree(message) >= message.getReplicationDeg();
    }
}
