package storage;

import java.io.Serializable;

/**
 * Represents a file's chunk
 */
public class FileChunk implements Serializable {

    private static final long serialVersionUID = 1L;
	private String fileId;
    private Integer chunkNo;

    public FileChunk(String fileId, Integer chunkNo){
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
		int hash = (chunkNo ^ (chunkNo >>> 8));
		hash = 31 * hash + fileId.hashCode();
		return hash;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;

		if (obj == null)
			return false;

		if (getClass() != obj.getClass())
			return false;

		FileChunk other = (FileChunk) obj;

		if (!chunkNo.equals(other.chunkNo))
			return false;

		if (fileId == null) {
			return other.fileId == null;
		} else return fileId.equals(other.fileId);

	}
}
