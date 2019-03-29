package receiver;

import message.PackedMessage;
import utils.Utils;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Receiver {

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
     * The Dispatcher.
     */
    private Dispatcher dispatcher;

    private ScheduledExecutorService threadPool = Executors.newScheduledThreadPool(MAX_RECEIVER_SENDING_THREADS);

    /**
     * Instantiates a new Receiver.
     *
     * @param address    the address
     * @param port       the port
     * @param dispatcher the dispatcher
     * @throws IOException the io exception
     */
    public Receiver(String address, int port, Dispatcher dispatcher) throws IOException {
        // create multicast socket
        this.socket = new MulticastSocket(port);
        this.socket.setTimeToLive(1);

        this.address = InetAddress.getByName(address);
        this.port = port;

        this.dispatcher = dispatcher;

        //join multicast group
        socket.joinGroup(this.address);

        startListening();

        System.out.println("Joined Multicast Receiver " + address + ":" + port);
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
                    this.socket.receive(multicastPacket);
                    this.dispatcher.handleMessage(multicastPacket.getData(), multicastPacket.getLength(), multicastPacket.getAddress());
                } catch (IOException e) {
                    System.out.println("Error receiving multicast message");
                    e.printStackTrace();
                }
            }
        }).start();
    }

    /**
     * Sends a packedMessage through the socket.
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

    /**
     * Sends a packedMessage through the socket.
     *
     * @param packedMessage the packedMessage to be sent
     */
    public void sendMessage(PackedMessage packedMessage, boolean sendBody) {
        byte[] rbuf = packedMessage.buildMessagePacket(sendBody);

        try {
            this.socket.send(new DatagramPacket(rbuf, rbuf.length, address, port));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Sends a packedMessage with random delay.
     *
     * @param min     the min delay
     * @param max     the max delay
     * @param packedMessage the packedMessage to be sent
     */
    public void sendWithRandomDelay(int min, int max, PackedMessage packedMessage) {
        threadPool.schedule(() -> {
            sendMessage(packedMessage);
        }, Utils.getRandomBetween(min, max), TimeUnit.MILLISECONDS);
    }

}
