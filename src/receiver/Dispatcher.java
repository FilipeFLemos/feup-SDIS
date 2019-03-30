package receiver;

import javafx.util.Pair;
import message.ChunkInfo;
import message.Message;
import peer.PeerController;
import protocol.Backup;
import utils.Globals;
import utils.Utils;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.concurrent.*;

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
      * Handles a message.
      *
      * @param message message to be handled
      * @param address address used in getchunk message handling
      */
    public void handleMessage(Message message, InetAddress address) {
        //Ignore messages from self
        if(message.getSenderId().equals(this.peerID))
            return;

        dispatchMessage(message, address);
    }

    /**
     * Handles a message.
     *
     * @param buf  the buf representing the message
     * @param size the message's size
     * @param address address used in getchunk message handling
     */
    public void handleMessage(byte[] buf, int size, InetAddress address) {
        Message message = new Message(buf, size);

        if(message.getSenderId().equals(this.peerID))
            return;
        dispatchMessage(message, address);
    }

    /**
      * Dispatches a message handler to the thread pool
      *
      * @param message message to be dispatched
      * @param address address used in getchunk message dispatch
      */
    public void dispatchMessage(Message message, InetAddress address) {
        int randomWait;

        switch(message.getMessageType()) {
            case PUTCHUNK:
                if(!message.getVersion().equals("1.0")) {
                    controller.listenForStoredReplies(message);
                    randomWait = Utils.getRandomBetween(0, Globals.MAX_BACKUP_ENH_WAIT_TIME);
                }
                else
                    randomWait = 0;

                threadPool.schedule(() -> handlePUTCHUNK(message), randomWait, TimeUnit.MILLISECONDS);
                break;
            case STORED:
                threadPool.submit(() -> handleSTORED(message));
                break;
            case GETCHUNK:
                controller.listenForChunkReplies(message);
                randomWait = Utils.getRandomBetween(0, Globals.MAX_CHUNK_WAITING_TIME);
                threadPool.schedule(() -> handleGETCHUNK(message, address), randomWait, TimeUnit.MILLISECONDS);
                break;
            case CHUNK:
                threadPool.submit(() -> handleCHUNK(message));
                break;
            case DELETE:
                threadPool.submit(() -> handleDELETE(message));
                break;
            case REMOVED:
                threadPool.submit(() -> handleREMOVED(message));
                break;
            default:
                System.out.println("No valid type");
        }
    }

    /**
     * Handles a PUTCHUNK message
     *
     * @param message the message
     */
    public void handlePUTCHUNK(Message message) {
        System.out.println("Received Putchunk: " + message.getChunkNo());

        String fileID = message.getFileId();
        int chunkIndex = message.getChunkNo();

        if(controller.isBackupEnhancement() && !message.getVersion().equals("1.0")) {
            Pair<String, Integer> key = new Pair<>(fileID, chunkIndex);
            ConcurrentHashMap<Pair<String, Integer>, ChunkInfo> storedRepliesInfo = controller.getStoredRepliesInfo();

            if(storedRepliesInfo.containsKey(key)) {

                if(storedRepliesInfo.get(key).isDegreeSatisfied()) {
                    System.out.println("Received enough STORED messages for " + message.getChunkNo() + " meanwhile, ignoring request");
                    return;
                }
            }
        }

        controller.startStoringChunks(message);
        ConcurrentHashMap<String, ArrayList<Integer>> storedChunks = controller.getStoredChunks();

        // check if chunk is already stored
        if(!storedChunks.get(message.getFileId()).contains(message.getChunkNo())) {
            if (!controller.getFileSystem().storeChunk(message)) {
                System.out.println("Not enough space to save chunk " + message.getChunkNo() + " of file " + message.getFileId());
                return;
            }

            //update map of stored chunks
            ArrayList<Integer> fileStoredChunks = storedChunks.get(message.getFileId());
            fileStoredChunks.add(message.getChunkNo());
            storedChunks.put(message.getFileId(), fileStoredChunks);
        }
        else
            System.out.println("Already stored chunk, sending STORED anyway.");

        Message storedMessage = new Message(message.getVersion(), peerID, message.getFileId(), null, Message.MessageType.STORED, message.getChunkNo());

        controller.getMCReceiver().sendWithRandomDelay(0, Globals.MAX_STORED_WAITING_TIME, storedMessage);

        System.out.println("Sent Stored Message: " + storedMessage.getChunkNo());
    }

    /**
     * Handles a CHUNK message
     *
     * @param message the message
     */
    public void handleCHUNK(Message message) {
        System.out.println("Received Chunk Message: " + message.getChunkNo());

        String fileId = message.getFileId();
        int chunkNo = message.getChunkNo();
        Pair<String, Integer> key = new Pair<>(fileId, chunkNo);

        ConcurrentHashMap<Pair<String, Integer>, Boolean> getChunkRequestsInfo = controller.getGetChunkRequestsInfo();
        if(getChunkRequestsInfo.containsKey(key)) {
            controller.addGetChunkRequestInfo(key);
            System.out.println("Added Chunk " + chunkNo + " to requests info.");
        }

        ConcurrentHashMap<String, ConcurrentSkipListSet<Message>> restoringFiles = controller.getRestoringFiles();
        if(!restoringFiles.containsKey(fileId))
            return;

        // if an enhanced chunk message is sent via multicast
        // channel, it only contains a header, don't restore
        //TODO: this verification isn't right
        if(!message.getVersion().equals("1.0") && !message.hasBody())
            return;

        ConcurrentSkipListSet<Message> fileRestoredChunks = restoringFiles.get(fileId);
        fileRestoredChunks.add(message);

        controller.addRestoredFile(message, fileRestoredChunks);

        int fileChunkAmount = controller.getRestoringFilesInfo().get(fileId).getValue();

        if(fileRestoredChunks.size() == fileChunkAmount) {
            controller.saveRestoredFile(fileId);
            controller.stopRestoringFile(fileId);
        }
    }

    /**
     * Handles a GETCHUNK message. If a CHUNK message for this chunk is received while handling GETCHUNK, the operation
     * is aborted. If the peer does not have any or a particular stored CHUNK for this file, the operation is aborted.
     *
     * @param message the message
     * @param sourceAddress address used for TCP connection in enhanced version of protocol
     */
    public void handleGETCHUNK(Message message, InetAddress sourceAddress) {
        System.out.println("Received GetChunk Message: " + message.getChunkNo());

        String fileId = message.getFileId();
        int chunkNo = message.getChunkNo();
        Pair<String, Integer> key = new Pair<>(fileId, chunkNo);

        ConcurrentHashMap<Pair<String, Integer>, Boolean> getChunkRequestsInfo = controller.getGetChunkRequestsInfo();
        if(getChunkRequestsInfo.containsKey(key)) {
            if(getChunkRequestsInfo.get(key)) {
                getChunkRequestsInfo.remove(key);
                System.out.println("Received a CHUNK message for " + chunkNo + " meanwhile, ignoring request");
                return;
            }
        }

        ConcurrentHashMap<String, ArrayList<Integer>> storedChunks = controller.getStoredChunks();
        if(!storedChunks.containsKey(fileId) || !storedChunks.get(fileId).contains(chunkNo)) {
            return;
        }

        Message chunk = controller.getFileSystem().retrieveChunk(fileId, chunkNo);
        controller.sendMessage(chunk,sourceAddress);
    }

    /**
     * Handles a STORED message
     *
     * @param message the message
     */
    public void handleSTORED(Message message) {
        System.out.println("Received Stored Message: " + message.getChunkNo());
        Pair<String, Integer> key = new Pair<>(message.getFileId(), message.getChunkNo());
        controller.updateChunksInfo(key,message);
    }

    /**
     * Handles a DELETE message. If the peer does not have the chunk, the message is ignored.
     *
     * @param message the message
     */
    public void handleDELETE(Message message) {
        System.out.println("Received Delete Message");
        ConcurrentHashMap<String, ArrayList<Integer>> storedChunks = controller.getStoredChunks();

        if(!storedChunks.containsKey(message.getFileId()))
            return;

        ArrayList<Integer> fileStoredChunks = storedChunks.get(message.getFileId());
        while(!fileStoredChunks.isEmpty())
            controller.deleteChunk(message.getFileId(), fileStoredChunks.get(0), false);

        controller.removeStoredChunksFile(message.getFileId());
        System.out.println("Delete Success: file deleted.");
    }

    /**
     * Handles a REMOVED message. If this action leads to an unsatisfied replication degree, a new backup protocol for
     * the chunk must be initiated. However, it must wait a random interval of [0-400]ms to check if the degree was
     * satisfied before taking action.
     *
     * @param message the message
     */
    public void handleREMOVED(Message message) {
        System.out.println("Received Removed Message: " + message.getChunkNo());
        ConcurrentHashMap<Pair<String, Integer>, ChunkInfo> storedChunksInfo = controller.getStoredChunksInfo();

        Pair<String, Integer> key = new Pair<>(message.getFileId(), message.getChunkNo());
        if(storedChunksInfo.containsKey(key)) {
            ChunkInfo chunkInfo = storedChunksInfo.get(key);
            chunkInfo.decreaseCurrentReplicationDeg();

            if(!chunkInfo.isDegreeSatisfied()) {
                System.out.println("Chunk " + message.getChunkNo() + " not satisfied anymore.");
                Message chunk = controller.getFileSystem().retrieveChunk(message.getFileId(), message.getChunkNo());

                threadPool.schedule( new Backup(controller, chunk, chunkInfo.getDesiredReplicationDeg(), controller.getMDBReceiver()),
                        Utils.getRandomBetween(0, Globals.MAX_REMOVED_WAITING_TIME), TimeUnit.MILLISECONDS);
            }
        }
    }
}
