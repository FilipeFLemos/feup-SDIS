package protocol;

import receiver.Channel;
import message.Message;
import peer.Peer;
import utils.Utils;

import java.io.File;

public class DeleteInitiator implements Runnable{

    private String filePath;
    private Peer peer;
    private Channel channel;

    /**
     * Instantiates a new Delete initiator.
     *
     * @param peer     the peer
     * @param filePath the file path
     * @param channel  the message
     */
    public DeleteInitiator(Peer peer, String filePath, Channel channel) {
        this.peer = peer;
        this.channel = channel;
        this.filePath = filePath;
    }

    /**
      * Method to be executed when thread starts running. Executes the delete protocol as an initiator peer
      */
    @Override
    public void run() {
        File file = new File(filePath);
        String fileID = Utils.getFileID(filePath);

        Message message = new Message(peer.getVersion(),peer.getPeerId(),fileID, null, Message.MessageType.DELETE);
        channel.sendMessage(message);
    }
}
