package protocol;

import message.Message;
import message.MessageType;
import peer.PeerController;
import receiver.Channel;
import peer.Peer;
import utils.Globals;
import utils.Utils;

import java.io.*;
import java.util.ArrayList;

public class BackupInitiator implements Runnable{

    private String filePath;
    private int replicationDegree;
    private int numberChunks;
    private ArrayList<Message> chunks;
    private String fileID;
    private File file;
    private PeerController peerController;
    private Channel channel;

    /**
     * Instantiates a new Backup initiator.
     *
     * @param filePath          the file path
     * @param replicationDegree the replication degree
     * @param channel           the message
     */
    public BackupInitiator(PeerController peerController, String filePath, int replicationDegree, Channel channel) {
        this.peerController = peerController;
        this.channel = channel;
        this.filePath = filePath;
        this.replicationDegree = replicationDegree;

        file = new File(filePath);
        fileID = Utils.getFileID(file);

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
        int waitTime = 500; // initially 500 so in first iteration it doubles to 1000

        // notify peer to listen for these chunks' stored messages
        for(Message chunk : chunks)
            peerController.backedUpChunkListenForStored(chunk);

        do {
            tries++; waitTime *= 2;
            System.out.println("Sent " + filePath + " PUTCHUNK messages " + tries + " times");

            if(tries > Globals.MAX_PUTCHUNK_TRIES) {
                System.out.println("Aborting backup, attempt limit reached");
                return;
            }

            for(Message chunk : chunks){
                channel.sendMessage(chunk);
                System.out.println("Sent " + chunk.getType() + " message: " + chunk.getChunkIndex());
            }

        } while(!confirmStoredMessages(waitTime));

        peerController.addBackedUpFile(filePath, fileID, numberChunks);
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
                chunks.add(new Message(peerController.getPeerVersion(), peerController.getPeerID(), fileID, body, MessageType.PUTCHUNK, i, replicationDegree));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
      * Checks if replication degrees have been satisfied for given chunks
      *
      * @param waitTime delay before starting to check
      * @return true if for every chunk observed rep degree >= desired rep degree
      */
    private boolean confirmStoredMessages(int waitTime) {
        try {
            //TODO: remove sleeps
            Thread.sleep(waitTime);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        //TODO testar isto
        for(Message chunk : chunks){
            if(peerController.getBackedUpChunkRepDegree(chunk) >= chunk.getReplicationDeg()){
                chunks.remove(chunk);
            }
        }

        return chunks.isEmpty();
    }
}
