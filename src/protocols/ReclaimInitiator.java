package protocols;

import message.Message;
import peer.PeerState;
import storage.FileChunk;
import channels.Channel;
import storage.StorageManager;
import user_interface.UI;

public class ReclaimInitiator implements Runnable{

    private PeerState peerState;
    private long space;
    private Channel mcChannel;

    /**
     * Instantiates a new Reclaim initiator.
     *
     * @param space the space
     */
    public ReclaimInitiator(PeerState peerState, long space, Channel mcChannel) {
        this.peerState = peerState;
        this.space = space;
        this.mcChannel = mcChannel;
    }

    /**
      * Method to be executed when thred starts running. Executes the reclaim protocols as an initiator peer
      */
    @Override
    public void run() {
        if(reclaimSpace(space))
            System.out.println("Successfully reclaimed down to " + space + " kB");
        else
            UI.printError("Couldn't reclaim down to " + space + " kB");
    }

    /**
     * Tries to reclaim some local space (executes the reclaim protocols)
     *
     * @param targetSpaceKb the target space, in kB
     * @return true
     */
    private boolean reclaimSpace(long targetSpaceKb) {
        StorageManager storageManager = peerState.getStorageManager();
        long targetSpace = targetSpaceKb * 1000; //kbs to bytes

        while(storageManager.getUsedSpace() > targetSpace) {
            FileChunk toDelete = peerState.getMostStoredChunk();

            // no more chunks to delete
            if (toDelete == null) {
                UI.printWarning("Nothing to delete");
                return storageManager.getAvailableSpace() < targetSpace;
            }

            String fileID = toDelete.getFileId();
            int chunkIndex = toDelete.getChunkNo();

            System.out.println("Deleting " + fileID + " - " + chunkIndex);
            peerState.deleteChunk(fileID, chunkIndex);

            Message removedMessage = new Message(peerState.getVersion(), peerState.getServerId(), fileID, null, Message.MessageType.REMOVED, chunkIndex);
            mcChannel.sendMessage(removedMessage);
        }

        return true;
    }
}
