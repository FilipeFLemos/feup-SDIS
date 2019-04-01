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
		final int prime = 31;

		int result = 1;

		result = prime * result + numberOfChunks;

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

		FileInfo other = (FileInfo) obj;

		if (numberOfChunks != other.numberOfChunks)
			return false;

		if (fileId == null) {
			if (other.fileId != null)
				return false;
		} else if (!fileId.equals(other.fileId))
			return false;

		return true;
	}
}
