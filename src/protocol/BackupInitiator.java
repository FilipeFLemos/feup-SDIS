package protocol;

import message.Message;
import peer.PeerController;
import receiver.Receiver;
import utils.Globals;
import utils.Utils;

import java.io.*;
import java.util.ArrayList;

public class BackupInitiator implements Runnable{

    private String filePath;
    private int replicationDegree;
    private int numberChunks;
    private ArrayList<Message> chunks;
    private String fileId;
    private File file;
    private PeerController peerController;
    private Receiver channel;

    /**
     * Instantiates a new BackupChunk initiator.
     *
     * @param filePath          the file path
     * @param replicationDegree the replication degree
     * @param channel           the message
     */
    public BackupInitiator(PeerController peerController, String filePath, int replicationDegree, Receiver channel) {
        this.peerController = peerController;
        this.channel = channel;
        this.filePath = filePath;
        this.replicationDegree = replicationDegree;

        file = new File(filePath);
        fileId = Utils.getFileID(filePath);

        numberChunks = (int) (file.length() / Globals.MAX_CHUNK_SIZE + 1);
        chunks = new ArrayList<>();
    }

    /**
      * Method executed when thread starts running. Executes the backup protocol as an initiator peer.
      */
    @Override
    public void run() {
        splitIntoChunks();

        int tries = 0;
        int waitTime = 500;

        for(Message chunk : chunks)
            peerController.listenForSTORED(chunk);

        do {
            tries++; waitTime *= 2;
            System.out.println("Sent " + filePath + " PUTCHUNK messages " + tries + " times");

            if(tries > Globals.MAX_PUTCHUNK_TRIES) {
                System.out.println("Aborting backup, attempt limit reached");
                return;
            }

            for(Message chunk : chunks){
                channel.sendMessage(chunk);
                System.out.println("Sent " + chunk.getMessageType() + " message: " + chunk.getChunkNo());
            }

        } while(!wereAllSTOREDReceived(waitTime));

        peerController.addBackedUpFile(filePath, fileId, numberChunks);
        System.out.println("File " + filePath + " backed up");
    }


    /**
     * Splits file in chunks
     */
    private void splitIntoChunks() {
        try {
            FileInputStream fileStream = new FileInputStream(file);
            BufferedInputStream bufferedFile = new BufferedInputStream(fileStream);

            for(int i = 0; i < numberChunks; i++) {
                byte[] body;
                byte[] aux = new byte[Globals.MAX_CHUNK_SIZE];
                int bytesRead = bufferedFile.read(aux);

                if(bytesRead == -1)
                    body = new byte[0];
                else if(bytesRead < Globals.MAX_CHUNK_SIZE)
                    body = new byte[bytesRead];
                else
                    body = new byte[Globals.MAX_CHUNK_SIZE];

                System.arraycopy(aux, 0, body, 0, body.length);
                chunks.add(new Message(peerController.getVersion(), peerController.getPeerId(), fileId, body, Message.MessageType.PUTCHUNK, i, replicationDegree));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
      * After the given waitTime, checks if all chunks have achieved their desired replication degree, while removing those that have from the chunks container.
      * @param waitTime - the delay before starting to check
      * @return true if all the chunks achieved their desired replication degree and false if otherwise
      */
    private boolean wereAllSTOREDReceived(int waitTime) {
        try {
            Thread.sleep(waitTime);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        chunks.removeIf( chunk -> peerController.getBackedUpChunkRepDegree(chunk) >= chunk.getReplicationDeg());

        return chunks.isEmpty();
    }
}
