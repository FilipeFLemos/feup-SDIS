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
        StorageManager storageManager = peerState.getStorageManager();
        long targetSpace = space * 1000; //kbs to bytes
        if(targetSpace == 0){
            if(reclaimAllSpace(storageManager,targetSpace)){
                System.out.println("Successfully reclaimed all disk space");
            }else {
                UI.printError("Couldn't reclaim all disk space");
            }

        } else{
            long reclaimedSpace = reclaimSpace(storageManager, targetSpace);
            if(reclaimedSpace >= targetSpace)
                System.out.println("Successfully reclaimed disk space. New disk used space is " + storageManager.getUsedSpace());
            else
                UI.printError("Couldn't reclaim down to " + space + " kB");
        }
    }

    /**
     * Tries to reclaim some local space (executes the reclaim protocols)
     *
     * @param targetSpace the target space, in kB
     * @return true
     */
    private long reclaimSpace(StorageManager storageManager, long targetSpace) {
        long reclaimedSpace = 0;
        while(reclaimedSpace < targetSpace) {
            FileChunk toDelete = peerState.getMostStoredChunk();

            // no more chunks to delete
            if (toDelete == null) {
                UI.printWarning("Nothing to delete");
                return reclaimedSpace;
            }

            String fileID = toDelete.getFileId();
            int chunkIndex = toDelete.getChunkNo();

            System.out.println("Deleting " + fileID + " - " + chunkIndex);

            long spaceBeforeDeleting = storageManager.getUsedSpace();
            peerState.deleteChunk(fileID, chunkIndex, true);
            reclaimedSpace += (spaceBeforeDeleting - storageManager.getUsedSpace());

            sendREMOVED(fileID, chunkIndex);
        }
        return reclaimedSpace;
    }

    private boolean reclaimAllSpace(StorageManager storageManager, long targetSpace) {
        while(storageManager.getUsedSpace() > targetSpace) {
            FileChunk toDelete = peerState.getMostStoredChunk();

            // no more chunks to delete
            if (toDelete == null) {
                UI.printWarning("Nothing to delete");
                return storageManager.getUsedSpace() > targetSpace;
            }

            String fileID = toDelete.getFileId();
            int chunkIndex = toDelete.getChunkNo();

            System.out.println("Deleting " + fileID + " - " + chunkIndex);
            peerState.deleteChunk(fileID, chunkIndex, true);

            sendREMOVED(fileID, chunkIndex);
        }
        return true;
    }

    private void sendREMOVED(String fileID, int chunkIndex) {
        Message removedMessage = new Message(peerState.getVersion(), peerState.getServerId(), fileID, null, Message.MessageType.REMOVED, chunkIndex);
        mcChannel.sendMessage(removedMessage);
    }
}
