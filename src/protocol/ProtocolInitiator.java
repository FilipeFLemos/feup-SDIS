package protocol;

import receiver.Channel;
import message.Message;
import peer.Peer;
import utils.Utils;

import java.util.ArrayList;

/**
  * Base protocol initiator
  */
public abstract class ProtocolInitiator implements Runnable {
    /**
     * The peer.
     */
    protected Peer peer;
    /**
     * The channel.
     */
    protected Channel channel;

    /**
     * Instantiates a new Protocol initiator.
     *
     * @param peer    the peer
     * @param channel the message
     */
    public ProtocolInitiator(Peer peer, Channel channel) {
        this.peer = peer;
        this.channel = channel;
    }

    /**
     * Send a list of messages to the channel.
     *
     * @param messageList  the message list
     */
    protected void sendMessages(ArrayList<Message> messageList) {
        for(Message message : messageList) {
            this.channel.sendMessage(message);
            System.out.println("Sent " + message.getType() + " message: " + message.getChunkIndex());
        }
    }

}
