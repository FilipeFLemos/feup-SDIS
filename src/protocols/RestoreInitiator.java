package protocols;

import message.Message;
import peer.PeerController;
import channels.Channel;

import java.util.ArrayList;

public class RestoreInitiator implements Runnable{

    private String filePath;
    private PeerController peerController;
    private Channel channel;

    /**
     * Instantiates a new Restore initiator.
     *  @param filePath the file path
     * @param channel  the message
     */
    public RestoreInitiator(PeerController peerController, String filePath, Channel channel) {
        this.peerController = peerController;
        this.channel = channel;
        this.filePath = filePath;
    }

    /**
      * Method to be executed when thread starts running. Executes the restore protocols as an initiator peer
      */
    @Override
    public void run() {
        String fileID = peerController.getBackedUpFileID(filePath);
        if(fileID == null) {
            System.out.println("Restore Error: file " + filePath + " is not backed up.");
            return;
        }

        int chunkAmount = peerController.getBackedUpFileChunkAmount(filePath);
        if(chunkAmount == 0) {
            System.out.println("Restore Error: error retrieving chunk ammount.");
            return;
        }

        ArrayList<Message> chunks = new ArrayList<>();
        for(int i = 0; i < chunkAmount; i++) {
            chunks.add(new Message(peerController.getVersion(), peerController.getPeerId(), fileID, null, Message.MessageType.GETCHUNK, i));
        }

        peerController.addToRestoringFiles(fileID, filePath, chunkAmount);
        System.out.println("Restoring file with " + chunkAmount + " chunks");

        for(Message chunk : chunks){
            channel.sendMessage(chunk);
            System.out.println("Sent " + chunk.getMessageType() + " message: " + chunk.getChunkNo());
        }
    }
}
