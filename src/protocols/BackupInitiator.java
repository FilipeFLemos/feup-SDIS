package protocols;

import message.Message;
import peer.PeerState;
import channels.Channel;
import utils.Globals;
import utils.Utils;
import user_interface.UI;

import java.io.*;
import java.util.ArrayList;


public class BackupInitiator implements Runnable {

    private String filePath;
    private int replicationDegree;
    private int numberOfChunks;
    private ArrayList<Message> chunks;
    private String fileId;
    private File file;
    private PeerState peerState;
    private Channel channel;

    public BackupInitiator(PeerState peerState, String filePath, int replicationDegree, Channel channel) {
        this.peerState = peerState;
        this.channel = channel;
        this.filePath = filePath;
        this.replicationDegree = replicationDegree;

        file = new File(filePath);
        fileId = Utils.getFileID(filePath);

        numberOfChunks = (int) (file.length() / Globals.MAX_CHUNK_SIZE + 1);
        chunks = new ArrayList<>();
    }

    /**
     * Executes the backup protocol for a file.
     */
    @Override
    public void run() {

        UI.printInfo("-------------- Executing Backup Protocol -------------");

        splitIntoChunks();

        peerState.initPeersWithFile(fileId);

        for (Message chunk : chunks) {
            peerState.listenForSTORED(chunk);
        }

        int tries = 1;
        int waitTime = 500;

        do {
            UI.print("Sending " + filePath + " PUTCHUNK messages " + tries + " times");

            if (tries > Globals.MAX_PUTCHUNK_TRIES) {
                UI.printError("Aborting backup, attempt limit reached");
                UI.printInfo("------------------------------------------------------");
                return;
            }

            for (Message chunk : chunks) {
                channel.sendMessage(chunk);
                UI.print("Sending " + chunk.getMessageType() + " message: " + chunk.getChunkNo());
            }
            tries++;
            waitTime *= 2;

        } while (!wereAllSTOREDReceived(waitTime));

        peerState.backUpFile(filePath, fileId, numberOfChunks);
        UI.printOK("File " + filePath + " backed up");
        UI.printInfo("------------------------------------------------------");
    }


    /**
     * Splits file in chunks
     */
    private void splitIntoChunks() {
        try {
            FileInputStream fileStream = new FileInputStream(file);
            BufferedInputStream bufferedFile = new BufferedInputStream(fileStream);

            for (int i = 0; i < numberOfChunks; i++) {
                byte[] body;
                byte[] aux = new byte[Globals.MAX_CHUNK_SIZE];
                int bytesRead = bufferedFile.read(aux);

                if (bytesRead == -1) {
                    body = new byte[0];
                } else if (bytesRead < Globals.MAX_CHUNK_SIZE) {
                    body = new byte[bytesRead];
                } else {
                    body = new byte[Globals.MAX_CHUNK_SIZE];
                }

                System.arraycopy(aux, 0, body, 0, body.length);
                chunks.add(new Message(peerState.getVersion(), peerState.getServerId(), fileId, body, Message.MessageType.PUTCHUNK, i, replicationDegree));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * After the given waitTime, checks if all chunks have achieved their desired replication degree, while removing those that have from the chunks container.
     *
     * @param waitTime - the delay before starting to check
     * @return true if all the chunks achieved their desired replication degree and false if otherwise
     */
    private boolean wereAllSTOREDReceived(int waitTime) {
        try {
            Thread.sleep(waitTime);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        chunks.removeIf(chunk -> peerState.getChunkRepDeg(chunk) >= chunk.getReplicationDeg());

        return chunks.isEmpty();
    }
}
