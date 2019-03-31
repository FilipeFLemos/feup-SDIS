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
}
