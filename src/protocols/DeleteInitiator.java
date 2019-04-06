package protocols;

import message.Message;
import peer.Peer;
import channels.Channel;
import storage.FileInfo;
import utils.Utils;
import user_interface.UI;

import java.util.concurrent.ConcurrentHashMap;

public class DeleteInitiator implements Runnable {

    private Peer peer;
    private String filePath;
    private Channel channel;

    public DeleteInitiator(Peer peer, String filePath, Channel channel) {
        this.peer = peer;
        this.filePath = filePath;
        this.channel = channel;
    }

    /**
     * Executes the delete protocol.
     * Starts by checking if the file is being backed up by the peer, aborting if otherwise.
     * Then, sends the DELETE message to the channel.
     */
    @Override
    public void run() {
        UI.printInfo("-------------- Executing Delete Protocol -------------");

        String fileId = Utils.getFileID(filePath);
        ConcurrentHashMap<String, FileInfo> backedUpFiles = peer.getPeerState().getBackedUpFiles();
        if (!backedUpFiles.containsKey(filePath)) {
            UI.printWarning("File " + filePath + " is not being backed up");
            UI.printInfo("------------------------------------------------------");
            return;
        }

        Message message = new Message(peer.getVersion(), peer.getServerId(), fileId, null, Message.MessageType.DELETE);
        channel.sendMessage(message);
        peer.getPeerState().deleteBackedUp(filePath);

        UI.printOK("Deleted file " + filePath);
        UI.printInfo("------------------------------------------------------");
    }
}
