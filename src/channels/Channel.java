package channels;

import message.Message;
import utils.Utils;
import user_interface.UI;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Channel {

    private static final int MAX_RECEIVER_SENDING_THREADS = 50;

    /**
     * The address.
     */

    private InetAddress address;
    /**
     * The port.
     */
    private int port;

    /**
     * The socket.
     */
    private MulticastSocket socket;

    /**
     * The channel type.
     */
    private String type;

    /**
     * The MessageHandler.
     */
    private MessageHandler messageHandler;

    private ScheduledExecutorService threadPool = Executors.newScheduledThreadPool(MAX_RECEIVER_SENDING_THREADS);

    /**
     * Instantiates a new Channel.
     *
     *
     * @param type       Type of the channel created
     * @param address    the address
     * @param port       the port
     * @param messageHandler the messageHandler
     * @throws IOException the io exception
     */
    public Channel(String type, String address, int port, MessageHandler messageHandler) throws IOException {
        // create multicast socket
        this.socket = new MulticastSocket(port);
        this.socket.setTimeToLive(1);

        this.address = InetAddress.getByName(address);
        this.port = port;

        this.messageHandler = messageHandler;

        //join multicast group
        socket.joinGroup(this.address);

        startListening();

        UI.printBoot("Joined " + type + " on " + address + " at port " + port);
    }

    /**
      * Starts listening for messages, dispatching them as they are received
      */
    private void startListening() {
        new Thread(() -> {
            byte[] mbuf = new byte[65535];

            while(true) {
                DatagramPacket multicastPacket = new DatagramPacket(mbuf, mbuf.length);

                try {
                    socket.receive(multicastPacket);
                    Message message = new Message(multicastPacket.getData(), multicastPacket.getLength());
                    messageHandler.handleMessage(message, multicastPacket.getAddress());
                } catch (IOException e) {
                    UI.printError("Failed to receive message in " + this.type + " on port " + this.port);
                    e.printStackTrace();
                }
            }
        }).start();
    }

    /**
     * Sends a message through the socket.
     *
     * @param message the message to be sent
     */
    public void sendMessage(Message message) {
        byte[] rbuf = message.getPacket(true);

        try {
            this.socket.send(new DatagramPacket(rbuf, rbuf.length, address, port));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Sends a message through the socket.
     *
     * @param message the message to be sent
     */
    public void sendMessage(Message message, boolean sendBody) {
        byte[] rbuf = message.getPacket(sendBody);

        try {
            this.socket.send(new DatagramPacket(rbuf, rbuf.length, address, port));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Sends a message with random delay.
     *
     * @param min     the min delay
     * @param max     the max delay
     * @param message the message to be sent
     */
    public void sendWithRandomDelay(int min, int max, Message message) {
        threadPool.schedule(() -> {
            sendMessage(message);
        }, Utils.getRandom(min, max), TimeUnit.MILLISECONDS);
    }

}
