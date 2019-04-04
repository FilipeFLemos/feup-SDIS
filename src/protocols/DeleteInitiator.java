package protocols;

import message.Message;
import peer.Peer;
import channels.Channel;
import storage.FileInfo;
import utils.Utils;
import user_interface.UI;

import java.util.concurrent.ConcurrentHashMap;

public class DeleteInitiator implements Runnable{

    private String filePath;
    private Peer peer;
    private Channel channel;

    /**
     * Instantiates a new Delete initiator.
     *  @param peer     the peer
     * @param filePath the file path
     * @param channel  the message
     */
    public DeleteInitiator(Peer peer, String filePath, Channel channel) {
        this.peer = peer;
        this.channel = channel;
        this.filePath = filePath;
    }

    /**
      * Method to be executed when thread starts running. Executes the delete protocols as an initiator peer
      */
    @Override
    public void run() {

        UI.printInfo("-------------- Executing Delete Protocol -------------");

        String fileId = Utils.getFileID(filePath);
        ConcurrentHashMap<String, FileInfo> backedUpFiles = peer.getController().getBackedUpFiles();
        if(!backedUpFiles.containsKey(filePath)){
            UI.printWarning("File "+filePath+" is not being backed up");
            UI.printInfo("------------------------------------------------------");
            return;
        }

        Message message = new Message(peer.getVersion(),peer.getServerId(),fileId, null, Message.MessageType.DELETE);
        channel.sendMessage(message);
        peer.getController().deleteBackedUp(filePath);
        UI.printOK("Deleted file " + filePath);
        UI.printInfo("------------------------------------------------------");


    }
}
