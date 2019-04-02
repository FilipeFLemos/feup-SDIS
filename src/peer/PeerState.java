package peer;

import message.*;
import storage.ChunkInfo;
import storage.FileChunk;
import storage.FileInfo;
import storage.StorageManager;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.*;

/**
  * Peer controller, where the peer's state is kept
 */
public class PeerState implements Serializable {

    private static final long serialVersionUID = 1L;
    private String version;
    private Integer serverId;
    private StorageManager storageManager;

    private ConcurrentHashMap<String, FileInfo> backedUpFilesByPaths;
    private ConcurrentHashMap<FileChunk, ChunkInfo> backedUpChunksInfo;

    private ConcurrentHashMap<String, ArrayList<Integer>> storedChunksByFileId;
    private ConcurrentHashMap<FileChunk, ChunkInfo> storedChunksInfo;
    private ConcurrentHashMap<FileChunk, ChunkInfo> storedChunksInfo_ENH;


    private ConcurrentHashMap<String, FileInfo> restoredFileInfoByFileId;
    private ConcurrentHashMap<String, ConcurrentSkipListSet<Message>> chunksByRestoredFile;
    private ConcurrentHashMap<FileChunk, Boolean> isBeingRestoredChunkMap;

    private boolean backupEnhancement;
    private boolean restoreEnhancement;

    public PeerState(String version, int serverId) {
        this.version = version;
        this.serverId = serverId;

        backedUpFilesByPaths = new ConcurrentHashMap<>();
        backedUpChunksInfo = new ConcurrentHashMap<>();

        storedChunksByFileId = new ConcurrentHashMap<>();
        storedChunksInfo = new ConcurrentHashMap<>();
        storedChunksInfo_ENH = new ConcurrentHashMap<>();

        restoredFileInfoByFileId = new ConcurrentHashMap<>();
        chunksByRestoredFile = new ConcurrentHashMap<>();
        isBeingRestoredChunkMap = new ConcurrentHashMap<>();


        if(version.equals("1.0")) {
            backupEnhancement = false;
            restoreEnhancement = false;
        }
        else {
            System.out.println("Enhancements activated");
            backupEnhancement = true;
            restoreEnhancement = true;
        }

        storageManager = new StorageManager(version, serverId);
    }

    /**
     * Starts listening to stored messages for a chunk the peer is storing
     * @param chunk
     */
    public void listenForSTORED_ENH(Message chunk) {
        FileChunk fileChunk = new FileChunk(chunk.getFileId(),chunk.getChunkNo());
        storedChunksInfo_ENH.putIfAbsent(fileChunk, new ChunkInfo(chunk.getReplicationDeg(), 0));
    }

    /**
     * Starts listening to stored messages for a chunk that the peer is backing up
     * @param chunk the chunk
     */
    public void listenForSTORED(Message chunk) {
        FileChunk fileChunk = new FileChunk(chunk.getFileId(),chunk.getChunkNo());
        backedUpChunksInfo.putIfAbsent(fileChunk, new ChunkInfo(chunk.getReplicationDeg(), 0));
    }

    /**
     * Starts listening to generic chunk messages
     * @param chunk
     */
    public void listenForCHUNK(Message chunk) {
        FileChunk fileChunk = new FileChunk(chunk.getFileId(),chunk.getChunkNo());
        isBeingRestoredChunkMap.putIfAbsent(fileChunk, false);
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

    public FileChunk getMostStoredChunk() {
        if(storedChunksInfo.isEmpty()) {
            System.out.println("Stored chunks info is empty");
            return null;
        }

        FileChunk bestChunk = null;
        int max = -1;

        for (Map.Entry<FileChunk, ChunkInfo> chunk : storedChunksInfo.entrySet()) {
            if(chunk.getValue().getReplicationDegDifference() > max){
                max = chunk.getValue().getReplicationDegDifference();
                bestChunk = chunk.getKey();
            }
        }
        return bestChunk;
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
        restoredFileInfoByFileId.putIfAbsent(fileID, new FileInfo(fileID, chunkAmount, filePath));
    }

    public void startStoringChunks(Message message) {
        storedChunksByFileId.putIfAbsent(message.getFileId(), new ArrayList<>());
        FileChunk fileChunk = new FileChunk(message.getFileId(), message.getChunkNo());
        storedChunksInfo.putIfAbsent(fileChunk, new ChunkInfo(message.getReplicationDeg(), 1));
    }

    public void addStoredChunk(Message message) {
        ArrayList<Integer> storedChunks = storedChunksByFileId.get(message.getFileId());
        storedChunks.add(message.getChunkNo());
        storedChunksByFileId.put(message.getFileId(), storedChunks);
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
            chunkInfo.increaseCurrentRepDeg();
            chunkInfo.addPeer(message.getSenderId());
            map.put(key, chunkInfo);
            System.out.println("Updated with received store message");
        }
    }

