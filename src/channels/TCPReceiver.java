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
                Socket socket = serverSocket.accept();
                threadPool.submit(() -> {
                    try {
                        socketHandler(socket);
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
      * @param socket the socket
      * @throws IOException
      * @throws ClassNotFoundException
      */
    private void socketHandler(Socket socket) throws IOException, ClassNotFoundException {
        ObjectInputStream stream = null;

        try {
            stream = new ObjectInputStream(socket.getInputStream());
        } catch (IOException e) {
            UI.printError("Error creating Object Input Stream");
            e.printStackTrace();
        }

        Message message;
        while((message = (Message) stream.readObject()) != null) {
            messageHandler.handleMessage(message, null);
            System.out.println("Received CHUNK message " + message.getChunkNo() + " via TCP");

            try {
                stream = new ObjectInputStream(socket.getInputStream());
            }
            catch (IOException e) {
                System.out.println("Closing TCP socket");
                socket.close();
                break;
            }
        }
    }
}
