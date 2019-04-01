package protocols;

import message.Message;
import storage.FileChunk;
import peer.PeerController;
import channels.Channel;
import storage.StorageManager;

public class ReclaimInitiator implements Runnable{

    private PeerController peerController;
    private long space;
    private Channel mcChannel;

    /**
     * Instantiates a new Reclaim initiator.
     *
     * @param space the space
     */
    public ReclaimInitiator(PeerController peerController, long space, Channel mcChannel) {
        this.peerController = peerController;
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
            System.out.println("Couldn't reclaim down to " + space + " kB");
    }

    /**
     * Tries to reclaim some local space (executes the reclaim protocols)
     *
     * @param targetSpaceKb the target space, in kB
     * @return true
     */
    private boolean reclaimSpace(long targetSpaceKb) {
        StorageManager storageManager = peerController.getStorageManager();
        long targetSpace = targetSpaceKb * 1000; //kbs to bytes

        while(storageManager.getUsedSpace() > targetSpace) {
            FileChunk toDelete = peerController.getMostStoredChunk();

            // no more chunks to delete
            if (toDelete == null) {
                System.out.println("Nothing to delete");
                return storageManager.getUsedSpace() < targetSpace;
            }

            String fileID = toDelete.getFileId();
            int chunkIndex = toDelete.getChunkNo();

            System.out.println("Deleting " + fileID + " - " + chunkIndex);
            peerController.deleteChunk(fileID, chunkIndex, true);

            Message removedMessage = new Message(peerController.getVersion(), peerController.getServerId(), fileID, null, Message.MessageType.REMOVED, chunkIndex);
            mcChannel.sendMessage(removedMessage);
        }

        return true;
    }
}
