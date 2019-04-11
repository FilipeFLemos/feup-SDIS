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
    private ExecutorService executorService = Executors.newFixedThreadPool(Utils.MAX_THREADS);
    private boolean isRestoring;

    public TCPReceiver(int port, MessageHandler messageHandler) {
        this.messageHandler = messageHandler;
        try {
            this.serverSocket = new ServerSocket(port);
        } catch (IOException e) {
            UI.printError("Error creating socket");
            e.printStackTrace();
        }
        isRestoring = true;
    }

    @Override
    public void run() {
        while (isRestoring) {
            try {
                Socket socket = serverSocket.accept();
                executorService.submit(() -> listenForCHUNKS(socket));
            } catch (IOException e) {
                UI.printWarning("Server socket closing");
            }
        }
    }

    public void close(){
        try {
            isRestoring = false;
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * Listens for CHUNK messages on the TCP socket
     */
    private void listenForCHUNKS(Socket socket) {
        ObjectInputStream stream;

        try {
            stream = new ObjectInputStream(socket.getInputStream());
        } catch (IOException e) {
            UI.print("Error reading message from TCP socket");
            return;
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
            UI.print("Received CHUNK " + message.getChunkNo() + " via the TCP socket");

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