package channels;

import message.Message;


import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import user_interface.UI;
import utils.Utils;

public class TCPReceiver implements Runnable {

    private MessageHandler messageHandler;
    private ServerSocket serverSocket;
    private ExecutorService threadPool = Executors.newFixedThreadPool(Utils.MAX_THREADS);
    private boolean isRestoring;

    public TCPReceiver(int port, MessageHandler messageHandler) {
        this.messageHandler = messageHandler;
        try {
            this.serverSocket = new ServerSocket(port);
        } catch (IOException e) {
            UI.print("ServerSocket already working, no need to open again");
        }
        isRestoring = true;
    }

    @Override
    public void run() {
        while (isRestoring) {
            try {
                Socket socket = serverSocket.accept();
                threadPool.submit(() -> listenForCHUNKS(socket));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Listens on the TCP channel for CHUNK messages.
     * @param socket - the client socket
     */
    private void listenForCHUNKS(Socket socket) {
        ObjectInputStream stream = null;

        do{
            try {
                stream = new ObjectInputStream(socket.getInputStream());
            } catch (IOException e) {
                e.printStackTrace();
                UI.print("Error reading message from TCP Server");
            }

            if(stream == null){
                continue;
            }

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
            UI.print("Received CHUNK message " + message.getChunkNo() + " via the TCP connection");
        }while(true);
    }
}
