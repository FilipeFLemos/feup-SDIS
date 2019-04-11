package channels;

import message.Message;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import user_interface.UI;
import utils.Utils;

public class TCPSender {

    private int port;
    private ConcurrentHashMap<InetAddress, Socket> sockets;
    private ExecutorService executorService = Executors.newFixedThreadPool(Utils.MAX_THREADS);

    public TCPSender(int port) {
        this.port = port;
        sockets = new ConcurrentHashMap<>();
    }

    /**
     * Starts a thread for each message to be sent.
     * If the socket for the specified address is still opened, uses it. Else, opens a new socket for that address and
     * adds it to the sockets map for future requests.
     * Finally sends the message.
     *
     * @param message - the message to be sent
     * @param address - the address of the destination
     */
    public synchronized void sendMessage(Message message, InetAddress address) {
        executorService.submit(() -> {
            Socket socket = null;

            if(sockets.containsKey(address)){
                socket = sockets.get(address);
                if(socket.isClosed()){
                    sockets.remove(address);
                    socket = null;
                }
            }

            if(socket == null){
                try {
                    socket = new Socket(address, port + message.getSenderId());
                    sockets.put(address, socket);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            ObjectOutputStream stream;
            try {
                stream = new ObjectOutputStream(socket.getOutputStream());
                stream.writeObject(message);
                UI.print("Sending CHUNK " + message.getChunkNo() + " via the TCP socket");
            } catch (IOException e) {
                UI.print("Closing TCP socket...");
                try {
                    socket.close();
                } catch (IOException e1) {
                    e.printStackTrace();
                }
            }
        });
    }
}