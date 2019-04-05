package protocols;

import message.Message;
import peer.PeerState;
import channels.Channel;
import storage.FileInfo;
import user_interface.UI;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

public class RestoreInitiator implements Runnable {

    private PeerState peerState;
    private String filePath;
    private Channel channel;

    public RestoreInitiator(PeerState peerState, String filePath, Channel channel) {
        this.peerState = peerState;
        this.filePath = filePath;
        this.channel = channel;
    }

    /**
     * Executes the restore protocol.
     * Starts by checking if the file is being backed up by the peer, aborting if otherwise.
     * Then, generates the PUTCHUNK messages for the file chunks of the file being restored and, sends them to the channel.
     */
    @Override
    public void run() {
        UI.printInfo("------------- Executing Restore Protocol -------------");

        ConcurrentHashMap<String, FileInfo> backedUpFilesByPaths = peerState.getBackedUpFiles();
        if (!backedUpFilesByPaths.containsKey(filePath)) {
            UI.printWarning("File " + filePath + " is  not being backed up");
            UI.printInfo("------------------------------------------------------");
            return;
        }

        FileInfo fileInfo = backedUpFilesByPaths.get(filePath);
        String fileId = fileInfo.getFileId();
        int numberOfChunks = fileInfo.getNumberOfChunks();

        peerState.addToRestoringFiles(fileId, fileInfo);
        UI.print("Restoring file with " + numberOfChunks + " chunks");

        ArrayList<Message> chunks = new ArrayList<>();
        for (int i = 0; i < numberOfChunks; i++) {
            chunks.add(new Message(peerState.getVersion(), peerState.getServerId(), fileId, null, Message.MessageType.GETCHUNK, i));
        }

        for (Message chunk : chunks) {
            channel.sendMessage(chunk);
            UI.print("Sending " + chunk.getMessageType() + " message: " + chunk.getChunkNo());
        }

        UI.printInfo("------------------------------------------------------");
    }
}
