package storage;

import message.Message;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ConcurrentSkipListSet;

import user_interface.UI;
import utils.Utils;

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
        maxReservedSpace = Utils.MAX_STORAGE_SPACE;

        backupDir = "peers/peer" + peerId + "/backup";
        restoreDir = "peers/peer" + peerId + "/restore";
        initDirectory(backupDir);
        initDirectory(restoreDir);
    }

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
     *
     * @param message - the chunk message
     * @return true if successful, false otherwise
     */
    public synchronized boolean saveChunk(Message message) {
        if (usedSpace + message.getBody().length > maxReservedSpace)
            return false;

        String fileId = message.getFileId();
        int chunkNo = message.getChunkNo();

        try {
            Path fileDir = Paths.get(backupDir + "/" + fileId);
            if (!Files.exists(fileDir)) {
                Files.createDirectories(fileDir);
            }

            Path chunkPath = Paths.get(backupDir + "/" + fileId + "/" + chunkNo);
            if (!Files.exists(chunkPath)) {
                Files.createFile(chunkPath);
            }
            Files.write(chunkPath, message.getBody());
        } catch (IOException e) {
            e.printStackTrace();
        }

        increaseUsedSpace(message.getBody().length);

        UI.printOK("Chunk " + chunkNo + " (from file " + fileId + ") saved successfully");
        return true;
    }

    /**
     * Deletes the chunk provided if it is stored locally.
     *
     * @param fileId  - the file id
     * @param chunkNo - the chunk number
     */
    public synchronized void deleteChunk(String fileId, int chunkNo) {
        try {
            Path path = Paths.get(backupDir + "/" + fileId + "/" + chunkNo);
            if (Files.exists(path)) {
                decreaseUsedSpace(Files.size(path));
                Files.delete(path);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        UI.printOK("Chunk " + chunkNo + " (from file " + fileId + ") deleted successfully");
    }

    public synchronized void deleteFileFolder(String fileId) {
        try {
            Path path = Paths.get(backupDir + "/" + fileId);
            if (Files.exists(path)) {
                Files.delete(path);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        UI.printOK("File " + fileId + " deleted successfully");
    }

    /**
     * Loads a chunk stored locally.
     *
     * @param fileId  - the file id
     * @param chunkNo - the chunk number
     * @return The chunk message
     */
    public synchronized Message loadChunk(String fileId, int chunkNo) {
        byte[] body = null;
        try {
            Path chunkPath = Paths.get(this.backupDir + "/" + fileId + "/" + chunkNo);
            body = Files.readAllBytes(chunkPath);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return new Message(version, peerId, fileId, body, Message.MessageType.CHUNK, chunkNo);
    }

    /**
     * Saves a file locally
     *
     * @param filePath - the original file path
     */
    public synchronized void saveFile(String filePath, ConcurrentSkipListSet<Message> fileChunks) {
        byte[] fileData = mergeChunks(fileChunks);
        try {
            Path path = Paths.get(this.restoreDir + "/" + cropFilesDir(filePath));
            if (!Files.exists(path)) {
                Files.createFile(path);
            }
            Files.write(path, fileData);
        } catch (IOException e) {
            e.printStackTrace();
        }

        UI.printOK("File " + filePath + " restored successfully");
    }

    /**
     * Removes the directory
     *
     * @param filePath - the file path
     * @return the fileName
     */
    private String cropFilesDir(String filePath) {
        int index = filePath.lastIndexOf('/');
        return filePath.substring(index);
    }

    /**
     * Merges all the files from a given file.
     *
     * @return the file data
     */
    private byte[] mergeChunks(ConcurrentSkipListSet<Message> fileChunks) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();

        try {
            for (Message chunk : fileChunks) {
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

    private void increaseUsedSpace(long amount) {
        this.usedSpace += amount;
    }

    private void decreaseUsedSpace(long amount) {
        this.usedSpace -= amount;
    }
}
