package receiver;

import message.PackedMessage;
import peer.PeerController;
import utils.Globals;
import utils.Utils;

import java.net.InetAddress;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Dispatcher {

    private final int MAX_DISPATCHER_THREADS = 50;
    private int peerID;
    private PeerController controller;

    private ScheduledExecutorService threadPool = Executors.newScheduledThreadPool(MAX_DISPATCHER_THREADS);

    /**
     * Instantiates a new Dispatcher.
     *
     * @param controller the controller
     * @param peerID     the peer id
     */
    public Dispatcher(PeerController controller, int peerID) {
        this.peerID = peerID;
        this.controller = controller;
    }

    /**
      * Handles a packedMessage.
      *
      * @param packedMessage packedMessage to be handled
      * @param address address used in getchunk packedMessage handling
      */
    public void handleMessage(PackedMessage packedMessage, InetAddress address) {
        //Ignore messages from self
        if(packedMessage.getPeerID().equals(this.peerID))
            return;

        dispatchMessage(packedMessage, address);
    }

    /**
     * Handles a message.
     *
     * @param buf  the buf representing the message
     * @param size the message's size
     * @param address address used in getchunk message handling
     */
    public void handleMessage(byte[] buf, int size, InetAddress address) {
        PackedMessage packedMessage = new PackedMessage(buf, size);

        if(packedMessage.getPeerID().equals(this.peerID))
            return;
        dispatchMessage(packedMessage, address);
    }

    /**
      * Dispatches a packedMessage handler to the thread pool
      *
      * @param packedMessage packedMessage to be dispatched
      * @param address address used in getchunk packedMessage dispatch
      */
    public void dispatchMessage(PackedMessage packedMessage, InetAddress address) {
        int randomWait;

        switch(packedMessage.getType()) {
            case PUTCHUNK:
                if(!packedMessage.getVersion().equals("1.0")) {
                    controller.listenForStoredReplies(packedMessage);
                    randomWait = Utils.getRandomBetween(0, Globals.MAX_BACKUP_ENH_WAIT_TIME);
                }
                else
                    randomWait = 0;

                threadPool.schedule(() -> {
                    controller.handlePutchunkMessage(packedMessage);
                }, randomWait, TimeUnit.MILLISECONDS);
                break;
            case STORED:
                threadPool.submit(() -> {
                    controller.handleStoredMessage(packedMessage);
                });
                break;
            case GETCHUNK:
                controller.listenForChunkReplies(packedMessage);
                randomWait = Utils.getRandomBetween(0, Globals.MAX_CHUNK_WAITING_TIME);
                threadPool.schedule(() -> {
                    controller.handleGetChunkMessage(packedMessage, address);
                }, randomWait, TimeUnit.MILLISECONDS);
                break;
            case CHUNK:
                threadPool.submit(() -> {
                    controller.handleChunkMessage(packedMessage);
                });
                break;
            case DELETE:
                threadPool.submit(() -> {
                    controller.handleDeleteMessage(packedMessage);
                });
                break;
            case REMOVED:
                threadPool.submit(() -> {
                    controller.handleRemovedMessage(packedMessage);
                });
                break;
            default:
                System.out.println("No valid type");
        }
    }
}
