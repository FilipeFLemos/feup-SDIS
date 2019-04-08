package peer;

import message.*;
import storage.ChunkInfo;
import storage.FileChunk;
import storage.FileInfo;
import storage.StorageManager;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.*;

import user_interface.UI;

public class PeerState implements Serializable {

    private static final long serialVersionUID = 1L;
    private String version;
    private Integer serverId;
    private StorageManager storageManager;

    private ConcurrentHashMap<String, FileInfo> backedUpFiles;
    private ConcurrentHashMap<FileChunk, ChunkInfo> backedUpChunks;

    private ConcurrentHashMap<String, ArrayList<Integer>> storedChunksByFileId;
    private ConcurrentHashMap<FileChunk, ChunkInfo> storedChunks;
    private ConcurrentHashMap<FileChunk, ChunkInfo> storedChunks_ENH;

    private ConcurrentHashMap<String, Set<Integer>> peersBackingUpFile;
    private Set<String> deletedFiles;

    private ConcurrentHashMap<String, FileInfo> filesBeingRestored;
    private ConcurrentHashMap<String, ConcurrentSkipListSet<Message>> restoredChunks;
    private ConcurrentHashMap<FileChunk, Boolean> isBeingRestoredChunkMap;

    private ConcurrentHashMap<FileChunk, ChunkInfo> chunksReclaimed;

    private boolean isEnhanced;

    PeerState(String version, int serverId) {
        setVersion(version);
        this.serverId = serverId;
        storageManager = new StorageManager(version, serverId);

        backedUpFiles = new ConcurrentHashMap<>();
        backedUpChunks = new ConcurrentHashMap<>();

        storedChunksByFileId = new ConcurrentHashMap<>();
        storedChunks = new ConcurrentHashMap<>();
        storedChunks_ENH = new ConcurrentHashMap<>();

        filesBeingRestored = new ConcurrentHashMap<>();
        restoredChunks = new ConcurrentHashMap<>();
        isBeingRestoredChunkMap = new ConcurrentHashMap<>();

        chunksReclaimed = new ConcurrentHashMap<>();
        peersBackingUpFile = new ConcurrentHashMap<>();
        deletedFiles = ConcurrentHashMap.newKeySet();
    }

    void setVersion(String version){
        this.version = version;
        isEnhanced = !version.equals("1.0");
    }

    /**
     * Initiates the stored chunks container for enhanced peers. This informs the peer to start listening for STORED
     * messages of the provided chunk No. (Peers storing file)
     * @param chunk - the received chunk
     */
    public void listenForSTORED_ENH(Message chunk) {
        FileChunk fileChunk = new FileChunk(chunk.getFileId(),chunk.getChunkNo());
        storedChunks_ENH.putIfAbsent(fileChunk, new ChunkInfo(chunk.getReplicationDeg(), 0));
    }

    /**
     * Initiates the stored chunks container. This informs the peer to start listening for STORED
     * messages of the provided chunk No. (Backup initiator peer)
     * @param chunk - the received chunk
     */
    public void listenForSTORED(Message chunk) {
        FileChunk fileChunk = new FileChunk(chunk.getFileId(),chunk.getChunkNo());
        backedUpChunks.putIfAbsent(fileChunk, new ChunkInfo(chunk.getReplicationDeg(), 0));
    }

    /**
     * Initiates the being restored chunks container. This informs the peer to start listening for CHUNK
     * messages of the provided chunk No. (Restore initiator peer)
     * @param chunk - the received chunk
     */
    public void listenForCHUNK(Message chunk) {
        FileChunk fileChunk = new FileChunk(chunk.getFileId(),chunk.getChunkNo());
        isBeingRestoredChunkMap.putIfAbsent(fileChunk, false);
    }

    /**
     * Add the file to the restoring file structures. (Restore initiator peer)
     *
     * @param fileId - the file id
     */
    public void addToRestoringFiles(String fileId, FileInfo fileInfo) {
        restoredChunks.putIfAbsent(fileId, new ConcurrentSkipListSet<>());
        filesBeingRestored.putIfAbsent(fileId, fileInfo);
    }

