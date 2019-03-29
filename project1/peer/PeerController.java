package peer;

import message.ChunkInfo;
import message.Message;
import message.RemovedMessage;
import message.StoredMessage;
import javafx.util.Pair;
import protocol.SingleBackupInitiator;
import receiver.Dispatcher;
import receiver.Receiver;
import receiver.SocketController;
import storage.FileSystem;
import utils.Globals;
import utils.Utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.*;

/**
  * Peer controller, where the peer's state is kept
 */
public class PeerController implements Serializable {

    /**
     * The protocol version being executed
     */
    private String peerVersion;

    /**
     * The peer's ID
     */
    private int peerID;

    /**
     * The dispatcher
     */
    private transient Dispatcher dispatcher;

    /**
     * The control channel receiver
     */
    private transient Receiver MCReceiver;

    /**
     * The backup channel receiver
     */
    private transient Receiver MDBReceiver;

    /**
     * The restore channel receiver
     */
    private transient Receiver MDRReceiver;

    /**
     * Peer's file system manager
     */
    private FileSystem fileSystem;

    /**
     * Is the peer running the enhanced backup protocol?
     */
    private boolean backupEnhancement;

    /**
     * Is the peer running the enhanced restore protocol?
     */
    private boolean restoreEnhancement;

    private transient ScheduledExecutorService putChunkThreadPool;

    /**
     *  Socket controller used in enhanced restore protocol
     */
    private transient SocketController TCPController;

    /**
     * Locally stored chunks. Key = fileID, Value = ArrayList of chunk indexes
     */
    private ConcurrentHashMap<String, ArrayList<Integer>> storedChunks;

    /**
     * Useful information of locally stored chunks. Key = <fileID, chunk nr>, Value = Information (observed and desired rep degrees)
     */
    private ConcurrentHashMap<Pair<String, Integer>, ChunkInfo> storedChunksInfo;

    /**
     * Backed up files on the LAN. Key = filename, Value = <fileID, nr. chunks>
     */
    private ConcurrentHashMap<String, Pair<String, Integer>> backedUpFiles;

    /**
     * Useful information of backed up files on the LAN. Key = <fileID, chunk nr>, Value = Information (observed and desired rep degree)
     */
    private ConcurrentHashMap<Pair<String, Integer>, ChunkInfo> backedUpChunksInfo;

    /**
     * Files being restored. Key = fileID, Value = chunks
     */
    private ConcurrentHashMap<String, ConcurrentSkipListSet<Message>> restoringFiles;

    /**
     * Useful information on files being restored. Key = fileID, Value = <filename, chunk amount>
     */
    private ConcurrentHashMap<String, Pair<String, Integer>> restoringFilesInfo;

    /**
     * Useful information on files being restored by other peers. Key = fileID, Value = ArrayList of chunk numbers
     */
    private ConcurrentHashMap<Pair<String, Integer>, Boolean> getChunkRequestsInfo;

    /**
     * Useful information about received STORED messages. Used in backup protocol enhancement. Key = <fileID, chunkInfo>, Value = Information (observed and desired rep degree)
     */
    private ConcurrentHashMap<Pair<String, Integer>, ChunkInfo> storedRepliesInfo;

    /**
     * Instantiates a new Peer controller.
     *
     * @param peerVersion the peers's protocol version
     * @param peerID      the peer's ID
     * @param MCAddress   the mc address
     * @param MCPort      the mc port
     * @param MDBAddress  the mdb address
     * @param MDBPort     the mdb port
     * @param MDRAddress  the mdr address
     * @param MDRPort     the mdr port
     */
    public PeerController(String peerVersion, int peerID, String MCAddress, int MCPort, String MDBAddress, int MDBPort, String MDRAddress, int MDRPort) {
        this.peerVersion = peerVersion;
        this.peerID = peerID;

        storedChunks = new ConcurrentHashMap<>();
        storedChunksInfo = new ConcurrentHashMap<>();

        backedUpFiles = new ConcurrentHashMap<>();
        backedUpChunksInfo = new ConcurrentHashMap<>();

        restoringFiles = new ConcurrentHashMap<>();
        restoringFilesInfo = new ConcurrentHashMap<>();

        getChunkRequestsInfo = new ConcurrentHashMap<>();

        storedRepliesInfo = new ConcurrentHashMap<>();

        //TODO: make proper verification
        if(peerVersion.equals("1.0")) {
            backupEnhancement = false;
            restoreEnhancement = false;
        }
        else {
            System.out.println("Enhancements activated");
            backupEnhancement = true;
            restoreEnhancement = true;
        }

        fileSystem = new FileSystem(peerVersion, peerID, Globals.MAX_PEER_STORAGE, Globals.PEER_FILESYSTEM_DIR + "/" + peerID);

        initTransientMethods(MCAddress, MCPort, MDBAddress, MDBPort, MDRAddress, MDRPort);
    }

