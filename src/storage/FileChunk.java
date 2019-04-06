package storage;

import java.io.Serializable;

public class FileChunk implements Serializable {

    private static final long serialVersionUID = 1L;
    private String fileId;
    private Integer chunkNo;

    /**
     * Creates a chunk for the file id provided and with the respective chunk number
     *
     * @param fileId  - the file id
     * @param chunkNo - the chunk number
     */
    public FileChunk(String fileId, Integer chunkNo) {
        this.fileId = fileId;
        this.chunkNo = chunkNo;
    }

    public String getFileId() {
        return fileId;
    }

    public Integer getChunkNo() {
        return chunkNo;
    }

    @Override
    public int hashCode() {
        int hash = (chunkNo ^ (chunkNo >>> 32));
        hash = 31 * hash + fileId.hashCode();
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;

        if (obj == null || getClass() != obj.getClass())
            return false;

        FileChunk other = (FileChunk) obj;

        return (chunkNo.equals(other.chunkNo) && fileId.equals(other.fileId));
    }
}