    /**
     * Initializes the stored chunk maps (Backup initiator peer)
     * @param message - the PUTCHUNK message
     */
    public void startStoringChunks(Message message) {
        storedChunksByFileId.putIfAbsent(message.getFileId(), new ArrayList<>());
        FileChunk fileChunk = new FileChunk(message.getFileId(), message.getChunkNo());
        storedChunks.putIfAbsent(fileChunk, new ChunkInfo(message.getReplicationDeg(), 1, message.getBody().length));
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
     * Updates the backed up chunks container and peers backing up file.
     * @param fileChunk - the file chunk
     * @param message - the received STORED message
     */
    public void updateBackedUpChunks(FileChunk fileChunk, Message message) {
        updateContainer(backedUpChunks, fileChunk, message);
        addPeerBackingUpFile(fileChunk,message.getSenderId());
    }

    /**
     * Adds the id of the sender to the set of peers backing up file if it isn't already there.
     * @param fileChunk - the file chunk
     * @param senderId - the received STORED message
     */
    private void addPeerBackingUpFile(FileChunk fileChunk, int senderId) {
        if(peersBackingUpFile.containsKey(fileChunk.getFileId())){
            Set<Integer> peers = peersBackingUpFile.get(fileChunk.getFileId());
            peers.add(senderId);
            peersBackingUpFile.put(fileChunk.getFileId(), peers);
        }
    }

    /**
     * Updates the stored chunks information.
     * @param fileChunk - the file chunk
     * @param message - the received STORED message
     */
    public void updateStoredChunks(FileChunk fileChunk, Message message) {
        updateContainer(storedChunks, fileChunk, message);

        if(isEnhanced) {
            updateContainer(storedChunks_ENH, fileChunk, message);
        }
    }

    /**
     * Updates map container at given key with the received STORED message
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
            UI.printOK("Updated with received STORED message");
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
        ConcurrentSkipListSet<Message> chunks = restoredChunks.get(fileId);
        chunks.add(message);
        restoredChunks.put(fileId, chunks);
    }

    /**
     * Checks if all the chunks from the given file were restored.
     * @param fileId - the provided file id
     * @return true if the chunks were all restored or false if otherwise
     */
    public boolean hasRestoredAllChunks(String fileId){
        int currentSize = restoredChunks.get(fileId).size();
        int desiredSize = filesBeingRestored.get(fileId).getNumberOfChunks();

        return currentSize == desiredSize;
    }

    /**
     * Saves the restored file in the restored peer folder
     * @param fileId - the id of the file to be saved
     */
    public void saveFileToRestoredFolder(String fileId) {
        String filePath = filesBeingRestored.get(fileId).getFilePath();
        ConcurrentSkipListSet<Message> chunks = restoredChunks.get(fileId);
        storageManager.saveFile(filePath, chunks);
    }

    /**
     * Removes the file from the containers responsible for restoring files.
     * @param fileId - the id of the file to be removed
     */
    public void stopRestoringFile(String fileId) {
        restoredChunks.remove(fileId);
        filesBeingRestored.remove(fileId);
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
     * @param isReclaiming - boolean to specify if the delete was done by a reclaim service
     */
    public void deleteChunk(String fileId, int chunkNo, boolean isReclaiming) {
        Message chunkBeingDeleted = storageManager.loadChunk(fileId,chunkNo);
        storageManager.deleteChunk(fileId, chunkNo);

        FileChunk fileChunk = new FileChunk(fileId, chunkNo);
        ChunkInfo chunkInfo = storedChunks.remove(fileChunk);
        chunkInfo.removePeer(serverId);
        chunkInfo.decreaseCurrentRepDeg();

        ArrayList<Integer> storedChunks = storedChunksByFileId.get(fileId);
        storedChunks.remove((Integer) chunkNo);
        if(storedChunks.isEmpty()) {
            storedChunksByFileId.remove(fileId);
            storageManager.deleteFileFolder(fileId);
        }

        if(isReclaiming && !chunkInfo.achievedDesiredRepDeg()){
            chunkInfo.setBody(chunkBeingDeleted.getBody());
            chunksReclaimed.putIfAbsent(fileChunk,chunkInfo);
        }
    }

    public void removePeerBackingUpFile(String fileId, Integer senderId) {
        Set<Integer> peers = peersBackingUpFile.get(fileId);
        peers.remove(senderId);
        if(peers.isEmpty()){
            peersBackingUpFile.remove(fileId);
            deletedFiles.remove(fileId);
        }
    }

    public void removeReclaimedChunk(FileChunk fileChunk) {
        chunksReclaimed.remove(fileChunk);
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

    public ConcurrentHashMap<String, ArrayList<Integer>> getStoredChunksByFileId() {
        return storedChunksByFileId;
    }

    public ConcurrentHashMap<FileChunk, ChunkInfo> getStoredChunks() {
        return storedChunks;
    }

    public ConcurrentHashMap<FileChunk, Boolean> getIsBeingRestoredChunkMap() {
        return isBeingRestoredChunkMap;
    }

    public ConcurrentHashMap<FileChunk, ChunkInfo> getStoredChunks_ENH() {
        return storedChunks_ENH;
    }

    public ConcurrentHashMap<String, ConcurrentSkipListSet<Message>> getRestoredChunks() {
        return restoredChunks;
    }

    public ConcurrentHashMap<String, FileInfo> getBackedUpFiles() {
        return backedUpFiles;
    }

    public ConcurrentHashMap<FileChunk, ChunkInfo> getChunksReclaimed() {
        return chunksReclaimed;
    }

    public ConcurrentHashMap<String, Set<Integer>> getPeersBackingUpFile() {
        return peersBackingUpFile;
    }

    public Set<String> getDeletedFiles() {
        return deletedFiles;
    }

    /**
     * Calculates the current replication degree of the backed up chunk.
     * @param message - the provided chunk message
     * @return the computed replication degree or 0 if the chunk isn't backed up by the peer.
     */
    public int getChunkRepDeg(Message message) {
        int currentDegree = 0;
        FileChunk fileChunk = new FileChunk(message.getFileId(),message.getChunkNo());
        if(backedUpChunks.containsKey(fileChunk)) {
            currentDegree = backedUpChunks.get(fileChunk).getCurrentReplicationDeg();
        }

        return currentDegree;
    }

    public void initPeersWithFile(String fileId) {
        peersBackingUpFile.putIfAbsent(fileId, new HashSet<>());
    }

    /**
     * Adds file to the backed up files container.
     * @param filePath  - the file path
     * @param fileId - the id of the file
     * @param numberOfChunks - the number of chunks
     */
    public void backUpFile(String filePath, String fileId, int numberOfChunks) {
        backedUpFiles.put(filePath, new FileInfo(fileId, numberOfChunks, filePath));
    }

    /**
     * Computes the best chunk for being removed.
     * @return the computed chunk
     */
    public FileChunk getMostStoredChunk() {
        if(storedChunks.isEmpty()) {
            UI.printWarning("Stored chunks info is empty");
            return null;
        }

        FileChunk bestChunk = null;
        int max = -1;

        for (Map.Entry<FileChunk, ChunkInfo> chunk : storedChunks.entrySet()) {
            if(chunk.getValue().getReplicationDegDifference() > max){
                max = chunk.getValue().getReplicationDegDifference();
                bestChunk = chunk.getKey();
            }
        }
        return bestChunk;
    }

    /**
     * Delete files from backed up files container. Starts by deleting every backed up chunk from the selected file.
     * @param filePath - the filepath of the file
     */
    public void deleteBackedUp(String filePath) {
        FileInfo fileInfo = backedUpFiles.remove(filePath);
        for(int i=0; i < fileInfo.getNumberOfChunks(); i++){
            FileChunk fileChunk = new FileChunk(fileInfo.getFileId(), i);
            backedUpChunks.remove(fileChunk);
        }
        if(!version.equals("1.0")) {
            deletedFiles.add(fileInfo.getFileId());
        }
    }

    /**
     * Retrieves the peer state.
     * @return - the string that describes it.
     */
    String getPeerState(){
        String output = "";
        output += "Files backed up:";
        for (Map.Entry<String, FileInfo> entry : backedUpFiles.entrySet()) {
            FileInfo fileInfo = entry.getValue();
            output += "\n  FileId: " + fileInfo.getFileId();
            output += "\n  Path: " + fileInfo.getFilePath();

            for(int i=0; i < fileInfo.getNumberOfChunks(); i++){
                ChunkInfo chunkInfo =  backedUpChunks.get(new FileChunk(fileInfo.getFileId(), i));
                if(i == 0){
                    output += "\n  Desired Replication Degree: " + chunkInfo.getDesiredReplicationDeg() + "\n Chunks:";
                }
                output += "\n    Chunk No " + i + " - Current replication degree: " + chunkInfo.getCurrentReplicationDeg();

            }
        }

        output += "\nChunks stored:";
        for (Map.Entry<String, ArrayList<Integer>> entry : storedChunksByFileId.entrySet()) {
            output += "\n  FileId: " + entry.getKey();
            Collections.sort(entry.getValue());
            for(int chunkNo : entry.getValue()){
                ChunkInfo chunkInfo = storedChunks.get(new FileChunk(entry.getKey(), chunkNo));
                output += "\n     Chunk No " + chunkNo + " (" + chunkInfo.getSize()/1000 +" kB) - Current replication degree: " + chunkInfo.getCurrentReplicationDeg();
            }
        }

        output += "\nStorage: \n  Available Memory(kB): "+ storageManager.getAvailableSpace()/1000 + "\n  Used Memory(kB): " + storageManager.getUsedSpace()/1000;
        return output;
    }
}
