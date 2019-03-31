package protocols;

import message.Message;
import peer.Peer;
import channels.Channel;
import utils.Utils;

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
        String fileID = Utils.getFileID(filePath);

        Message message = new Message(peer.getVersion(),peer.getPeerId(),fileID, null, Message.MessageType.DELETE);
        channel.sendMessage(message);
    }
}
