package channels;

import message.ChunkInfo;
import message.Message;
import peer.FileChunk;
import peer.Peer;
import peer.PeerController;
import protocols.BackupChunk;
import utils.Globals;
import utils.Utils;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.concurrent.*;

public class MessageHandler {

    private final int MAX_DISPATCHER_THREADS = 50;
    private PeerController controller;
    private Peer peer;

    private ScheduledExecutorService threadPool = Executors.newScheduledThreadPool(MAX_DISPATCHER_THREADS);

    /**
     * Instantiates a new MessageHandler.
     *
     */
    public MessageHandler(Peer peer) {
        this.peer = peer;
        this.controller = peer.getController();
    }

    /**
      * Handles a message and sends it to the thread pool.
      * Ignores messages sent my itself.
      *
      * @param message message to be handled
      * @param address address used in GETCHUNK message (TCP address). Unless the peer is enhanced, this field is always
      * null.
      */
    void handleMessage(Message message, InetAddress address) {
        if(message.getSenderId().equals(peer.getServerId()))
            return;

        int randomWait;

        switch(message.getMessageType()) {
            case PUTCHUNK:
                if(!message.getVersion().equals("1.0")) {
                    controller.listenForSTORED_ENH(message);
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
    private void handlePUTCHUNK(Message message) {
        System.out.println("Received Putchunk: " + message.getChunkNo());

        String fileId = message.getFileId();
        int chunkNo = message.getChunkNo();

        if(controller.isBackupEnhancement() && !message.getVersion().equals("1.0")) {
            FileChunk key = new FileChunk(fileId, chunkNo);
            ConcurrentHashMap<FileChunk, ChunkInfo> storedRepliesInfo = controller.getStoredChunksInfo_ENH();

            if(storedRepliesInfo.containsKey(key)) {
                if(storedRepliesInfo.get(key).achievedDesiredRepDeg()) {
                    System.out.println("Received enough STORED messages for " + message.getChunkNo() + " meanwhile, ignoring request");
                    return;
                }
            }
        }

        controller.startStoringChunks(message);
        ConcurrentHashMap<String, ArrayList<Integer>> storedChunksByFileId = controller.getStoredChunksByFileId();

        if(storedChunksByFileId.get(fileId).contains(message.getChunkNo())) {
            System.out.println("Already stored chunk, sending STORED anyway.");
        }
        else {
            if (!controller.getStorageManager().saveChunk(message)) {
                System.out.println("Not enough space to save chunk " + message.getChunkNo() + " of file " + message.getFileId());
                return;
            }
            controller.addStoredChunk(message);
        }

        Message storedMessage = new Message(message.getVersion(), peer.getServerId(), message.getFileId(), null, Message.MessageType.STORED, message.getChunkNo());
        peer.getMCChannel().sendWithRandomDelay(0, Globals.MAX_STORED_WAITING_TIME, storedMessage);

        System.out.println("Sent Stored Message: " + storedMessage.getChunkNo());
    }

    /**
     * Handles a STORED message
     *
     * @param message the message
     */
    private void handleSTORED(Message message) {
        System.out.println("Received Stored Message: " + message.getChunkNo());

        FileChunk key = new FileChunk(message.getFileId(), message.getChunkNo());
        controller.updateChunksInfo(key,message);
    }

    /**
     * Handles a GETCHUNK message. If a CHUNK message for this chunk is received while handling GETCHUNK, the operation
     * is aborted. If the peer does not have any CHUNK for this file or this CHUNK No, the operation is aborted.
     *
     * @param message the message
     * @param sourceAddress address used for TCP connection in enhanced version of protocols
     */
    private void handleGETCHUNK(Message message, InetAddress sourceAddress) {
        System.out.println("Received GetChunk Message: " + message.getChunkNo());

        String fileId = message.getFileId();
        int chunkNo = message.getChunkNo();
        FileChunk fileChunk = new FileChunk(fileId, chunkNo);

        ConcurrentHashMap<FileChunk, Boolean> isBeingRestoredChunkMap = controller.getIsBeingRestoredChunkMap();
        if(isBeingRestoredChunkMap.containsKey(fileChunk)) {
            if(isBeingRestoredChunkMap.get(fileChunk)) {
                controller.removeChunk(fileChunk);
                System.out.println("Received a CHUNK message for " + chunkNo + " meanwhile, ignoring request");
                return;
            }
        }

        ConcurrentHashMap<String, ArrayList<Integer>> storedChunksByFileId = controller.getStoredChunksByFileId();
        if(!storedChunksByFileId.containsKey(fileId) || !storedChunksByFileId.get(fileId).contains(chunkNo)) {
            return;
        }

        Message chunk = controller.getStorageManager().retrieveChunk(fileId, chunkNo);
        peer.sendMessage(chunk,sourceAddress);
    }

    /**
     * Handles a CHUNK message
     *
     * @param message the message
     */
    private void handleCHUNK(Message message) {
        System.out.println("Received Chunk Message: " + message.getChunkNo());

        String fileId = message.getFileId();
        FileChunk fileChunk = new FileChunk(fileId, message.getChunkNo());

        ConcurrentHashMap<FileChunk, Boolean> isBeingRestoredChunkMap = controller.getIsBeingRestoredChunkMap();
        if(isBeingRestoredChunkMap.containsKey(fileChunk)) {
            controller.setIsBeingRestored(fileChunk);
            System.out.println("Added Chunk " + message.getChunkNo() + " to requests info.");
        }

        ConcurrentHashMap<String, ConcurrentSkipListSet<Message>> chunksByRestoredFile = controller.getChunksByRestoredFile();
        if(!chunksByRestoredFile.containsKey(fileId))
            return;

        // if an enhanced chunk message is sent via multicast
        // channel, it only contains a header, don't restore
        //TODO: this verification isn't right
        if(!message.getVersion().equals("1.0") && !message.hasBody())
            return;

        controller.addRestoredFileChunks(message);

        if(controller.hasRestoredAllChunks(fileId)) {
            controller.saveRestoredFile(fileId);
            controller.stopRestoringFile(fileId);
        }
    }

    /**
     * Handles a DELETE message. If the peer does not have the chunk, the message is ignored.
     *
     * @param message the message
     */
    private void handleDELETE(Message message) {
        System.out.println("Received Delete Message");

        String fileId = message.getFileId();

        ConcurrentHashMap<String, ArrayList<Integer>> storedChunksByFileId = controller.getStoredChunksByFileId();
        if(!storedChunksByFileId.containsKey(fileId))
            return;

        ArrayList<Integer> storedChunks = storedChunksByFileId.get(fileId);
        while(!storedChunks.isEmpty()) {
            controller.deleteChunk(fileId, storedChunks.get(0), false);
        }

        controller.removeStoredChunksFile(fileId);
        System.out.println("Delete Success: file deleted.");
    }

    /**
     * Handles a REMOVED message. If this action leads to an unsatisfied replication degree, a new backup protocols for
     * the chunk must be initiated. However, it must wait a random interval of [0-400]ms to check if the degree was
     * satisfied before taking action.
     *
     * @param message the message
     */
    private void handleREMOVED(Message message) {
        System.out.println("Received Removed Message: " + message.getChunkNo());

        FileChunk fileChunk = new FileChunk(message.getFileId(), message.getChunkNo());
        ConcurrentHashMap<FileChunk, ChunkInfo> storedChunksInfo = controller.getStoredChunksInfo();
        if(storedChunksInfo.containsKey(fileChunk)) {
            ChunkInfo chunkInfo = storedChunksInfo.get(fileChunk);
            chunkInfo.decreaseCurrentRepDeg();

            if(!chunkInfo.achievedDesiredRepDeg()) {
                System.out.println("Chunk " + message.getChunkNo() + " not satisfied anymore.");
                Message chunk = controller.getStorageManager().retrieveChunk(message.getFileId(), message.getChunkNo());

                threadPool.schedule( new BackupChunk(controller, chunk, chunkInfo.getDesiredReplicationDeg(), peer.getMDBChannel()),
                        Utils.getRandomBetween(0, Globals.MAX_REMOVED_WAITING_TIME), TimeUnit.MILLISECONDS);
            }
        }
    }
}
