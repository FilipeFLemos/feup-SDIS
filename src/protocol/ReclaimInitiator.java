package protocol;

import javafx.util.Pair;
import message.Message;
import peer.PeerController;
import storage.FileSystem;

public class ReclaimInitiator implements Runnable{

    private PeerController peerController;
    private long space;

    /**
     * Instantiates a new Reclaim initiator.
     *
     * @param space the space
     */
    public ReclaimInitiator(PeerController peerController, long space) {
        this.peerController = peerController;
        this.space = space;
    }

    /**
      * Method to be executed when thred starts running. Executes the reclaim protocol as an initiator peer
      */
    @Override
    public void run() {
        if(reclaimSpace(space))
            System.out.println("Successfully reclaimed down to " + space + " kB");
        else
            System.out.println("Couldn't reclaim down to " + space + " kB");
    }

    /**
     * Tries to reclaim some local space (executes the reclaim protocol)
     *
     * @param targetSpaceKb the target space, in kB
     * @return true
     */
    private boolean reclaimSpace(long targetSpaceKb) {
        FileSystem fileSystem = peerController.getFileSystem();
        long targetSpace = targetSpaceKb * 1000; //kbs to bytes

        while(fileSystem.getUsedStorage() > targetSpace) {
            Pair<String, Integer> toDelete = peerController.getMostSatisfiedChunk();

            // no more chunks to delete
            if (toDelete == null) {
                System.out.println("Nothing to delete");
                return fileSystem.getUsedStorage() < targetSpace;
            }

            String fileID = toDelete.getKey();
            int chunkIndex = toDelete.getValue();

            System.out.println("Deleting " + fileID + " - " + chunkIndex);
            peerController.deleteChunk(fileID, chunkIndex, true);

            Message removedMessage = new Message(peerController.getVersion(), peerController.getPeerId(), fileID, null, Message.MessageType.REMOVED, chunkIndex);
            peerController.getMCReceiver().sendMessage(removedMessage);
        }

        return true;
    }
}
