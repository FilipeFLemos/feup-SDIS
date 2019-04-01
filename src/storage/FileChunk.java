package storage;

import java.io.Serializable;

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
		final int prime = 31;

		int result = 1;

		result = prime * result + chunkNo;

		result = prime * result + ((fileId == null) ? 0 : fileId.hashCode());

		return result;
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

		if (chunkNo != other.chunkNo)
			return false;

		if (fileId == null) {
			if (other.fileId != null)
				return false;
		} else if (!fileId.equals(other.fileId))
			return false;

		return true;
	}
}
