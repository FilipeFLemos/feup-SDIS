package protocols;

import message.Message;
import peer.PeerState;
import channels.Channel;
import storage.FileInfo;
import user_interface.UI;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

public class RestoreInitiator implements Runnable{

    private String filePath;
    private PeerState peerState;
    private Channel channel;

    /**
     * Instantiates a new Restore initiator.
     *  @param filePath the file path
     * @param channel  the message
     */
    public RestoreInitiator(PeerState peerState, String filePath, Channel channel) {
        this.peerState = peerState;
        this.channel = channel;
        this.filePath = filePath;
    }

    /**
      * Method to be executed when thread starts running. Executes the restore protocols as an initiator peer
      */
    @Override
    public void run() {
        UI.printInfo("------------- Executing Restore Protocol -------------");
        ConcurrentHashMap<String, FileInfo> backedUpFilesByPaths = peerState.getBackedUpFiles();
        if(!backedUpFilesByPaths.containsKey(filePath)) {
            UI.printWarning("File " + filePath + " is not backed up.");
            UI.printInfo("------------------------------------------------------");
            return;
        }

        FileInfo fileInfo = backedUpFilesByPaths.get(filePath);
        String fileId = fileInfo.getFileId();
        int numberOfChunks = fileInfo.getNumberOfChunks();

        ArrayList<Message> chunks = new ArrayList<>();
        for(int i = 0; i < numberOfChunks; i++) {
            chunks.add(new Message(peerState.getVersion(), peerState.getServerId(), fileId, null, Message.MessageType.GETCHUNK, i));
        }

        peerState.addToRestoringFiles(fileId, fileInfo);
        System.out.println("Restoring file with " + numberOfChunks + " chunks");

        for(Message chunk : chunks){
            channel.sendMessage(chunk);
            System.out.println("Sent " + chunk.getMessageType() + " message: " + chunk.getChunkNo());
        }
        UI.printInfo("------------------------------------------------------");
    }
}
