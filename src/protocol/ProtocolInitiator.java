package protocol;

import message.PackedMessage;
import receiver.Channel;
import peer.Peer;

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
     * @param packedMessageList  the message list
     */
    protected void sendMessages(ArrayList<PackedMessage> packedMessageList) {
        for(PackedMessage packedMessage : packedMessageList) {
            this.channel.sendMessage(packedMessage);
            System.out.println("Sent " + packedMessage.getType() + " packedMessage: " + packedMessage.getChunkIndex());
        }
    }

}
