package peer;

import java.io.Serializable;

public class ChunkFile implements Serializable {

    private String fileId;
    private Integer chunkNo;

    public ChunkFile(String fileId, Integer chunkNo){
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
	public boolean equals(Object obj) {
		if (this == obj)
			return true;

		if (obj == null)
			return false;

		if (getClass() != obj.getClass())
			return false;

		ChunkFile other = (ChunkFile) obj;

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