    public void setIsBeingRestored(FileChunk key) {
        isBeingRestoredChunkMap.put(key, true);
    }

    public void addRestoredFileChunks(Message message) {
        String fileId = message.getFileId();
        ConcurrentSkipListSet<Message> chunks = chunksByRestoredFile.get(fileId);
        chunks.add(message);
        chunksByRestoredFile.put(fileId, chunks);
    }

    public boolean hasRestoredAllChunks(String fileId){
        int currentSize = chunksByRestoredFile.get(fileId).size();
        int desiredSize = restoredFileInfoByFileId.get(fileId).getNumberOfChunks();

        return currentSize == desiredSize;
    }

    /**
     * Saves a restored file locally.
     *
     * @param fileID the file id
     */
    public void saveRestoredFile(String fileID) {
        ConcurrentSkipListSet<Message> fileChunks = chunksByRestoredFile.get(fileID);
        String filePath = restoredFileInfoByFileId.get(fileID).getFilePath();
        storageManager.saveFile(filePath, fileChunks);
    }

    public void stopRestoringFile(String fileId) {
        chunksByRestoredFile.remove(fileId);
        restoredFileInfoByFileId.remove(fileId);
    }

    public void removeChunk(FileChunk fileChunk) {
        isBeingRestoredChunkMap.remove(fileChunk);
    }

    /**
     * Deletes a  chunk.
     *
     * @param fileId           the file id
     * @param chunkNo       the chunk index
     * @param updateMaxStorage true if max storage value should be updated in the process
     */
    public void deleteChunk(String fileId, int chunkNo, boolean updateMaxStorage) {
        storageManager.deleteChunk(fileId, chunkNo, updateMaxStorage);

        FileChunk fileChunk = new FileChunk(fileId, chunkNo);
        ChunkInfo chunkInfo = storedChunksInfo.remove(fileChunk);
        chunkInfo.removePeer(serverId);

        if(storedChunksByFileId.get(fileId).contains(chunkNo)) {
            storedChunksByFileId.get(fileId).remove((Integer) chunkNo);
        }
    }

    public void removeStoredChunksFile(String fileId) {
        storedChunksByFileId.remove(fileId);
    }

    public StorageManager getStorageManager() {
        return storageManager;
    }

    public String getVersion() {
        return version;
    }

    public int getServerId() {
        return serverId;
    }

    public boolean isBackupEnhancement() {
        return backupEnhancement;
    }

    public boolean isRestoreEnhancement() {
        return restoreEnhancement;
    }

    public ConcurrentHashMap<String, ArrayList<Integer>> getStoredChunksByFileId() {
        return storedChunksByFileId;
    }

    public ConcurrentHashMap<FileChunk, ChunkInfo> getStoredChunksInfo() {
        return storedChunksInfo;
    }

    public ConcurrentHashMap<FileChunk, Boolean> getIsBeingRestoredChunkMap() {
        return isBeingRestoredChunkMap;
    }

    public ConcurrentHashMap<FileChunk, ChunkInfo> getStoredChunksInfo_ENH() {
        return storedChunksInfo_ENH;
    }

    public ConcurrentHashMap<String, ConcurrentSkipListSet<Message>> getChunksByRestoredFile() {
        return chunksByRestoredFile;
    }

    public String getPeerState(){
        String output = "";
        output += "Files backed up:";
        for (Map.Entry<String, FileInfo> entry : backedUpFilesByPaths.entrySet()) {
            FileInfo fileInfo = entry.getValue();
            output += "\n\t FileId: " + entry.getKey();
            output += "\n\t Path: " + fileInfo.getFilePath();

            for(int i=0; i < fileInfo.getNumberOfChunks(); i++){
                ChunkInfo chunkInfo =  backedUpChunksInfo.get(new FileChunk(fileInfo.getFileId(), i));
                if(i == 0){
                    output += "\n\t Desired Replication Degree: " + chunkInfo.getDesiredReplicationDeg() + "\n Chunks:";
                }
                output += "\n\t\t Chunk No " + i + " - Current replication degree: " + chunkInfo.getCurrentReplicationDeg();

            }
        }

        output += "\nChunks stored:";
        for (Map.Entry<String, ArrayList<Integer>> entry : storedChunksByFileId.entrySet()) {
            output += "\n\t FileId: " + entry.getKey();
            for(int chunkNo : entry.getValue()){
                ChunkInfo chunkInfo = storedChunksInfo.get(new FileChunk(entry.getKey(), chunkNo));
                //TODO chunkSize
                output += "\n\t\t Chunk No " + chunkNo + " - Current replication degree: " + chunkInfo.getCurrentReplicationDeg();
            }
        }

        output += "\nStorage: \n\t Available Memory(kB): "+ storageManager.getAvailableSpace()/1000 + "\n\t Used Memory(kB): " + storageManager.getUsedSpace()/1000;
        return output;
    }
}
