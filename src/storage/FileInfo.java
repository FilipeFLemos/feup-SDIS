package storage;

import java.io.Serializable;

public class FileInfo implements Serializable {

    private static final long serialVersionUID = 1L;
    private String fileId;
    private Integer numberOfChunks;
    private String filePath;

    /**
     * Creates a container for the file details
     *
     * @param fileId         - the file id
     * @param numberOfChunks - the number of chunks of that file
     * @param filePath       - the path of the file
     */
    public FileInfo(String fileId, Integer numberOfChunks, String filePath) {
        this.fileId = fileId;
        this.numberOfChunks = numberOfChunks;
        this.filePath = filePath;
    }

    public String getFileId() {
        return fileId;
    }

    public Integer getNumberOfChunks() {
        return numberOfChunks;
    }

    public String getFilePath() {
        return filePath;
    }

    @Override
    public int hashCode() {
        int hash = (numberOfChunks ^ (numberOfChunks >>> 8));
        hash = 31 * hash + fileId.hashCode();
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;

        if (obj == null || getClass() != obj.getClass())
            return false;

        FileInfo other = (FileInfo) obj;
        return (numberOfChunks.equals(other.numberOfChunks) && fileId.equals(other.fileId));
    }
}
