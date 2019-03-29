package receiver;

import message.Message;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SocketReceiver implements Runnable {

    private static final int MAX_TCP_SOCKETS = 50;

    private ServerSocket serverSocket;

    private ExecutorService threadPool = Executors.newFixedThreadPool(MAX_TCP_SOCKETS);

    private Dispatcher dispatcher;

    /**
      * Instantiates a socket receiver
      *
      * @param port receiver port
      * @param dispatcher receiver dispatcher
      */
    public SocketReceiver(int port, Dispatcher dispatcher) {
        this.dispatcher = dispatcher;
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
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (ClassNotFoundException e) {
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
            System.out.println("Error creating Object Input Stream");
            e.printStackTrace();
        }

        Message message;
        while((message = (Message) stream.readObject()) != null) {
            dispatcher.handleMessage(message, null);
            System.out.println("Received CHUNK message " + message.getChunkIndex() + " via TCP");

            try {
                stream = new ObjectInputStream(socket.getInputStream());
            }
            catch (IOException e) {
                System.out.println("Closing TCP socket...");
                socket.close();
                break;
            }
        }
    }
}
