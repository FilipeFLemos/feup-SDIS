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
        storageManager = new StorageManager(version, serverId);

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
    }

    /**
     * Initiates the stored chunks container for enhanced peers. This informs the peer to start listening for STORED
     * messages of the provided chunk No.
     * @param chunk - the received chunk
     */
    public void listenForSTORED_ENH(Message chunk) {
        FileChunk fileChunk = new FileChunk(chunk.getFileId(),chunk.getChunkNo());
        storedChunksInfo_ENH.putIfAbsent(fileChunk, new ChunkInfo(chunk.getReplicationDeg(), 0));
    }

    /**
     * Initiates the stored chunks container. This informs the peer to start listening for STORED
     * messages of the provided chunk No.
     * @param chunk - the received chunk
     */
    public void listenForSTORED(Message chunk) {
        FileChunk fileChunk = new FileChunk(chunk.getFileId(),chunk.getChunkNo());
        backedUpChunksInfo.putIfAbsent(fileChunk, new ChunkInfo(chunk.getReplicationDeg(), 0));
    }

    /**
     * Initiates the being restored chunks container. This informs the peer to start listening for CHUNK
     * messages of the provided chunk No.
     * @param chunk - the received chunk
     */
    public void listenForCHUNK(Message chunk) {
        FileChunk fileChunk = new FileChunk(chunk.getFileId(),chunk.getChunkNo());
        isBeingRestoredChunkMap.putIfAbsent(fileChunk, false);
    }

    /**
     * Add file to restoring files structure.
     *
     * @param fileID      the file id
     */
    public void addToRestoringFiles(String fileID, FileInfo fileInfo) {
        chunksByRestoredFile.putIfAbsent(fileID, new ConcurrentSkipListSet<>());
        restoredFileInfoByFileId.putIfAbsent(fileID, fileInfo);
        System.out.println("----The path will be : " + fileInfo.getFilePath());
    }

    /**
     * Initializes the stored chunk maps
     * @param message - the PUTCHUNK message
     */
    public void startStoringChunks(Message message) {
        storedChunksByFileId.putIfAbsent(message.getFileId(), new ArrayList<>());
        FileChunk fileChunk = new FileChunk(message.getFileId(), message.getChunkNo());
        storedChunksInfo.putIfAbsent(fileChunk, new ChunkInfo(message.getReplicationDeg(), 1, message.getBody().length));
    }

    /**
     * Add chunk to the file list of stored chunks
     * @param message - the received chunk message
     */
    public void addStoredChunk(Message message) {
        ArrayList<Integer> storedChunks = storedChunksByFileId.get(message.getFileId());
        storedChunks.add(message.getChunkNo());
        storedChunksByFileId.put(message.getFileId(), storedChunks);
    }

    /**
     * Updates stored and backed up Chunk information.
     * @param fileChunk - the chunk
     * @param message - the STORED message
     */
    public void updateChunkInfo(FileChunk fileChunk, Message message) {
        updateContainer(storedChunksInfo, fileChunk, message);
        updateContainer(backedUpChunksInfo, fileChunk, message);

        if(backupEnhancement && !message.getVersion().equals("1.0")) {
            updateContainer(storedChunksInfo_ENH, fileChunk, message);
        }
        System.out.println("Finished updating");
    }

    /**
     * Updates map container at given key with the received STORED message IF:
     * 1) Chunk is stored by the peer and received this message from a new peer
     * 2) Chunk was requested by the peer and hasn't received it yet - backup initiator peer
     * In both cases, the replication degree of the chunk is increased and the sender peer is added to the chunks mirrors.
     * @param map - The map container
     * @param fileChunk - The chunk
     * @param message - The STORED message
     */
    private void updateContainer(ConcurrentHashMap<FileChunk, ChunkInfo> map, FileChunk fileChunk, Message message) {
        ChunkInfo chunkInfo;
        if(map.containsKey(fileChunk) && !map.get(fileChunk).isBackedUpByPeer(message.getSenderId())) {
            chunkInfo = map.get(fileChunk);
            chunkInfo.increaseCurrentRepDeg();
            chunkInfo.addPeer(message.getSenderId());
            map.put(fileChunk, chunkInfo);
            System.out.println("Updated with received store message");
        }
    }

    /**
     * Marks the received CHUNK message as being restored.
     * @param fileChunk - the received Chunk
     */
    public void setIsBeingRestored(FileChunk fileChunk) {
        isBeingRestoredChunkMap.put(fileChunk, true);
    }

    /**
     * Adds the chunk to the list of chunks being restored by the message file id
     * @param message - the message
     */
    public void addRestoredFileChunks(Message message) {
        String fileId = message.getFileId();
        ConcurrentSkipListSet<Message> chunks = chunksByRestoredFile.get(fileId);
        chunks.add(message);
        chunksByRestoredFile.put(fileId, chunks);
    }

    /**
     * Checks if all the chunks from the given file were restored.
     * @param fileId - the provided file id
     * @return true if the chunks were all restored or false if otherwise
     */
    public boolean hasRestoredAllChunks(String fileId){
        int currentSize = chunksByRestoredFile.get(fileId).size();
        int desiredSize = restoredFileInfoByFileId.get(fileId).getNumberOfChunks();

        return currentSize == desiredSize;
    }

    /**
     * Saves the restored file in the restored peer folder
     * @param fileId - the id of the file to be saved
     */
    public void saveFileToRestoredFolder(String fileId) {
        String filePath = restoredFileInfoByFileId.get(fileId).getFilePath();
        ConcurrentSkipListSet<Message> chunks = chunksByRestoredFile.get(fileId);
        storageManager.saveFile(filePath, chunks);
    }

    /**
     * Removes the file from the containers responsible for restoring files.
     * @param fileId - the id of the file to be removed
     */
    public void stopRestoringFile(String fileId) {
        chunksByRestoredFile.remove(fileId);
        restoredFileInfoByFileId.remove(fileId);
    }

    /**
     * Chunk was already being restored by another peer. Therefore it is removed from the being restored chunks map
     * @param fileChunk - the chunk being restored
     */
    public void removeChunk(FileChunk fileChunk) {
        isBeingRestoredChunkMap.remove(fileChunk);
    }

    /**
     * Deletes a chunk that was saved locally.
     * @param fileId - the file id where the chunk belongs
     * @param chunkNo - the chunk number
     */
    public void deleteChunk(String fileId, int chunkNo) {
        storageManager.deleteChunk(fileId, chunkNo);

        FileChunk fileChunk = new FileChunk(fileId, chunkNo);
        ChunkInfo chunkInfo = storedChunksInfo.remove(fileChunk);
        chunkInfo.removePeer(serverId);

        ArrayList<Integer> storedChunks = storedChunksByFileId.get(fileId);
        storedChunks.remove((Integer) chunkNo);
        if(storedChunks.isEmpty()) {
            storedChunksByFileId.remove(fileId);
        }
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

    public ConcurrentHashMap<String, FileInfo> getBackedUpFilesByPaths() {
        return backedUpFilesByPaths;
    }

    /******************************************************************************************************
     *                                        Initiators methods
     *******************************************************************************************************/

    /**
     * Calculates the current replication degree of the backed up chunk.
     * @param message - the provided chunk message
     * @return the computed replication degree or 0 if the chunk isn't backed up by the peer.
     */
    public int getChunkRepDeg(Message message) {
        int currentDegree = 0;
        FileChunk fileChunk = new FileChunk(message.getFileId(),message.getChunkNo());
        if(backedUpChunksInfo.containsKey(fileChunk)) {
            currentDegree = backedUpChunksInfo.get(fileChunk).getCurrentReplicationDeg();
        }

        return currentDegree;
    }

    /**
     * Adds file to the backed up files container.
     * @param filePath  - the file path
     * @param fileId - the id of the file
     * @param numberOfChunks - the number of chunks
     */
    public void backUpFile(String filePath, String fileId, int numberOfChunks) {
        backedUpFilesByPaths.put(filePath, new FileInfo(fileId, numberOfChunks, filePath));
    }

    /**
     * Computes the best chunk for being removed.
     * @return the computed chunk
     */
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
     * Retrieves the peer state.
     * @return - the string that describes it.
     */
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
                output += "\n\t\t Chunk No " + chunkNo + " (" + chunkInfo.getSize()/1000 +" kB) - Current replication degree: " + chunkInfo.getCurrentReplicationDeg();
            }
        }

        output += "\nStorage: \n\t Available Memory(kB): "+ storageManager.getAvailableSpace()/1000 + "\n\t Used Memory(kB): " + storageManager.getUsedSpace()/1000;
        return output;
    }
}
