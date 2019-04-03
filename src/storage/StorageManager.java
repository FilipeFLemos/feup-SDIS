package storage;

import message.Message;
import utils.Globals;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ConcurrentSkipListSet;

public class StorageManager implements Serializable {

    private static final long serialVersionUID = 1L;
    private String version;
    private int peerID;
    private long usedSpace;
    private long maxReservedSpace;

    private String backupDir;
    private String restoreDir;

    public StorageManager(String version, int peerID) {
        this.version = version;
        this.peerID = peerID;
        this.maxReservedSpace = Globals.MAX_PEER_STORAGE;
        this.usedSpace = 0;

        this.backupDir = "localData/backup/peer" + peerID;
        this.restoreDir = "localData/restore/peer" + peerID;
        initDirectories();
    }

    /**
     * Initiates local directories to structure saved data
     */
    private void initDirectories() {
        Path backupPath = Paths.get(backupDir);
        Path restorePath = Paths.get(restoreDir);

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
     * Tries to save a chunk
     *
     * @param chunk the chunk to store
     * @return true if successful, false otherwise
     */
    public synchronized boolean saveChunk(Message chunk) {
        if(!hasFreeSpace(chunk.getBody().length))
            return false;

        try {
            Path chunkPath = Paths.get(this.backupDir + "/"+chunk.getFileId()+"_"+chunk.getChunkNo());

            if(!Files.exists(chunkPath))
                Files.createFile(chunkPath);

            Files.write(chunkPath, chunk.getBody());
        }
        catch(IOException e){
            e.printStackTrace();
        }

        increaseUsedSpace(chunk.getBody().length);
        return true;
    }

    /**
     * Deletes a chunk (if present).
     *
     * @param fileId           the file id
     * @param chunkNo       the chunk index
     * @param updateMaxStorage true if maxReservedSpace should be updated, false otherwise
     */
    public synchronized void deleteChunk(String fileId, int chunkNo, boolean updateMaxStorage) {
        Path path = Paths.get(this.backupDir +"/"+fileId+"_"+chunkNo);

        try {
            if(Files.exists(path)) {
                long chunkBytes = Files.size(path);
                decreaseUsedSpace(chunkBytes);
                if(updateMaxStorage)
                    maxReservedSpace -= chunkBytes;
                Files.delete(path);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Retrieves a chunk stored locally.
     *
     * @param fileID     the original file's id
     * @param chunkIndex the chunk index
     * @return message with the chunk
     */
    public synchronized Message retrieveChunk(String fileID, int chunkIndex) {
        Path chunkPath = Paths.get(this.backupDir + "/"+fileID+"_"+chunkIndex);

        byte[] body = null;
        try {
            body = Files.readAllBytes(chunkPath);
        } catch (IOException e) {
            e.printStackTrace();
        }

        Message chunk = new Message(version, peerID, fileID, body, Message.MessageType.CHUNK, chunkIndex);
        return chunk;
    }

    /**
     * Saves a file locally
     *
     * @param filePath the path to save the file
     */
    public synchronized void saveFile(String filePath, ConcurrentSkipListSet<Message> fileChunks) {
        byte[] body = mergeRestoredFile(fileChunks);

        Path path = Paths.get(this.restoreDir + "/" + filePath);
        System.out.println(filePath);
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
     * Merges a restored file into a single byte[]
     *
     * @return the file as a byte[]
     */
    public byte[] mergeRestoredFile(ConcurrentSkipListSet<Message> fileChunks) {
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
      * Checks if peer still has free space to backup a specific chunk
      * @param size size of chunk to be stored
      * @return true if usedSpace + size <= maxReservedSpace, false otherwise
      */
    private boolean hasFreeSpace(long size) {
        return usedSpace + size <= maxReservedSpace;
    }


    public long getUsedSpace() {
        return usedSpace;
    }

    public long getAvailableSpace() {
        return maxReservedSpace - usedSpace;
    }

    public void increaseUsedSpace(long amount){
        this.usedSpace += amount;
    }

    public void decreaseUsedSpace(long amount){
        this.usedSpace -= amount;
    }
}
