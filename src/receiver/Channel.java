package receiver;

import message.PackedMessage;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

public class Channel {

    /**
     * The channel's address.
     */
    protected InetAddress address;

    /**
     * The channel's port.
     */
    protected int port;

    /**
     * The channel's socket.
     */
    protected MulticastSocket socket;

    /**
     * Instantiates a new Channel.
     *
     * @param address the address
     * @param port    the port
     * @throws IOException
     */
    public Channel(String address, int port) throws IOException {
        this.socket = new MulticastSocket(port);
        this.socket.setTimeToLive(1);

        this.address = InetAddress.getByName(address);
        this.port = port;

        System.out.println("Joined Multicast Receiver " + address + ":" + port);
    }

    /**
     * Sends a packedMessage encapsulated in a PackedMessage object to the channel
     *
     * @param packedMessage the packedMessage to be sent
     */
    public void sendMessage(PackedMessage packedMessage) {
        byte[] rbuf = packedMessage.buildMessagePacket();
        try {
            this.socket.send(new DatagramPacket(rbuf, rbuf.length, address, port));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
