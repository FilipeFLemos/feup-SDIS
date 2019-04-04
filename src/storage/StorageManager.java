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
import user_interface.UI;

public class StorageManager implements Serializable {

    private static final long serialVersionUID = 1L;
    private String version;
    private int peerId;
    private long usedSpace;
    private long maxReservedSpace;

    private String backupDir;
    private String restoreDir;

    public StorageManager(String version, int peerId) {
        this.version = version;
        this.peerId = peerId;
        usedSpace = 0;
        maxReservedSpace = Globals.MAX_PEER_STORAGE;

        backupDir = "peers/peer/" + peerId + "/backup";
        restoreDir = "peers/peer/" + peerId + "/restore/files";
        initDirectory(backupDir);
        initDirectory(restoreDir);
    }

    //TODO dentro de cada backup/restore folder têm de ter um fileId

    /**
     * Creates the directory with the given path if it does not exist.
     */
    private void initDirectory(String path) {
        Path dirPath = Paths.get(path);

        try {
            if (!Files.exists(dirPath)) {
                Files.createDirectories(dirPath);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Saves the chunk locally if there is enough free space.
     * @param message - the chunk message
     * @return true if successful, false otherwise
     */
    public synchronized boolean saveChunk(Message message) {
        if(usedSpace + message.getBody().length > maxReservedSpace)
            return false;

        try {
            //Path chunkPath = Paths.get(this.backupDir + "/" +message.getFileId() + "-" + message.getChunkNo());
            Path fileDir = Paths.get(backupDir + "/" + message.getFileId());
            if(!Files.exists(fileDir)) {
                Files.createDirectories(fileDir);
            }

            Path chunkPath = Paths.get(this.backupDir + "/" +message.getFileId() + "/" + message.getChunkNo());
            if(!Files.exists(chunkPath)) {
                Files.createFile(chunkPath);
            }
            Files.write(chunkPath, message.getBody());
        }
        catch(IOException e){
            e.printStackTrace();
        }

        increaseUsedSpace(message.getBody().length);
        return true;
    }

    /**
     * Deletes the chunk provided if it is stored locally.
     *  @param fileId - the file id
     * @param chunkNo - the chunk number
     */
    public synchronized void deleteChunk(String fileId, int chunkNo) {
        //Path path = Paths.get(this.backupDir + "/" + fileId + "-" + chunkNo);
        Path path = Paths.get(this.backupDir + "/" + fileId + "/" + chunkNo);

        try {
            if(Files.exists(path)) {
                decreaseUsedSpace(Files.size(path));
                Files.delete(path);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Loads a chunk stored locally.
     * @param fileId - the file id
     * @param chunkNo - the chunk number
     * @return The chunk message
     */
    public synchronized Message loadChunk(String fileId, int chunkNo) {
        Path chunkPath = Paths.get(this.backupDir + "/"+fileId+"_"+chunkNo);

        byte[] body = null;
        try {
            body = Files.readAllBytes(chunkPath);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return new Message(version, peerId, fileId, body, Message.MessageType.CHUNK, chunkNo);
    }

    /**
     * Saves a file locally
     * @param filePath - the original file path
     */
    public synchronized void saveFile(String filePath, ConcurrentSkipListSet<Message> fileChunks) {
        byte[] fileData = mergeChunks(fileChunks);

        Path path = Paths.get(this.restoreDir + "/" + cropFilesDir(filePath));
        UI.printInfo("FULL PATH: " + path.toAbsolutePath());

        try {
            if(!Files.exists(path)) {
                Files.createFile(path);
            }
            Files.write(path, fileData);
        } catch (IOException e) {
            e.printStackTrace();
        }

        UI.printOK("File " + filePath + " restored successfully");
    }

    //TODO isto nao podia ser mais feio, arranjar melhor maneira
    private String cropFilesDir(String filePath){
        String dir = "files/";
        return filePath.substring(dir.length());
    }

    /**
     * Merges all the files from a given file.
     * @return the file data
     */
    private byte[] mergeChunks(ConcurrentSkipListSet<Message> fileChunks) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();

        try {
            for(Message chunk : fileChunks) {
                stream.write(chunk.getBody());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return stream.toByteArray();
    }

    public long getUsedSpace() {
        return usedSpace;
    }

    public long getAvailableSpace() {
        return maxReservedSpace - usedSpace;
    }

    private void increaseUsedSpace(long amount){
        this.usedSpace += amount;
    }

    private void decreaseUsedSpace(long amount){
        this.usedSpace -= amount;
    }
}
