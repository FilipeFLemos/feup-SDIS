package receiver;

import message.PackedMessage;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SocketController {

    private static final int MAX_TCP_SOCKET_THREADS = 50;

    private ExecutorService threadPool = Executors.newFixedThreadPool(MAX_TCP_SOCKET_THREADS);

    private ConcurrentHashMap<InetAddress, Socket> sockets;

    private int port;

    /**
      * Instantiates a new SocketController
      *
      * @param port controller port
      */
    public SocketController(int port) {
        this.port = port;
        sockets = new ConcurrentHashMap<>();
    }

    /**
      * Send a packedMessage
      *
      * @param packedMessage packedMessage to be sent
      * @param address destination address
      */
    public synchronized void sendMessage(PackedMessage packedMessage, InetAddress address) {
        threadPool.submit(() -> {
            Socket socket = null;

            try {
                socket = sockets.getOrDefault(address, new Socket(address, port));
                if(socket.isClosed())
                    socket = new Socket(address, port);
            } catch (IOException e) {
                e.printStackTrace();
            }

            ObjectOutputStream stream = null;
            try {
                stream = new ObjectOutputStream(socket.getOutputStream());
                stream.writeObject(packedMessage);
                System.out.println("Sent CHUNK packedMessage " + packedMessage.getChunkIndex() + " via TCP");
            } catch (IOException e) {
                System.out.println("Closing TCP socket...");
                try { socket.close(); }
                catch (IOException e1) { }
            }
        });
    }
}
