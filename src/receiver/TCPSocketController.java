package receiver;

import message.Message;
import utils.Globals;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TCPSocketController {

    private ExecutorService threadPool = Executors.newFixedThreadPool(Globals.MAX_TCP_SOCKET_THREADS);

    private ConcurrentHashMap<InetAddress, Socket> sockets;

    private int port;

    /**
      * Instantiates a new TCPSocketController
      *
      * @param port controller port
      */
    public TCPSocketController(int port) {
        this.port = port;
        sockets = new ConcurrentHashMap<>();
    }

    /**
      * Send a message
      *
      * @param message message to be sent
      * @param address destination address
      */
    public synchronized void sendMessage(Message message, InetAddress address) {
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
                stream.writeObject(message);
                System.out.println("Sent CHUNK message " + message.getChunkIndex() + " via TCP");
            } catch (IOException e) {
                System.out.println("Closing TCP socket...");
                try { socket.close(); }
                catch (IOException e1) { }
            }
        });
    }
}
