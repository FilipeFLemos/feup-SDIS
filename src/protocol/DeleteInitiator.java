package protocol;

import message.MessageType;
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
        String fileID = Utils.getFileID(file);

        Message message = new Message(peer.getProtocolVersion(),peer.getPeerId(),fileID, null, MessageType.DELETE);
        channel.sendMessage(message);
    }
}
