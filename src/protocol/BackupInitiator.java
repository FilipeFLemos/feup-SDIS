package protocol;

import message.Message;
import receiver.Channel;
import peer.Peer;
import storage.ChunkCreator;
import utils.Globals;

import java.util.ArrayList;

public class BackupInitiator extends ProtocolInitiator {

    private String filePath;
    private int replicationDegree;

    /**
     * Instantiates a new Backup initiator.
     *
     * @param peer              the peer
     * @param filePath          the file path
     * @param replicationDegree the replication degree
     * @param channel           the message
     */
    public BackupInitiator(Peer peer, String filePath, int replicationDegree, Channel channel) {
        super(peer, channel);
        this.filePath = filePath;
        this.replicationDegree = replicationDegree;
    }

    /**
      * Method executed when thread starts running. Executes the backup protocol as an initiator peer.
      */
    @Override
    public void run() {
        ChunkCreator creator = new ChunkCreator(filePath, replicationDegree, peer.getPeerId(), peer.getProtocolVersion());

        ArrayList<Message> chunkList = creator.getChunkList();
        String fileID = new String(chunkList.get(0).getFileID());
        int chunkAmmount = chunkList.size();

        int tries = 0;
        int waitTime = 500; // initially 500 so in first iteration it doubles to 1000

        // notify peer to listen for these chunks' stored messages
        for(Message chunk : chunkList)
            peer.getController().backedUpChunkListenForStored(chunk);

        do {
            tries++; waitTime *= 2;
            System.out.println("Sent " + filePath + " PUTCHUNK messages " + tries + " times");

            if(tries > Globals.MAX_PUTCHUNK_TRIES) {
                System.out.println("Aborting backup, attempt limit reached");
                return;
            }
            sendMessages(chunkList);
        } while(!confirmStoredMessages(chunkList, waitTime));

        peer.getController().addBackedUpFile(filePath, fileID, chunkAmmount);
        System.out.println("File " + filePath + " backed up");
    }

    /**
      * Checks if replication degrees have been satisfied for given chunks
      *
      * @param chunkList chunks to be verified
      * @param waitTime delay before starting to check
      * @return true if for every chunk observed rep degree >= desired rep degree
      */
    private boolean confirmStoredMessages(ArrayList<Message> chunkList, int waitTime) {
        try {
            //TODO: remove sleeps
            Thread.sleep(waitTime);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        for(int i = 0; i < chunkList.size(); i++) {
            // if degree is satisfied, remove from list
            Message chunk = chunkList.get(i);
            if (peer.getController().getBackedUpChunkRepDegree(chunk) >= chunk.getReplicationDeg()) {
                chunkList.remove(chunk);
                i--;
            }
        }

        return chunkList.isEmpty();
    }
}
