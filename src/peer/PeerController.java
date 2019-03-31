package peer;

import message.*;
import receiver.Dispatcher;
import receiver.Receiver;
import receiver.TCPSocketController;
import storage.FileSystem;
import utils.Globals;

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


    private static final long serialVersionUID = 1L;
    private transient Peer peer;
    private String version;
    private int peerId;

    /**
     * The dispatcher
     */
    private transient Dispatcher dispatcher;

    
    private FileSystem fileSystem;

    private boolean backupEnhancement;

    private boolean restoreEnhancement;

    /**
     * Locally stored chunks. Key = fileID, Value = ArrayList of chunk indexes
     */
    private ConcurrentHashMap<String, ArrayList<Integer>> storedChunks;

    /**
     * Useful information of locally stored chunks. Key = <fileID, chunk nr>, Value = Information (observed and desired rep degrees)
     */
    //private ConcurrentHashMap<Pair<String, Integer>, ChunkInfo> storedChunksInfo;
    private ConcurrentHashMap<FileChunk, ChunkInfo> storedChunksInfo;

    private ConcurrentHashMap<String, FileInfo> backedupFilesByPaths;
    private ConcurrentHashMap<FileChunk, ChunkInfo> backedUpChunksInfo;

    /**
     * Files being restored. Key = fileID, Value = chunks
     */
    private ConcurrentHashMap<String, ConcurrentSkipListSet<Message>> restoringFiles;

    /**
     * Useful information on files being restored. Key = fileID, Value = <filename, chunk amount>
     */
    //private ConcurrentHashMap<String, Pair<String, Integer>> restoringFilesInfo;
    private ConcurrentHashMap<String, FileChunk> restoringFilesInfo;

    /**
     * Useful information on files being restored by other peers. Key = fileID, Value = ArrayList of chunk numbers
     */
    //private ConcurrentHashMap<Pair<String, Integer>, Boolean> getChunkRequestsInfo;
    private ConcurrentHashMap<FileChunk, Boolean> getChunkRequestsInfo;

    /**
     * Useful information about received STORED messages. Used in backup protocol enhancement. Key = <fileID, chunkInfo>, Value = Information (observed and desired rep degree)
     */
    //private ConcurrentHashMap<Pair<String, Integer>, ChunkInfo> storedRepliesInfo;
    private ConcurrentHashMap<FileChunk, ChunkInfo> storedRepliesInfo;

    /**
     * Instantiates a new Peer controller.
     */
    public PeerController(Peer peer) {
        this.version = peer.getProtocolVersion();
        this.peerId = peer.getPeerId();

        storedChunks = new ConcurrentHashMap<>();
        storedChunksInfo = new ConcurrentHashMap<>();

        backedupFilesByPaths = new ConcurrentHashMap<>();
        backedUpChunksInfo = new ConcurrentHashMap<>();

        restoringFiles = new ConcurrentHashMap<>();
        restoringFilesInfo = new ConcurrentHashMap<>();

        getChunkRequestsInfo = new ConcurrentHashMap<>();

        storedRepliesInfo = new ConcurrentHashMap<>();

        if(version.equals("1.0")) {
            backupEnhancement = false;
            restoreEnhancement = false;
        }
        else {
            System.out.println("Enhancements activated");
            backupEnhancement = true;
            restoreEnhancement = true;
        }

        fileSystem = new FileSystem(version, peerId, Globals.MAX_PEER_STORAGE, Globals.PEER_FILESYSTEM_DIR + "/" + peerId);

        setChannels(peer);
    }

    public void setChannels(Peer peer){
        this.peer = peer;
        dispatcher = peer.getDispatcher();
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
    /*public void initTransientMethods(String MCAddress, int MCPort, String MDBAddress, int MDBPort, String MDRAddress, int MDRPort) {
        this.dispatcher = new Dispatcher(this, peerId);

        // subscribe to multicast channels
        try {
            this.MCReceiver = new Receiver(MCAddress, MCPort, dispatcher);
            this.MDBReceiver = new Receiver(MDBAddress, MDBPort, dispatcher);
            this.MDRReceiver = new Receiver(MDRAddress, MDRPort, dispatcher);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if(restoreEnhancement)
            TCPController = new TCPSocketController(MDRPort);
    }*/

    /**
     * Starts listening to stored messages for a chunk that the peer is backing up
     *
     * @param chunk the chunk
     */
    public void listenforSTORED(Message chunk) {
        FileChunk key = new FileChunk(chunk.getFileId(),chunk.getChunkNo());
        backedUpChunksInfo.putIfAbsent(key, new ChunkInfo(chunk.getReplicationDeg(), 0));
    }

    /**
     * Starts listening to stored messages for a chunk the peer is storing
     * @param chunk
     */
    public void listenForStoredReplies(Message chunk) {
        //Pair<String, Integer> key = new Pair<>(chunk.getFileId(), chunk.getChunkNo());
        FileChunk key = new FileChunk(chunk.getFileId(),chunk.getChunkNo());
        storedRepliesInfo.putIfAbsent(key, new ChunkInfo(chunk.getReplicationDeg(), 0));
    }

    /**
     * Starts listening to generic stored messages
     * @param chunk
     */
    public void listenForChunkReplies(Message chunk) {
        //Pair<String, Integer> key = new Pair<>(chunk.getFileId(), chunk.getChunkNo());
        FileChunk key = new FileChunk(chunk.getFileId(),chunk.getChunkNo());
        getChunkRequestsInfo.putIfAbsent(key, false);
    }

    /**
     * Gets backed up chunk's observed rep degree.
     *
     * @param chunk the chunk
     * @return the backed up chunk's observed rep degree
     */
    public int getBackedUpChunkRepDegree(Message chunk) {
        //Pair<String, Integer> key = new Pair<>(chunk.getFileId(), chunk.getChunkNo());
        FileChunk key = new FileChunk(chunk.getFileId(),chunk.getChunkNo());

        int currentDegree = 0;
        if(backedUpChunksInfo.containsKey(key))
            currentDegree = backedUpChunksInfo.get(key).getCurrentReplicationDeg();

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
        //backedUpFiles.put(filePath, new Pair<>(fileID, chunkAmount));
        backedupFilesByPaths.put(filePath, new FileInfo(fileID, chunkAmount));
    }

    /**
     * Gets backed up file id.
     *
     * @param filePath the file path
     * @return the backed up file id
     */
    public String getBackedUpFileID(String filePath) {
        if(!backedupFilesByPaths.containsKey(filePath))
            return null;

        FileInfo fileInfo = backedupFilesByPaths.get(filePath);
        //Pair<String, Integer> fileInfo = backedUpFiles.get(filePath);
        //return fileInfo.getKey();
        return fileInfo.getFileId();
    }

    /**
     * Gets backed up file chunk amount.
     *
     * @param filePath the file path
     * @return the backed up file chunk amount
     */
    public Integer getBackedUpFileChunkAmount(String filePath) {
        if(!backedupFilesByPaths.containsKey(filePath))
            return 0;

        FileInfo fileInfo = backedupFilesByPaths.get(filePath);
        //Pair<String, Integer> fileInfo = backedUpFiles.get(filePath);
        //return fileInfo.getValue();
        return fileInfo.getNumberOfChunks();
    }

    /**
     * Gets most satisfied chunk.
     *
     * @return the most satisfied chunk
     */
    //public Pair<String, Integer> getMostSatisfiedChunk() {
    public FileChunk getMostSatisfiedChunk() {
        if(storedChunksInfo.isEmpty()) {
            System.out.println("Stored chunks info is empty");
            return null;
        }

        System.out.println("Getting max satisfied chunk");
        ChunkInfo maxChunk = Collections.max(storedChunksInfo.values());

        for(Map.Entry<FileChunk, ChunkInfo> chunk : storedChunksInfo.entrySet())
            if(chunk.getValue() == maxChunk)
                return chunk.getKey();

//        for(Map.Entry<Pair<String, Integer>, ChunkInfo> chunk : storedChunksInfo.entrySet())
//            if(chunk.getValue() == maxChunk)
//                return chunk.getKey();

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

        //Pair<String, Integer> key = new Pair<>(fileID, chunkIndex);
        FileChunk key = new FileChunk(fileID, chunkIndex);
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
        //restoringFilesInfo.putIfAbsent(fileID, new Pair<>(filePath, chunkAmount));
        restoringFilesInfo.putIfAbsent(fileID, new FileChunk(filePath, chunkAmount));
    }

    /**
     * Saves a restored file locally.
     *
     * @param fileID the file id
     */
    public void saveRestoredFile(String fileID) {
        byte[] fileBody = mergeRestoredFile(fileID);
        //String filePath = restoringFilesInfo.get(fileID).getKey();
        String filePath = restoringFilesInfo.get(fileID).getFileId();

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
//        for (Map.Entry<String, Pair<String, Integer>> entry : backedUpFiles.entrySet()) {
//            output.append("\tPathname = " + entry.getKey() + ", fileID = " + entry.getValue().getKey()+"\n");
//
//            Pair<String, Integer> firstChunkInfo = new Pair<>(entry.getValue().getKey(), 0);
//            output.append("\t\tDesired replication degree: " + backedUpChunksInfo.get(firstChunkInfo).getDesiredReplicationDeg() + "\n");
//            output.append("\t\tChunks:\n");
//
//            for(int chunkNr = 0; chunkNr < entry.getValue().getValue(); ++chunkNr) {
//                Pair<String, Integer> chunkEntry = new Pair<>(entry.getValue().getKey(), chunkNr);
//                output.append("\t\t\tChunk nr. " + chunkNr + " | Perceived replication degree: " + backedUpChunksInfo.get(chunkEntry).getCurrentReplicationDeg() + "\n");
//            }
//            output.append("\n");
//        }
        for (Map.Entry<String, FileInfo> entry : backedupFilesByPaths.entrySet()) {
            output.append("\tPathname = " + entry.getKey() + ", fileID = " + entry.getValue().getFileId()+"\n");

            FileChunk firstChunkInfo = new FileChunk(entry.getValue().getFileId(), 0);
            output.append("\t\tDesired replication degree: " + backedUpChunksInfo.get(firstChunkInfo).getDesiredReplicationDeg() + "\n");
            output.append("\t\tChunks:\n");

            for(int chunkNr = 0; chunkNr < entry.getValue().getNumberOfChunks(); ++chunkNr) {
                FileChunk chunkEntry = new FileChunk(entry.getValue().getFileId(), chunkNr);
                output.append("\t\t\tChunk nr. " + chunkNr + " | Perceived replication degree: " + backedUpChunksInfo.get(chunkEntry).getCurrentReplicationDeg() + "\n");
            }
            output.append("\n");
        }

        output.append("Chunks stored by this peer:\n");

        for (Map.Entry<String, ArrayList<Integer>> entry : storedChunks.entrySet()) {
            output.append("\tBacked up chunks of file with fileID " + entry.getKey() + ":\n");

            for (int chunkNr : entry.getValue()) {
                //Pair<String, Integer> chunkEntry = new Pair<>(entry.getKey(), chunkNr);
                FileChunk chunkEntry = new FileChunk(entry.getKey(), chunkNr);
                //TODO: Add chunk size here
                output.append("\t\tChunk nr. " + chunkNr + " | Perceived replication degree: " + storedChunksInfo.get(chunkEntry).getCurrentReplicationDeg() + "\n");
            }
            output.append("\n");
        }

        output.append("Max storage capacity (in kB): " + fileSystem.getMaxStorage()/1000 + "\n");
        output.append("Current storage capacity used (in kB): " + fileSystem.getUsedStorage()/1000 + "\n");
        return output.toString();
    }

    public FileSystem getFileSystem() {
        return fileSystem;
    }

    public Receiver getMCReceiver() {
        return peer.getMCReceiver();
    }

    public Receiver getMDBReceiver() {
        return peer.getMDBReceiver();
    }

    public Receiver getMDRReceiver() {
        return peer.getMDRReceiver();
    }

    public String getVersion() {
        return version;
    }

    public int getPeerId() {
        return peerId;
    }

    public boolean isBackupEnhancement() {
        return backupEnhancement;
    }

    public ConcurrentHashMap<String, ArrayList<Integer>> getStoredChunks() {
        return storedChunks;
    }

//    public ConcurrentHashMap<Pair<String, Integer>, ChunkInfo> getStoredChunksInfo() {
//        return storedChunksInfo;
//    }

    public ConcurrentHashMap<FileChunk, ChunkInfo> getStoredChunksInfo() {
        return storedChunksInfo;
    }

    public ConcurrentHashMap<FileChunk, Boolean> getGetChunkRequestsInfo() {
        return getChunkRequestsInfo;
    }

//    public ConcurrentHashMap<Pair<String, Integer>, Boolean> getGetChunkRequestsInfo() {
//        return getChunkRequestsInfo;
//    }

//    public ConcurrentHashMap<Pair<String, Integer>, ChunkInfo> getStoredRepliesInfo() {
//        return storedRepliesInfo;
//    }
    public ConcurrentHashMap<FileChunk, ChunkInfo> getStoredRepliesInfo() {
        return storedRepliesInfo;
    }

    public ConcurrentHashMap<String, ConcurrentSkipListSet<Message>> getRestoringFiles() {
        return restoringFiles;
    }

//    public ConcurrentHashMap<String, Pair<String, Integer>> getRestoringFilesInfo() {
//        return restoringFilesInfo;
//    }
    public ConcurrentHashMap<String, FileChunk> getRestoringFilesInfo() {
        return restoringFilesInfo;
    }

    public void removeStoredChunksFile(String fileId) {
        storedChunks.remove(fileId);
    }

    public void sendMessage(Message message, InetAddress sourceAddress){
        if(restoreEnhancement && !message.getVersion().equals("1.0")) {
            //send chunk via tcp and send header to MDR
            peer.getTCPController().sendMessage(message, sourceAddress);
            getMDRReceiver().sendMessage(message, false);
        }
        else
            getMDRReceiver().sendMessage(message);
    }


//    public void updateChunksInfo(Pair<String, Integer> key, Message message) {
//        // if this chunk is from a file the peer
//        // has requested to backup (aka is the
//        // initiator peer), and hasn't received
//        // a stored message, update actual rep degree,
//        // and add peer
//        updateMapInformation(backedUpChunksInfo, key, message);
//
//        // if this peer has this chunk stored,
//        // and hasn't received stored message
//        // from this peer yet, update actual
//        // rep degree, and add peer
//        updateMapInformation(storedChunksInfo, key, message);
//
//        if(backupEnhancement && !message.getVersion().equals("1.0")) {
//            updateMapInformation(storedRepliesInfo, key, message);
//        }
//    }

    public void updateChunksInfo(FileChunk key, Message message) {
        // if this chunk is from a file the peer
        // has requested to backup (aka is the
        // initiator peer), and hasn't received
        // a stored message, update actual rep degree,
        // and add peer
        System.out.println("Trying backUpChunksInfo:");
        updateMapInformation(backedUpChunksInfo, key, message);

        // if this peer has this chunk stored,
        // and hasn't received stored message
        // from this peer yet, update actual
        // rep degree, and add peer
        System.out.println("Trying storedChunksInfo:");
        updateMapInformation(storedChunksInfo, key, message);

        if(backupEnhancement && !message.getVersion().equals("1.0")) {
            System.out.println("Trying storedRepliesInfo:");
            updateMapInformation(storedRepliesInfo, key, message);
        }
        System.out.println("Finished updating");
    }

    /**
     * Updates a given ConcurrentHashMaps information at a given key
     * @param map the map to update
     * @param key the key whose value to update
     * @param message the message whose information the map update is based on
     */
//    private void updateMapInformation(ConcurrentHashMap<Pair<String, Integer>, ChunkInfo> map, Pair<String, Integer> key, Message message) {
//        ChunkInfo chunkInfo;
//
//        if(map.containsKey(key) && !map.get(key).isBackedUpByPeer(message.getSenderId())) {
//            chunkInfo = map.get(key);
//            chunkInfo.incActualReplicationDegree();
//            chunkInfo.addPeer(message.getSenderId());
//            map.put(key, chunkInfo);
//        }
//    }

    private void updateMapInformation(ConcurrentHashMap<FileChunk, ChunkInfo> map, FileChunk key, Message message) {
        ChunkInfo chunkInfo;
        if(map.containsKey(key) && !map.get(key).isBackedUpByPeer(message.getSenderId())) {
            chunkInfo = map.get(key);
            chunkInfo.incActualReplicationDegree();
            chunkInfo.addPeer(message.getSenderId());
            map.put(key, chunkInfo);
            System.out.println("Updated with received store message");
        }
    }

    public void startStoringChunks(Message message) {
        storedChunks.putIfAbsent(message.getFileId(), new ArrayList<>());
        //Pair<String, Integer> chunkInfoKey = new Pair<>(message.getFileId(), message.getChunkNo());
        FileChunk chunkInfoKey = new FileChunk(message.getFileId(), message.getChunkNo());
        storedChunksInfo.putIfAbsent(chunkInfoKey, new ChunkInfo(message.getReplicationDeg(), 1));
    }

//    public void addGetChunkRequestInfo(Pair<String, Integer> key) {
//        getChunkRequestsInfo.put(key, true);
//    }
    public void addGetChunkRequestInfo(FileChunk key) {
        getChunkRequestsInfo.put(key, true);
    }

    public void stopRestoringFile(String fileId) {
        restoringFiles.remove(fileId);
        restoringFilesInfo.remove(fileId);
    }

    public void addRestoredFile(Message message, ConcurrentSkipListSet<Message> fileRestoredChunks) {
        restoringFiles.put(message.getFileId(), fileRestoredChunks);
    }
}