    /**
      * Initiates fields not retrievable from non-volatile memory
      *
      * @param MCAddress control channel address
      * @param MCPort control channel port
      * @param MDBAddress backup channel address
      * @param MDBPort backup channel port
      * @param MDRAddress restore channel address
      * @param MDRPort restore channel port
      */
    public void initTransientMethods(String MCAddress, int MCPort, String MDBAddress, int MDBPort, String MDRAddress, int MDRPort) {
        this.dispatcher = new Dispatcher(this, peerID);

        // subscribe to multicast channels
        try {
            this.MCReceiver = new Receiver(MCAddress, MCPort, dispatcher);
            this.MDBReceiver = new Receiver(MDBAddress, MDBPort, dispatcher);
            this.MDRReceiver = new Receiver(MDRAddress, MDRPort, dispatcher);
        } catch (IOException e) {
            e.printStackTrace();
        }

        putChunkThreadPool = Executors.newScheduledThreadPool(50);

        if(restoreEnhancement)
            TCPController = new SocketController(MDRPort);
    }

    /**
     * Handles a PUTCHUNK message
     *
     * @param message the message
     */
    public void handlePutchunkMessage(Message message) {
        System.out.println("Received Putchunk: " + message.getChunkIndex());

        String fileID = message.getFileID();
        int chunkIndex = message.getChunkIndex();

        if(backupEnhancement && !message.getVersion().equals("1.0")) {
            Pair<String, Integer> key = new Pair<>(fileID, chunkIndex);

            if(storedRepliesInfo.containsKey(key)) {
                ChunkInfo chunkInfo = storedRepliesInfo.get(key);

                if(storedRepliesInfo.get(key).isDegreeSatisfied()) {
                    System.out.println("Received enough STORED messages for " + message.getChunkIndex() + " meanwhile, ignoring request");
                    return;
                }
            }
        }

        // check if chunks from this file are already being saved
        storedChunks.putIfAbsent(message.getFileID(), new ArrayList<>());

        //TODO: init this somewhere else
        Pair<String, Integer> chunkInfoKey = new Pair<>(message.getFileID(), message.getChunkIndex());
        storedChunksInfo.putIfAbsent(chunkInfoKey, new ChunkInfo(message.getRepDegree(), 1));

        // check if chunk is already stored
        if(!storedChunks.get(message.getFileID()).contains(message.getChunkIndex())) {
            if (!this.fileSystem.storeChunk(message)) {
                System.out.println("Not enough space to save chunk " + message.getChunkIndex() + " of file " + message.getFileID());
                return;
            }

            //update map of stored chunks
            ArrayList<Integer> fileStoredChunks = storedChunks.get(message.getFileID());
            fileStoredChunks.add(message.getChunkIndex());
            storedChunks.put(message.getFileID(), fileStoredChunks);
        }
        else
            System.out.println("Already stored chunk, sending STORED anyway.");

        Message storedMessage = new StoredMessage(message.getVersion(), peerID, message.getFileID(), message.getChunkIndex());

        MCReceiver.sendWithRandomDelay(0, Globals.MAX_STORED_WAITING_TIME, storedMessage);

        System.out.println("Sent Stored Message: " + storedMessage.getChunkIndex());
    }

    /**
     * Handles a STORED message
     *
     * @param message the message
     */
    public void handleStoredMessage(Message message) {
        System.out.println("Received Stored Message: " + message.getChunkIndex());
        Pair<String, Integer> key = new Pair<>(message.getFileID(), message.getChunkIndex());

        // if this chunk is from a file the peer
        // has requested to backup (aka is the
        // initiator peer), and hasn't received
        // a stored message, update actual rep degree,
        // and add peer
        updateMapInformation(backedUpChunksInfo, key, message);

        // if this peer has this chunk stored,
        // and hasn't received stored message
        // from this peer yet, update actual
        // rep degree, and add peer
        updateMapInformation(storedChunksInfo, key, message);

        if(backupEnhancement && !message.getVersion().equals("1.0")) {
            updateMapInformation(storedRepliesInfo, key, message);
        }
    }

