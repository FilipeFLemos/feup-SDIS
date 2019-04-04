package channels;

import message.Message;
import utils.Globals;
import user_interface.UI;


import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TCPReceiver implements Runnable {

    private ServerSocket serverSocket;

    private ExecutorService threadPool = Executors.newFixedThreadPool(Globals.MAX_TCP_SOCKET_THREADS);

    private MessageHandler messageHandler;

    /**
      * Instantiates a socket channels
      *
      * @param port channels port
      * @param messageHandler channels messageHandler
      */
    public TCPReceiver(int port, MessageHandler messageHandler) {
        this.messageHandler = messageHandler;
        try {
            this.serverSocket = new ServerSocket(port);
        } catch (IOException e) {
            System.out.println("ServerSocket already working, no need to open again");
        }
    }

    /**
      * Method ran when thread starts executing. Submits handler to the thread pool
      */
    @Override
    public void run() {
        while (true) {
            try {
                Socket client = serverSocket.accept();
                System.out.println("TCP Client joined");
                threadPool.submit(() -> {
                    try {
                        socketHandler(client);
                    } catch (IOException | ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
      * Socket handler.
      *
      * @param client - the client socket
      * @throws IOException
      * @throws ClassNotFoundException
      */
    private void socketHandler(Socket client) throws IOException, ClassNotFoundException {
        Message message = null;
        try {
            ObjectInputStream objectInputStream = new ObjectInputStream(client.getInputStream());
            message = (Message) objectInputStream.readObject();
            objectInputStream.close();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            System.out.println("Error reading message from TCP Server");
        }

        if(message == null)
            return;

        System.out.println("Received CHUNK message " + message.getChunkNo() + " via TCP");
        messageHandler.handleMessage(message, null);
        threadPool.submit(() -> {
            try {
                socketHandler(client);
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        });
    }
}
