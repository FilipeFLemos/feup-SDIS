package peer;

import message.*;
import storage.FileSystem;
import utils.Globals;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.*;

/**
  * Peer controller, where the peer's state is kept
 */
public class PeerController implements Serializable {


    private static final long serialVersionUID = 1L;
    private String version;
    private int peerId;
    private FileSystem fileSystem;

    private boolean backupEnhancement;
    private boolean restoreEnhancement;

    private ConcurrentHashMap<String, FileInfo> backedUpFilesByPaths;
    private ConcurrentHashMap<FileChunk, ChunkInfo> backedUpChunksInfo;

    private ConcurrentHashMap<String, ArrayList<Integer>> storedChunksByFileId;
    private ConcurrentHashMap<FileChunk, ChunkInfo> storedChunksInfo;
    private ConcurrentHashMap<FileChunk, ChunkInfo> storedChunksInfo_ENH;

    //TODO: nao deveria ser fileinfo pq ele quer guardar o path e nao o id
    private ConcurrentHashMap<String, FileInfo> fileInfoByRestoredFile;
    private ConcurrentHashMap<String, ConcurrentSkipListSet<Message>> chunksByRestoredFile;
    private ConcurrentHashMap<FileChunk, Boolean> isBeingRestoredByFileChunk;

    public PeerController(String version, int peerId) {
        this.version = version;
        this.peerId = peerId;

        backedUpFilesByPaths = new ConcurrentHashMap<>();
        backedUpChunksInfo = new ConcurrentHashMap<>();

        storedChunksByFileId = new ConcurrentHashMap<>();
        storedChunksInfo = new ConcurrentHashMap<>();
        storedChunksInfo_ENH = new ConcurrentHashMap<>();

        fileInfoByRestoredFile = new ConcurrentHashMap<>();
        chunksByRestoredFile = new ConcurrentHashMap<>();
        isBeingRestoredByFileChunk = new ConcurrentHashMap<>();


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
    }

    /**
     * Starts listening to stored messages for a chunk the peer is storing
     * @param chunk
     */
    public void listenForSTORED_ENH(Message chunk) {
        FileChunk key = new FileChunk(chunk.getFileId(),chunk.getChunkNo());
        storedChunksInfo_ENH.putIfAbsent(key, new ChunkInfo(chunk.getReplicationDeg(), 0));
    }

    /**
     * Starts listening to stored messages for a chunk that the peer is backing up
     * @param chunk the chunk
     */
    public void listenForSTORED(Message chunk) {
        FileChunk key = new FileChunk(chunk.getFileId(),chunk.getChunkNo());
        backedUpChunksInfo.putIfAbsent(key, new ChunkInfo(chunk.getReplicationDeg(), 0));
    }

    /**
     * Starts listening to generic stored messages
     * @param chunk
     */
    public void listenForChunkReplies(Message chunk) {
        FileChunk key = new FileChunk(chunk.getFileId(),chunk.getChunkNo());
        isBeingRestoredByFileChunk.putIfAbsent(key, false);
    }

    /**
     * Gets backed up chunk's observed rep degree.
     *
     * @param chunk the chunk
     * @return the backed up chunk's observed rep degree
     */
    public int getBackedUpChunkRepDegree(Message chunk) {
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
        backedUpFilesByPaths.put(filePath, new FileInfo(fileID, chunkAmount));
    }

    /**
     * Gets backed up file id.
     *
     * @param filePath the file path
     * @return the backed up file id
     */
    public String getBackedUpFileID(String filePath) {
        if(!backedUpFilesByPaths.containsKey(filePath))
            return null;

        FileInfo fileInfo = backedUpFilesByPaths.get(filePath);
        return fileInfo.getFileId();
    }

    /**
     * Gets backed up file chunk amount.
     *
     * @param filePath the file path
     * @return the backed up file chunk amount
     */
    public Integer getBackedUpFileChunkAmount(String filePath) {
        if(!backedUpFilesByPaths.containsKey(filePath))
            return 0;

        FileInfo fileInfo = backedUpFilesByPaths.get(filePath);
        return fileInfo.getNumberOfChunks();
    }

    /**
     * Gets most satisfied chunk.
     *
     * @return the most satisfied chunk
     */
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