    /**
     * Updates a given ConcurrentHashMaps information at a given key
     * @param map the map to update
     * @param key the key whose value to update
     * @param message the message whose information the map update is based on
     */
    private void updateMapInformation(ConcurrentHashMap<Pair<String, Integer>, ChunkInfo> map, Pair<String, Integer> key, Message message) {
        ChunkInfo chunkInfo;

        if(map.containsKey(key) && !map.get(key).isBackedUpByPeer(message.getPeerID())) {
            chunkInfo = map.get(key);
            chunkInfo.incActualReplicationDegree();
            chunkInfo.addPeer(message.getPeerID());
            map.put(key, chunkInfo);
        }
    }

    /**
     * Handles a GETCHUNK message.
     *
     * @param message the message
     * @param sourceAddress address used for TCP connection in enhanced version of protocol
     */
    public void handleGetChunkMessage(Message message, InetAddress sourceAddress) {
        System.out.println("Received GetChunk Message: " + message.getChunkIndex());

        String fileID = message.getFileID();
        int chunkIndex = message.getChunkIndex();

        // if received a chunk message for this chunk meanwhile, return
        Pair<String, Integer> key = new Pair<>(fileID, chunkIndex);
        if(getChunkRequestsInfo.containsKey(key)) {
            if(getChunkRequestsInfo.get(key)) {
                getChunkRequestsInfo.remove(key);
                System.out.println("Received a CHUNK message for " + chunkIndex + " meanwhile, ignoring request");
                return;
            }
        }

        // if peer doesn't have any chunks from this file, return
        if(!storedChunks.containsKey(fileID))
            return;

        // if peer doesn't have this chunk, return
        if(!storedChunks.get(fileID).contains(chunkIndex))
            return;

        Message chunkMessage = fileSystem.retrieveChunk(fileID, chunkIndex);

        if(restoreEnhancement && !message.getVersion().equals("1.0")) {
            //send chunk via tcp and send header to MDR
            TCPController.sendMessage(chunkMessage, sourceAddress);
            MDRReceiver.sendMessage(chunkMessage, false);
        }
        else
            MDRReceiver.sendMessage(chunkMessage);

    }

    /**
     * Handles a CHUNK message
     *
     * @param message the message
     */
    public void handleChunkMessage(Message message) {
        System.out.println("Received Chunk Message: " + message.getChunkIndex());

        String fileID = message.getFileID();
        int chunkIndex = message.getChunkIndex();

        Pair<String, Integer> key = new Pair<>(fileID, chunkIndex);
        if(getChunkRequestsInfo.containsKey(key)) {
            getChunkRequestsInfo.put(key, true);
            System.out.println("Added Chunk " + chunkIndex + " to requests info.");
        }

        // Not restoring this file
        if(!restoringFiles.containsKey(fileID))
            return;

        // if an enhanced chunk message is sent via multicast
        // channel, it only contains a header, don't restore
        //TODO: this verification isn't right
        if(!message.getVersion().equals("1.0") && !message.hasBody())
            return;

        ConcurrentSkipListSet<Message> fileRestoredChunks = restoringFiles.get(fileID);
        fileRestoredChunks.add(message);

        restoringFiles.put(message.getFileID(), fileRestoredChunks);

        int fileChunkAmount = restoringFilesInfo.get(fileID).getValue();

        if(fileRestoredChunks.size() == fileChunkAmount) {
            saveRestoredFile(fileID);

            // restoring complete
            restoringFiles.remove(fileID);
            restoringFilesInfo.remove(fileID);
        }
    }

    /**
     * Handles a DELETE message
     *
     * @param message the message
     */
    public void handleDeleteMessage(Message message) {
        System.out.println("Received Delete Message");

        // peer doesn't have this chunk
        if(!storedChunks.containsKey(message.getFileID()))
            return;

        ArrayList<Integer> fileChunks = storedChunks.get(message.getFileID());
        while(!fileChunks.isEmpty())
            deleteChunk(message.getFileID(), fileChunks.get(0), false);

        storedChunks.remove(message.getFileID());
        System.out.println("Delete Success: file deleted.");
    }

    /**
     * Handles a REMOVED message
     *
     * @param message the message
     */
    public void handleRemovedMessage(Message message) {
        System.out.println("Received Removed Message: " + message.getChunkIndex());

        Pair<String, Integer> key = new Pair<>(message.getFileID(), message.getChunkIndex());
        if(storedChunksInfo.containsKey(key)) {
            ChunkInfo chunkInfo = storedChunksInfo.get(key);
            chunkInfo.decActualReplicationDegree();

            // if replication degree isn't satisfied anymore,
            // initiate new putchunk protocol for the chunk,
            // but wait between 0-400 ms and check if degree
            // satisfied in the meantime
            if(!chunkInfo.isDegreeSatisfied()) {
                System.out.println("Chunk " + message.getChunkIndex() + " not satisfied anymore.");
                Message chunk = fileSystem.retrieveChunk(message.getFileID(), message.getChunkIndex());

                putChunkThreadPool.schedule( new SingleBackupInitiator(this, chunk, chunkInfo.getDesiredReplicationDegree(), MDBReceiver),
                Utils.getRandomBetween(0, Globals.MAX_REMOVED_WAITING_TIME), TimeUnit.MILLISECONDS);
            }
        }
    }

