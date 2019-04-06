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

import static utils.Utils.MAX_MESSAGE_SIZE;
import static utils.Utils.MAX_THREADS;

public class Channel implements Runnable{

    private String type;
    private InetAddress address;
    private int port;
    private MessageHandler messageHandler;

    private MulticastSocket multicastSocket;
    private ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(MAX_THREADS);
    boolean isActive;

    public Channel(String type, String address, int port, MessageHandler messageHandler) throws IOException {
        this.type = type;
        this.address = InetAddress.getByName(address);
        this.port = port;
        this.messageHandler = messageHandler;
        isActive = true;

        multicastSocket = new MulticastSocket(port);
        multicastSocket.setTimeToLive(1);
        multicastSocket.joinGroup(this.address);

        UI.printBoot("Joined " + type + " on " + address + " at port " + port);
    }

    @Override
    public void run() {
        byte[] packet = new byte[MAX_MESSAGE_SIZE];
        DatagramPacket multicastPacket = new DatagramPacket(packet, packet.length);
        while(true) {
            try {
                multicastSocket.receive(multicastPacket);
                Message message = new Message(multicastPacket.getData(), multicastPacket.getLength());
                messageHandler.handleMessage(message, multicastPacket.getAddress());
            } catch (IOException e) {
                UI.printError("Failed to receive message in " + type + " on port " + port);
                e.printStackTrace();
            }
        }
    }

    /**
     * Abstracts channels from defining if the message has to be sent with or without body.
     *
     * @param message - the message to be sent
     */
    public void sendMessage(Message message) {
        sendMessage(message,true);
        System.out.println(message);
    }

    /**
     * Sends a message through the multicastSocket.
     *
     * @param message the message to be sent
     */
    public void sendMessage(Message message, boolean sendBody) {
        byte[] packet = message.getPacket(sendBody);

        try {
            this.multicastSocket.send(new DatagramPacket(packet, packet.length, address, port));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Schedules a message to be sent after a random delay.
     * @param max - the max delay
     * @param message - the message to be sent
     */
    void sendWithRandomDelay(int max, Message message) {
        scheduledExecutorService.schedule(() -> sendMessage(message), Utils.getRandom(0, max), TimeUnit.MILLISECONDS);
    }
}
