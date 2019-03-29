package protocol;

import message.Message;
import message.MessageType;
import receiver.Channel;
import peer.Peer;

import java.util.ArrayList;

public class RestoreInitiator extends ProtocolInitiator {

    private String filePath;

    /**
     * Instantiates a new Restore initiator.
     *
     * @param peer     the peer
     * @param filePath the file path
     * @param channel  the message
     */
    public RestoreInitiator(Peer peer, String filePath, Channel channel) {
        super(peer, channel);
        this.filePath = filePath;
    }

    /**
      * Method to be executed when thread starts running. Executes the restore protocol as an initiator peer
      */
    @Override
    public void run() {
        String fileID = peer.getController().getBackedUpFileID(filePath);
        if(fileID == null) {
            System.out.println("Restore Error: file " + filePath + " is not backed up.");
            return;
        }

        int chunkAmount = peer.getController().getBackedUpFileChunkAmount(filePath);
        if(chunkAmount == 0) {
            System.out.println("Restore Error: error retrieving chunk ammount.");
            return;
        }

        ArrayList<Message> getChunkList = new ArrayList<>();
        for(int i = 0; i < chunkAmount; i++) {
            getChunkList.add(new Message(peer.getProtocolVersion(), peer.getPeerId(), fileID, null, MessageType.GETCHUNK, i));
        }

        peer.getController().addToRestoringFiles(fileID, filePath, chunkAmount);
        System.out.println("Restoring file with " + chunkAmount + " chunks");

        sendMessages(getChunkList);
    }
}