    /**
     * Tries to reclaim some local space (executes the reclaim protocol)
     *
     * @param targetSpaceKb the target space, in kB
     * @return true
     */
    public boolean reclaimSpace(long targetSpaceKb) {
        long targetSpace = targetSpaceKb * 1000; //kbs to bytes

        while(fileSystem.getUsedStorage() > targetSpace) {
            Pair<String, Integer> toDelete = getMostSatisfiedChunk();

            // no more chunks to delete
            if (toDelete == null) {
                System.out.println("Nothing to delete");
                return fileSystem.getUsedStorage() < targetSpace;
            }

            String fileID = toDelete.getKey();
            int chunkIndex = toDelete.getValue();

            System.out.println("Deleting " + fileID + " - " + chunkIndex);
            deleteChunk(fileID, chunkIndex, true);

            Message removedMessage = new RemovedMessage(peerVersion, peerID, fileID, chunkIndex);
            MCReceiver.sendMessage(removedMessage);
        }

        return true;
    }

    /**
     * Starts listening to stored messages for a chunk that the peer is backing up
     *
     * @param chunk the chunk
     */
    public void backedUpChunkListenForStored(Message chunk) {
        Pair<String, Integer> key = new Pair<>(chunk.getFileID(), chunk.getChunkIndex());
        backedUpChunksInfo.putIfAbsent(key, new ChunkInfo(chunk.getRepDegree(), 0));
    }

    /**
     * Starts listening to stored messages for a chunk the peer is storing
     * @param chunk
     */
    public void listenForStoredReplies(Message chunk) {
        Pair<String, Integer> key = new Pair<>(chunk.getFileID(), chunk.getChunkIndex());
        storedRepliesInfo.putIfAbsent(key, new ChunkInfo(chunk.getRepDegree(), 0));
    }

    /**
     * Starts listening to generic stored messages
     * @param chunk
     */
    public void listenForChunkReplies(Message chunk) {
        Pair<String, Integer> key = new Pair<>(chunk.getFileID(), chunk.getChunkIndex());
        getChunkRequestsInfo.putIfAbsent(key, false);
    }

    /**
     * Gets backed up chunk's observed rep degree.
     *
     * @param chunk the chunk
     * @return the backed up chunk's observed rep degree
     */
    public int getBackedUpChunkRepDegree(Message chunk) {
        Pair<String, Integer> key = new Pair<>(chunk.getFileID(), chunk.getChunkIndex());

        int currentDegree = 0;
        if(backedUpChunksInfo.containsKey(key))
            currentDegree = backedUpChunksInfo.get(key).getActualReplicationDegree();

        return currentDegree;
    }

    /**
     * Add backed up file.
     *
     * @param filePath    the file path
     * @param fileID      the file id
     * @param chunkAmount the chunk amount
     */
    public void addBackedUpFile(String filePath, String fileID, int chunkAmount) {
        backedUpFiles.put(filePath, new Pair<>(fileID, chunkAmount));
    }

    /**
     * Gets backed up file id.
     *
     * @param filePath the file path
     * @return the backed up file id
     */
    public String getBackedUpFileID(String filePath) {
        if(!backedUpFiles.containsKey(filePath))
            return null;

        Pair<String, Integer> fileInfo = backedUpFiles.get(filePath);
        return fileInfo.getKey();
    }

    /**
     * Gets backed up file chunk amount.
     *
     * @param filePath the file path
     * @return the backed up file chunk amount
     */
    public Integer getBackedUpFileChunkAmount(String filePath) {
        if(!backedUpFiles.containsKey(filePath))
            return 0;

        Pair<String, Integer> fileInfo = backedUpFiles.get(filePath);
        return fileInfo.getValue();
    }

    /**
     * Gets most satisfied chunk.
     *
     * @return the most satisfied chunk
     */
    public Pair<String, Integer> getMostSatisfiedChunk() {
        if(storedChunksInfo.isEmpty()) {
            System.out.println("Stored chunks info is empty");
            return null;
        }

        System.out.println("Getting max satisfied chunk");
        ChunkInfo maxChunk = Collections.max(storedChunksInfo.values());

        for(Map.Entry<Pair<String, Integer>, ChunkInfo> chunk : storedChunksInfo.entrySet())
            if(chunk.getValue() == maxChunk)
                return chunk.getKey();

        System.out.println("Didn't find max chunk");
        return null;
    }

