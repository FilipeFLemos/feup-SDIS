package protocols;

import message.Message;
import channels.Channel;
import peer.PeerState;
import storage.ChunkInfo;
import storage.FileChunk;
import user_interface.UI;
import utils.Utils;

import java.util.concurrent.ConcurrentHashMap;

public class BackupChunkInitiator implements Runnable {

    private Message message;
    private Channel channel;
    private PeerState peerState;
    private boolean selfDoing = false;

    public BackupChunkInitiator(PeerState peerState, Message message, Channel channel) {
        this.peerState = peerState;
        this.channel = channel;
        this.message = message;
        if (this.message.getSenderId() == -1) {
            selfDoing = true;
        }
    }

    /**
     * Executes the backup protocol for a specific chunk.
     * Starts by checking if the replication degree was achieved meanwhile, aborting if positive.
     * Then, sends the PUTCHUNK message and waits until the replication degree is satisfied.
     * Aborts if the max number of tries is achieved.
     */
    @Override
    public void run() {
        UI.printInfo("----------- Executing Chunk Backup Protocol ----------");

        if (peerState.getChunkRepDeg(message) >= message.getReplicationDeg()) {
            UI.printWarning("Chunk " + message.getChunkNo() + " replication degree was achieved in the meantime");
            UI.printInfo("------------------------------------------------------");
            return;
        }

        peerState.listenForSTORED(message);

        int tries = 1;
        int waitTime = 500;

        do {
            if (tries > Utils.MAX_PUTCHUNK_TRIES) {
                UI.printError("Aborting backup, attempt limit reached");
                UI.printInfo("------------------------------------------------------");
                break;
            }
            channel.sendMessage(message);
            tries++;
            waitTime *= 2;
        } while (!hasDesiredReplicationDeg(waitTime));

        UI.printInfo("------------------------------------------------------");
    }

    /**
     * Checks if the desired replication degree for the chunk has been met
     *
     * @param waitTime - max delay before checking
     * @return true if desired replication degree has been met, false otherwise
     */
    private boolean hasDesiredReplicationDeg(int waitTime) {
        try {
            Thread.sleep(waitTime);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if (selfDoing) {
            int currentDegree = 0;
            FileChunk fileChunk = new FileChunk(message.getFileId(), message.getChunkNo());
            ConcurrentHashMap<FileChunk, ChunkInfo> storedChunks = peerState.getStoredChunks();
            if (storedChunks.containsKey(fileChunk)) {
                currentDegree = storedChunks.get(fileChunk).getCurrentReplicationDeg();
            }
            return currentDegree >= message.getReplicationDeg();
        }

        return peerState.getChunkRepDeg(message) >= message.getReplicationDeg();
    }
}
