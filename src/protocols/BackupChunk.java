package protocols;

import message.Message;
import channels.Channel;
import peer.PeerState;
import utils.Globals;
import user_interface.UI;

public class BackupChunk implements Runnable {

    private Message message;
    private Channel channel;
    private PeerState peerState;

    /**
      * Instantiates a new BackupChunk protocols
      *
      * @param peerState the peer's peerState
      * @param chunk the target chunk
      * @param replicationDeg the desired replication degree
      * @param channel the helper channel
      */
    public BackupChunk(PeerState peerState, Message chunk, int replicationDeg, Channel channel) {
        this.peerState = peerState;
        this.channel = channel;
        if(chunk.getSenderId() == -1) {
            peerState.setAtomicBoolean();
        }
        createPUTCHANK(chunk, replicationDeg);
    }

    public BackupChunk(PeerState peerState, Message putchunk, Channel channel) {
        this.peerState = peerState;
        this.channel = channel;
        this.message = putchunk;
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
      * Method to be executed when thread starts running. Executes the backup protocols for a specific chunk as the initiator peer
      */
    @Override
    public void run() {
        UI.printInfo("----------- Executing Chunk Backup Protocol ----------");

        if(peerState.getChunkRepDeg(message) >= message.getReplicationDeg()) {
            UI.printWarning("Chunk " + message.getChunkNo() + " replication degree was achived in the meantime");
            UI.printInfo("------------------------------------------------------");
            return;
        }

        peerState.listenForSTORED(message);

        int tries = 0;
        int waitTime = 500;

        do {
            tries++; waitTime *= 2;

            if(tries > Globals.MAX_PUTCHUNK_TRIES) {
                UI.printError("Aborting backup, attempt limit reached");
                UI.printInfo("------------------------------------------------------");
                break;
            }
            channel.sendMessage(message);
        } while(!hasDesiredReplicationDeg(waitTime));
        UI.printInfo("------------------------------------------------------");
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

        return peerState.getChunkRepDeg(message) >= message.getReplicationDeg();
    }
}