    /**
     * Deletes a  chunk.
     *
     * @param fileID           the file id
     * @param chunkIndex       the chunk index
     * @param updateMaxStorage true if max storage value should be updated in the process
     */
    public void deleteChunk(String fileID, int chunkIndex, boolean updateMaxStorage) {
        fileSystem.deleteChunk(fileID, chunkIndex, updateMaxStorage);

        Pair<String, Integer> key = new Pair<>(fileID, chunkIndex);
        if(storedChunksInfo.containsKey(key))
            storedChunksInfo.remove(key);

        if(storedChunks.get(fileID).contains(chunkIndex))
            storedChunks.get(fileID).remove((Integer) chunkIndex);
    }

    /**
     * Add file to restoring files structure.
     *
     * @param fileID      the file id
     * @param filePath    the file path
     * @param chunkAmount the chunk amount
     */
    public void addToRestoringFiles(String fileID, String filePath, int chunkAmount) {
        restoringFiles.putIfAbsent(fileID, new ConcurrentSkipListSet<>());
        restoringFilesInfo.putIfAbsent(fileID, new Pair<>(filePath, chunkAmount));
    }

    /**
     * Saves a restored file locally.
     *
     * @param fileID the file id
     */
    public void saveRestoredFile(String fileID) {
        byte[] fileBody = mergeRestoredFile(fileID);
        String filePath = restoringFilesInfo.get(fileID).getKey();

        fileSystem.saveFile(filePath, fileBody);
    }

    /**
     * Merges a restored file into a single byte[]
     *
     * @param fileID the file id
     * @return the file as a byte[]
     */
    public byte[] mergeRestoredFile(String fileID) {
        // get file's chunks
        ConcurrentSkipListSet<Message> fileChunks = restoringFiles.get(fileID);

        ByteArrayOutputStream stream = new ByteArrayOutputStream();

        try {
            for(Message chunk : fileChunks)
                stream.write(chunk.getBody());
        } catch (IOException e) {
            e.printStackTrace();
        }

        return stream.toByteArray();
    }

    /**
      * Gets the peer's dispatcher
      *
      * @return the dispatcher
      */
    public Dispatcher getDispatcher() {
        return dispatcher;
    }

    /**
      * Represents the peer's state by printing all the information about the structures it keeps and its file system manager
      */
    @Override
    public String toString() {
        StringBuilder output = new StringBuilder();
        output.append("Current Peer state:\n");

        output.append("Files whose backup was initiated by this peer:\n");
        for (Map.Entry<String, Pair<String, Integer>> entry : backedUpFiles.entrySet()) {
            output.append("\tPathname = " + entry.getKey() + ", fileID = " + entry.getValue().getKey()+"\n");

            Pair<String, Integer> firstChunkInfo = new Pair<>(entry.getValue().getKey(), 0);
            output.append("\t\tDesired replication degree: " + backedUpChunksInfo.get(firstChunkInfo).getDesiredReplicationDegree() + "\n");
            output.append("\t\tChunks:\n");

            for(int chunkNr = 0; chunkNr < entry.getValue().getValue(); ++chunkNr) {
                Pair<String, Integer> chunkEntry = new Pair<>(entry.getValue().getKey(), chunkNr);
                output.append("\t\t\tChunk nr. " + chunkNr + " | Perceived replication degree: " + backedUpChunksInfo.get(chunkEntry).getActualReplicationDegree() + "\n");
            }
            output.append("\n");
        }

        output.append("Chunks stored by this peer:\n");

        for (Map.Entry<String, ArrayList<Integer>> entry : storedChunks.entrySet()) {
            output.append("\tBacked up chunks of file with fileID " + entry.getKey() + ":\n");

            for (int chunkNr : entry.getValue()) {
                Pair<String, Integer> chunkEntry = new Pair<>(entry.getKey(), chunkNr);
                //TODO: Add chunk size here
                output.append("\t\tChunk nr. " + chunkNr + " | Perceived replication degree: " + storedChunksInfo.get(chunkEntry).getActualReplicationDegree() + "\n");
            }
            output.append("\n");
        }

        output.append("Max storage capacity (in kB): " + fileSystem.getMaxStorage()/1000 + "\n");
        output.append("Current storage capacity used (in kB): " + fileSystem.getUsedStorage()/1000 + "\n");
        return output.toString();
    }

}
