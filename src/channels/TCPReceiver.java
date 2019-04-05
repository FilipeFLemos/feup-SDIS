package channels;

import message.Message;
import utils.Globals;


import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import user_interface.UI;

public class TCPReceiver implements Runnable {

    private MessageHandler messageHandler;
    private ServerSocket serverSocket;
    private ExecutorService threadPool = Executors.newFixedThreadPool(Globals.MAX_TCP_SOCKET_THREADS);
    private boolean isRestoring;

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
            UI.print("ServerSocket already working, no need to open again");
        }
        isRestoring = true;
    }

    /**
      * Method ran when thread starts executing. Submits handler to the thread pool
      */
    @Override
    public void run() {
        while (isRestoring) {
            try {
                Socket socket = serverSocket.accept();
                threadPool.submit(() -> listenClientMessages(socket));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void close(){
        try {
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
            UI.print("Error Closing TCP Socket");
        }
    }

    /**
      * Socket handler.
      */
    private void listenClientMessages(Socket socket) {
        ObjectInputStream stream = null;

        try {
            stream = new ObjectInputStream(socket.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
            UI.print("Error reading message from TCP Server");
        }

        while(true) {
            Message message = null;
            try {
                message = (Message) stream.readObject();
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
            if(message == null){
                break;
            }
            messageHandler.handleMessage(message, null);
            UI.print("Received CHUNK message " + message.getChunkNo() + " via TCP");

            try {
                stream = new ObjectInputStream(socket.getInputStream());
            }
            catch (IOException e) {
                UI.print("Closing TCP socket...");
                try {
                    socket.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                break;
            }
        }
    }
}
