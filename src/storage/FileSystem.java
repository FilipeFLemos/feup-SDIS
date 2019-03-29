package storage;

import message.ChunkMessage;
import message.PackedMessage;

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileSystem implements Serializable {

    private static final String BACKUP_DIRECTORY = "backup";
    private static final String RESTORE_DIRECTORY = "restore";

    /**
      * Peer reserved space for backing chunks
      */
    private long maxStorage;

    /**
      * Currently used space for backing chunks
      */
    private long usedStorage;

    /**
      * Protocol version being used
      */
    private String peerVersion;

    /**
      * Peer ID
      */
    private int peerID;

    private String baseDirectory;
    private String backupDirectory;
    private String restoreDirectory;

    /**
     * Initiates the file system manager
     * @param peerVersion
     * @param peerID
     * @param maxStorage
     * @param baseDirectory
     */
    public FileSystem(String peerVersion, int peerID, long maxStorage, String baseDirectory) {
        this.maxStorage = maxStorage;
        this.baseDirectory = baseDirectory + "/";

        this.peerVersion = peerVersion;
        this.peerID = peerID;

        this.backupDirectory = baseDirectory + BACKUP_DIRECTORY;
        this.restoreDirectory = baseDirectory + RESTORE_DIRECTORY;

        initDirectories();
    }

    /**
     * Tries to store a chunk
     *
     * @param chunk the chunk to store
     * @return true if successful, false otherwise
     */
    public synchronized boolean storeChunk(PackedMessage chunk) {
        if(!hasFreeSpace(chunk.getBody().length))
            return false;

        try {
            Path chunkPath = Paths.get(this.backupDirectory + "/"+chunk.getFileID()+"_"+chunk.getChunkIndex());

            if(!Files.exists(chunkPath))
                Files.createFile(chunkPath);

            Files.write(chunkPath, chunk.getBody());
        }
        catch(IOException e){
            e.printStackTrace();
        }

        this.usedStorage += chunk.getBody().length;
        return true;
    }

    /**
     * Retrieves a chunk stored locally.
     *
     * @param fileID     the original file's id
     * @param chunkIndex the chunk index
     * @return message with the chunk
     */
    public synchronized PackedMessage retrieveChunk(String fileID, int chunkIndex) {
        Path chunkPath = Paths.get(this.backupDirectory + "/"+fileID+"_"+chunkIndex);

        byte[] buf = null;
        try {
            buf = Files.readAllBytes(chunkPath);
        } catch (IOException e) {
            e.printStackTrace();
        }

        PackedMessage chunk = new ChunkMessage(peerVersion, peerID, fileID, chunkIndex, buf);
        return chunk;
    }

    /**
     * Saves a file locally
     *
     * @param filePath the path to save the file
     * @param body     the content to be saved
     */
    public synchronized void saveFile(String filePath, byte[] body) {
        Path path = Paths.get(this.restoreDirectory + "/" + filePath);
        System.out.println("FULL PATH: " + path.toAbsolutePath());

        try {
            if(!Files.exists(path))
                Files.createFile(path);

            Files.write(path, body);
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("File " + filePath + " restored successfully");
    }

    /**
     * Deletes a chunk (if present).
     *
     * @param fileID           the file id
     * @param chunkIndex       the chunk index
     * @param updateMaxStorage true if maxStorage should be updated, false otherwise
     */
    public synchronized void deleteChunk(String fileID, int chunkIndex, boolean updateMaxStorage) {
        Path path = Paths.get(this.backupDirectory+"/"+fileID+"_"+chunkIndex);
        long savedStorage = 0;

        try {
            if(Files.exists(path)) {
                savedStorage = Files.size(path);
                Files.delete(path);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        usedStorage -= savedStorage;
        if(updateMaxStorage)
            maxStorage -= savedStorage;
    }

    /**
      * Initiates local directories to structure saved data
      */
    private void initDirectories() {
        Path backupPath = Paths.get(this.backupDirectory);
        Path restorePath = Paths.get(this.restoreDirectory);

        try {
            if (!Files.exists(backupPath))
                Files.createDirectories(backupPath);

            if (!Files.exists(restorePath))
                Files.createDirectories(restorePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
      * Checks if peer still has free space to backup a specific chunk
      * @param size size of chunk to be stored
      * @return true if usedStorage + size > maxStorage, false otherwise
      */
    private boolean hasFreeSpace(long size) {
        return usedStorage + size > maxStorage;
    }

    /**
     * Gets used storage.
     *
     * @return the used storage
     */
    public long getUsedStorage() {
        return usedStorage;
    }

    /**
     * Gets max storage.
     *
     * @return the max storage
     */
    public long getMaxStorage() {
        return maxStorage;
    }
}
