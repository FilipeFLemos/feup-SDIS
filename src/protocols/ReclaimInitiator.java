package protocols;

import message.Message;
import peer.PeerState;
import storage.ChunkInfo;
import storage.FileChunk;
import channels.Channel;
import storage.StorageManager;
import user_interface.UI;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ReclaimInitiator implements Runnable {

    private PeerState peerState;
    private long space;
    private Channel mcChannel;

    public ReclaimInitiator(PeerState peerState, long space, Channel mcChannel) {
        this.peerState = peerState;
        this.space = space;
        this.mcChannel = mcChannel;
    }

    /**
     * Executes the reclaim protocol.
     * If the provided space amount is 0, it clears all the peer disk space.
     * Else, it removes just the required number of chunks to satisfy the request.
     */
    @Override
    public void run() {
        UI.printInfo("------------- Executing Reclaim Protocol -------------");

        StorageManager storageManager = peerState.getStorageManager();
        long targetSpace = space * 1000;
        if(reclaimSpace(storageManager, targetSpace)){
            UI.print("Successfully reclaimed disk space. New disk used space is " + storageManager.getUsedSpace());
        } else {
            UI.printError("Couldn't reclaim " + space + " kB");
        }

        UI.printInfo("------------------------------------------------------");
    }

    /**
     * Tries to reclaim the space amount specified by deleting the most stored chunks on the system (not local).
     *
     * @param targetSpace - the target space
     * @return the space reclaimed
     */
    private boolean reclaimSpace(StorageManager storageManager, long targetSpace) {
        while (storageManager.getUsedSpace() > targetSpace) {
            FileChunk mostStoredChunk = peerState.getMostStoredChunk();

            if (mostStoredChunk == null) {
                UI.printWarning("There is no chunk to be deleted");
                return storageManager.getUsedSpace() > targetSpace;
            }

            String fileId = mostStoredChunk.getFileId();
            int chunkNo = mostStoredChunk.getChunkNo();

            UI.print("Deleting " + fileId + " - " + chunkNo);
            peerState.deleteChunk(fileId, chunkNo, true);
            sendREMOVED(fileId, chunkNo);
        }

        return true;
    }

    /**
     * Creates and sends the REMOVED message to the channel.
     *
     * @param fileId - the file id
     * @param chunkNo - the chunk No
     */
    private void sendREMOVED(String fileId, int chunkNo) {
        Message removedMessage = new Message(peerState.getVersion(), peerState.getServerId(), fileId, null, Message.MessageType.REMOVED, chunkNo);
        mcChannel.sendMessage(removedMessage);
    }
}
