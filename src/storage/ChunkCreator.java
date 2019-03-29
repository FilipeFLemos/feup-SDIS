package storage;

import message.Message;
import message.MessageType;
import utils.Globals;
import utils.Utils;

import java.io.*;
import java.util.ArrayList;

public class ChunkCreator {
    private ArrayList<Message> chunkList;

    private int nChunks;

    private String fileID;
    private int replicationDegree;
    private int peerID;
    private String version;

    /**
     * Instantiates a new Chunk creator.
     *
     * @param filePath          the file path
     * @param replicationDegree the replication degree
     * @param peerID            the peer id
     * @param version           the version
     */
    public ChunkCreator(String filePath, int replicationDegree, int peerID, String version) {
        this.replicationDegree = replicationDegree;
        this.peerID = peerID;
        this.version = version;

        File file = new File(filePath);
        this.fileID = Utils.getFileID(file);

        initChunkList(file);
        createChunks(file);
    }

    /**
      * Creates the chunk list for a given file
      * @param file file to be split
      */
    private void initChunkList(File file) {
        long fileSize = file.length();

        this.nChunks = (int) (fileSize / Globals.CHUNK_MAX_SIZE + 1);
        this.chunkList = new ArrayList<>();
    }

    /**
      * Splits a file in chunks
      * @param file file to be split
      */
    private void createChunks(File file) {
        try {
            FileInputStream fileStream = new FileInputStream(file);
            BufferedInputStream bufferedFile = new BufferedInputStream(fileStream);

            for(int chunkIndex = 0; chunkIndex < this.nChunks; chunkIndex++) {
                byte[] tempBuf = new byte[Globals.CHUNK_MAX_SIZE];
                byte[] finalBuf;

                int nBytesRead = bufferedFile.read(tempBuf);

                if(nBytesRead == -1)
                    finalBuf = new byte[0];
                else if(nBytesRead < Globals.CHUNK_MAX_SIZE)
                    finalBuf = new byte[nBytesRead];
                else
                    finalBuf = new byte[Globals.CHUNK_MAX_SIZE];

                System.arraycopy(tempBuf, 0, finalBuf, 0, finalBuf.length);
                chunkList.add(new Message(version, peerID, fileID, finalBuf, MessageType.PUTCHUNK, chunkIndex, replicationDegree));
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Gets chunk list.
     *
     * @return the chunk list
     */
    public ArrayList<Message> getChunkList() {
        return chunkList;
    }
}
