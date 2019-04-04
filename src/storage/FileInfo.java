package storage;

import java.io.Serializable;

public class FileInfo implements Serializable {

    private static final long serialVersionUID = 1L;
	private String fileId;
    private Integer numberOfChunks;
    private String filePath = "";

	public FileInfo(String fileId, Integer numberOfChunks){
		this.fileId = fileId;
		this.numberOfChunks = numberOfChunks;
	}

    public FileInfo(String fileId, Integer numberOfChunks, String filePath){
        this(fileId, numberOfChunks);
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

		if (obj == null)
			return false;

		if (getClass() != obj.getClass())
			return false;

		FileInfo other = (FileInfo) obj;

		if (!numberOfChunks.equals(other.numberOfChunks))
			return false;

		if (fileId == null) {
			return other.fileId == null;
		} else return fileId.equals(other.fileId);

	}
}