        FileChunk key = new FileChunk(fileID, chunkIndex);
        storedChunksInfo.remove(key);

        if(storedChunksByFileId.get(fileID).contains(chunkIndex))
            storedChunksByFileId.get(fileID).remove((Integer) chunkIndex);
    }

    /**
     * Add file to restoring files structure.
     *
     * @param fileID      the file id
     * @param filePath    the file path
     * @param chunkAmount the chunk amount
     */
    public void addToRestoringFiles(String fileID, String filePath, int chunkAmount) {
        chunksByRestoredFile.putIfAbsent(fileID, new ConcurrentSkipListSet<>());
        fileInfoByRestoredFile.putIfAbsent(fileID, new FileInfo(filePath, chunkAmount));
    }

    /**
     * Saves a restored file locally.
     *
     * @param fileID the file id
     */
    public void saveRestoredFile(String fileID) {
        byte[] fileBody = mergeRestoredFile(fileID);
        //String filePath = restoringFilesInfo.get(fileID).getKey();
        String filePath = fileInfoByRestoredFile.get(fileID).getFileId();

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
        ConcurrentSkipListSet<Message> fileChunks = chunksByRestoredFile.get(fileID);

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
      * Represents the peer's state by printing all the information about the structures it keeps and its file system manager
      */
    @Override
    public String toString() {
        StringBuilder output = new StringBuilder();
        output.append("Current Peer state:\n");

        output.append("Files whose backup was initiated by this peer:\n");
        for (Map.Entry<String, FileInfo> entry : backedUpFilesByPaths.entrySet()) {
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

        for (Map.Entry<String, ArrayList<Integer>> entry : storedChunksByFileId.entrySet()) {
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

    public String getVersion() {
        return version;
    }

    public int getPeerId() {
        return peerId;
    }

    public boolean isBackupEnhancement() {
        return backupEnhancement;
    }

    public boolean isRestoreEnhancement() {
        return restoreEnhancement;
    }

    public ConcurrentHashMap<String, ArrayList<Integer>> getStoredChunks() {
        return storedChunksByFileId;
    }

    public ConcurrentHashMap<FileChunk, ChunkInfo> getStoredChunksInfo() {
        return storedChunksInfo;
    }

    public ConcurrentHashMap<FileChunk, Boolean> getIsBeingRestoredByFileChunk() {
        return isBeingRestoredByFileChunk;
    }

    public ConcurrentHashMap<FileChunk, ChunkInfo> getStoredChunksInfo_ENH() {
        return storedChunksInfo_ENH;
    }

    public ConcurrentHashMap<String, ConcurrentSkipListSet<Message>> getRestoringFiles() {
        return chunksByRestoredFile;
    }

    public ConcurrentHashMap<String, FileInfo> getRestoringFilesInfo() {
        return fileInfoByRestoredFile;
    }

    public void removeStoredChunksFile(String fileId) {
        storedChunksByFileId.remove(fileId);
    }

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
            System.out.println("Trying storedChunksInfo_ENH:");
            updateMapInformation(storedChunksInfo_ENH, key, message);
        }
        System.out.println("Finished updating");
    }

    /**
     * Updates a given ConcurrentHashMaps information at a given key
     * @param map the map to update
     * @param key the key whose value to update
     * @param message the message whose information the map update is based on
     */
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
        storedChunksByFileId.putIfAbsent(message.getFileId(), new ArrayList<>());
        //Pair<String, Integer> chunkInfoKey = new Pair<>(message.getFileId(), message.getChunkNo());
        FileChunk chunkInfoKey = new FileChunk(message.getFileId(), message.getChunkNo());
        storedChunksInfo.putIfAbsent(chunkInfoKey, new ChunkInfo(message.getReplicationDeg(), 1));
    }

    public void addGetChunkRequestInfo(FileChunk key) {
        isBeingRestoredByFileChunk.put(key, true);
    }

    public void stopRestoringFile(String fileId) {
        chunksByRestoredFile.remove(fileId);
        fileInfoByRestoredFile.remove(fileId);
    }

    public void addRestoredFile(Message message, ConcurrentSkipListSet<Message> fileRestoredChunks) {
        chunksByRestoredFile.put(message.getFileId(), fileRestoredChunks);
    }
}
