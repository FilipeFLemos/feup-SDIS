package channels;

import peer.PeerState;
import protocols.BackupChunkInitiator;
import storage.ChunkInfo;
import message.Message;
import storage.FileChunk;
import peer.Peer;
import utils.Utils;
import user_interface.UI;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;

import static utils.Utils.MAX_THREADS;

public class MessageHandler {

    private PeerState peerState;
    private Peer peer;
    private ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(MAX_THREADS);

    public MessageHandler(Peer peer) {
        this.peer = peer;
        this.peerState = peer.getPeerState();
    }

    /**
      * Handles a message and sends it to the thread pool.
      * Ignores messages sent my itself unless they are REMOVED messages.
      *
      * @param message message to be handled
      * @param address address used in GETCHUNK message (TCP address). Unless the peer is enhanced, this field is always
      * null.
      */
    void handleMessage(Message message, InetAddress address) {

        if(message.getMessageType()!= Message.MessageType.REMOVED && message.getSenderId().equals(peer.getServerId())) {
            return;
        }

        int randomWait = 0;
        switch(message.getMessageType()) {
            case PUTCHUNK:
                if(peer.isEnhanced()) {
                    randomWait = Utils.getRandom(0, Utils.MAX_DELAY_BACKUP_ENH);
                    peerState.listenForSTORED_ENH(message);
                }

                scheduledExecutorService.schedule(() -> handlePUTCHUNK(message), randomWait, TimeUnit.MILLISECONDS);
                break;
            case STORED:
                scheduledExecutorService.submit(() -> handleSTORED(message));
                break;
            case GETCHUNK:
                peerState.listenForCHUNK(message);
                randomWait = Utils.getRandom(0, Utils.MAX_DELAY_CHUNK);
                scheduledExecutorService.schedule(() -> handleGETCHUNK(message, address), randomWait, TimeUnit.MILLISECONDS);
                break;
            case CHUNK:
                scheduledExecutorService.submit(() -> handleCHUNK(message));
                break;
            case DELETE:
                scheduledExecutorService.submit(() -> handleDELETE(message));
                break;
            case REMOVED:
                scheduledExecutorService.submit(() -> handleREMOVED(message));
                break;
            case CONTROL:
                scheduledExecutorService.submit(() -> handleCONTROL(message));
                break;
            case ACK_DELETE:
                scheduledExecutorService.submit(() -> handleACK_DELETE(message));
                break;
            default:
                UI.printError("Message type "+message.getMessageType()+" is not a valid type");
        }

    }

    /**
     * Handles a PUTCHUNK message.
     * Starts by checking if this peer was the one asking for this file to be backed up, ignoring if positive.
     * If the peer is enhanced, and the replication degree for the chunk No received was achieved in the meantime, it
     * aborts the request.
     * Then, if that chunk size is larger than the available free space, the request is aborted.
     * Finally, the chunk is saved in the local storage and the peer sends the STORED message. If the chunk was already
     * saved, it still sends the STORED message.
     *
     * @param message - the received STORED message
     */
    private void handlePUTCHUNK(Message message) {
        UI.printBoot("------------- Received PUTCHUNK Message: "+message.getChunkNo()+" -----------");

        String fileId = message.getFileId();
        int chunkNo = message.getChunkNo();

        ConcurrentHashMap<String, Set<Integer>> peersWithFile = peerState.getPeersBackingUpFile();
        if(peersWithFile.containsKey(fileId)){
            UI.printWarning("Since I'm the one backing up this file, this request wil be ignored");
            UI.printBoot("------------------------------------------------------");
            return;
        }

        if(peer.isEnhanced()) {
            FileChunk fileChunk = new FileChunk(fileId, chunkNo);
            ConcurrentHashMap<FileChunk, ChunkInfo> storedChunks_ENH = peerState.getStoredChunks_ENH();

            if(storedChunks_ENH.containsKey(fileChunk)) {
                if(storedChunks_ENH.get(fileChunk).achievedDesiredRepDeg()) {
                    UI.printWarning("Replication degree is already being respected for chunk " + message.getChunkNo() + ". Ignoring further requests");
                    UI.printBoot("------------------------------------------------------");
                    return;
                }
            }
        }

        peerState.startStoringChunks(message);
        ConcurrentHashMap<String, ArrayList<Integer>> storedChunksByFileId = peerState.getStoredChunksByFileId();

        if(storedChunksByFileId.get(fileId).contains(message.getChunkNo())) {
            UI.printWarning("Chunk is already stored, sending STORED message");
        }
        else {
            if (!peerState.getStorageManager().saveChunk(message)) {
                UI.printError("Chunk " + chunkNo + " of file " + fileId + " is larger than the available space (" + peerState.getStorageManager().getAvailableSpace() + ")");
                UI.printBoot("------------------------------------------------------");
                return;
            }
            peerState.addStoredChunk(message);
        }

        Message storedMessage = new Message(peer.getVersion(), peer.getServerId(), fileId, null, Message.MessageType.STORED, chunkNo);
        peer.getMCChannel().sendWithRandomDelay(Utils.MAX_DELAY_STORED, storedMessage);

        UI.printOK("Sending STORED message: " + storedMessage.getChunkNo());
        UI.printBoot("------------------------------------------------------");
    }

    /**
     * Handles a STORED message
     * If the peer is the backup initiator peer, updates the backed up information regarding the chunk and sender.
     * Else, updates the stored information (number of peers storing/replication degree).
     *
     * @param message - the received STORED message
     */
    private void handleSTORED(Message message) {
        UI.printBoot("-------------- Received STORED Message: "+ message.getChunkNo() +" ------------");
        FileChunk fileChunk = new FileChunk(message.getFileId(), message.getChunkNo());
        peerState.updateBackedUpChunks(fileChunk, message);

        ConcurrentHashMap<String, Set<Integer>> peersWithFile = peerState.getPeersBackingUpFile();
        if(peersWithFile.containsKey(message.getFileId())){
            UI.printOK("Finished updating");
            UI.printBoot("------------------------------------------------------");
            return;
        }

        peerState.updateStoredChunks(fileChunk,message);
        UI.printOK("Finished updating");
        UI.printBoot("------------------------------------------------------");
    }

    /**
     * Handles a GETCHUNK message.
     * If a CHUNK message for this chunk is received while handling GETCHUNK, the operation is aborted.
     * is aborted.
     * If the peer does not have any CHUNK for this file or this CHUNK No, the operation is aborted.
     * Finally it loads the chunk stored in its local storage and sends the CHUNK to the channel.
     *
     * @param message - the received GETCHUNK message
     * @param address - address used for TCP connection in enhanced peers
     */
    private void handleGETCHUNK(Message message, InetAddress address) {
        UI.printBoot("------------ Received GETCHUNK Message: "+message.getChunkNo()+" ------------");

        String fileId = message.getFileId();
        int chunkNo = message.getChunkNo();
        FileChunk fileChunk = new FileChunk(fileId, chunkNo);

        ConcurrentHashMap<FileChunk, Boolean> isBeingRestoredChunkMap = peerState.getIsBeingRestoredChunkMap();
        if(isBeingRestoredChunkMap.containsKey(fileChunk)) {
            if(isBeingRestoredChunkMap.get(fileChunk)) {
                peerState.removeChunk(fileChunk);
                UI.printWarning("Chunk " + chunkNo + " is already being restored, ignoring request");
                UI.printBoot("------------------------------------------------------");
                return;
            }
        }

        ConcurrentHashMap<String, ArrayList<Integer>> storedChunksByFileId = peerState.getStoredChunksByFileId();
        if(!storedChunksByFileId.containsKey(fileId) || !storedChunksByFileId.get(fileId).contains(chunkNo)) {
            UI.printBoot("------------------------------------------------------");
            return;
        }

        Message chunk = peerState.getStorageManager().loadChunk(fileId, chunkNo);
        UI.printOK("Sending CHUNK Message: " + message.getChunkNo());
        peer.sendMessage(chunk,address);
        UI.printBoot("------------------------------------------------------");
    }

    /**
     * Handles a CHUNK message.
     * Starts by marking the chunk as being restored.
     * Then checks if the file where that chunk belongs to is really being restored, aborting if negative.
     * If the message was sent by an enhanced peer and it only contains the header, the request must be ignored. (used
     * to avoid flooding the host)
     * Finally it adds the chunk to the restored chunks. If the last chunk required was received, the peer has successfully
     * restored the file.
     *
     * @param message - the received CHUNK message
     */
    private void handleCHUNK(Message message) {
        UI.printBoot("-------------- Received CHUNK Message: "+ message.getChunkNo() +" -------------");

        String fileId = message.getFileId();
        FileChunk fileChunk = new FileChunk(fileId, message.getChunkNo());

        ConcurrentHashMap<FileChunk, Boolean> isBeingRestoredChunkMap = peerState.getIsBeingRestoredChunkMap();
        if(isBeingRestoredChunkMap.containsKey(fileChunk)) {
            peerState.setIsBeingRestored(fileChunk);
            UI.printOK("Marked chunk No " + message.getChunkNo() + " as being restored");
        }

        ConcurrentHashMap<String, ConcurrentSkipListSet<Message>> chunksByRestoredFile = peerState.getRestoredChunks();
        if(!chunksByRestoredFile.containsKey(fileId)) {
            UI.print("File is not being restored by this peer");
            UI.printBoot("------------------------------------------------------");
            return;
        }

        if(!message.getVersion().equals("1.0") && !message.hasBody()) {
            UI.print("Enhanced peer sent only header, ignoring restore request");
            UI.printBoot("------------------------------------------------------");
            return;
        }

        peerState.addRestoredFileChunks(message);

        if(peerState.hasRestoredAllChunks(fileId)) {
            peerState.saveFileToRestoredFolder(fileId);
            peerState.stopRestoringFile(fileId);
        }
        UI.printBoot("------------------------------------------------------");
    }

    /**
     * Handles a DELETE message.
     * Starts by checking if the peer is backing up the file, ignoring the request if negative.
     * Then, informs the peer to delete every chunk related to that file.
     *
     * @param message - the received DELETE message
     */
    private void handleDELETE(Message message) {
        UI.printBoot("-------------- Received DELETE Message ---------------");

        String fileId = message.getFileId();

        ConcurrentHashMap<String, ArrayList<Integer>> storedChunksByFileId = peerState.getStoredChunksByFileId();
        if(!storedChunksByFileId.containsKey(fileId)) {
            UI.printBoot("------------------------------------------------------");
            return;
        }

        ArrayList<Integer> storedChunks = storedChunksByFileId.get(fileId);
        while(!storedChunks.isEmpty()) {
            peerState.deleteChunk(fileId, storedChunks.get(0), false);
        }

        UI.printOK("File deleted successfully");

        if(!peer.isEnhanced()){
            Message messageACK_DELETE = new Message(peer.getVersion(),peer.getServerId(),fileId, null, Message.MessageType.ACK_DELETE);
            peer.getMCChannel().sendMessage(messageACK_DELETE);
            UI.printOK("Sending ACK_DELETE message");
        }

        UI.printBoot("------------------------------------------------------");
    }

    /**
     * Handles a REMOVED message.
     * If the deletion of the chunk has lead to an unsatisfiable replication degree, a new backup protocol for that
     * chunk is initiated.
     *
     * @param message - the received REMOVED message
     */
    private void handleREMOVED(Message message) {
        UI.printBoot("------------- Received REMOVE Message: "+message.getChunkNo()+" ------------");

        FileChunk fileChunk = new FileChunk(message.getFileId(), message.getChunkNo());
        ConcurrentHashMap<FileChunk, ChunkInfo> storedChunks = peerState.getStoredChunks();
        ConcurrentHashMap<FileChunk, ChunkInfo> reclaimedChunks = peerState.getChunksReclaimed();

        if(storedChunks.containsKey(fileChunk)) {
            ChunkInfo chunkInfo = storedChunks.get(fileChunk);
            chunkInfo.decreaseCurrentRepDeg();

            if(!chunkInfo.achievedDesiredRepDeg()) {
                UI.print("Replication degree of Chunk " + message.getChunkNo() + " is no longer being respected");
                Message messagePUTCHUNK = peerState.getStorageManager().loadChunk(message.getFileId(), message.getChunkNo());
                messagePUTCHUNK.setMessageType(Message.MessageType.PUTCHUNK);
                messagePUTCHUNK.setReplicationDeg(chunkInfo.getDesiredReplicationDeg());

                scheduledExecutorService.schedule( new BackupChunkInitiator(peerState, messagePUTCHUNK, peer.getMDBChannel()),
                        Utils.getRandom(0, Utils.MAX_DELAY_REMOVED), TimeUnit.MILLISECONDS);
            }
        } else if(reclaimedChunks.containsKey(fileChunk)){
            ChunkInfo chunkInfo = reclaimedChunks.get(fileChunk);

            UI.print("Replication degree of Chunk " + message.getChunkNo() + " is no longer being respected");
            Message messagePUTCHUNK = new Message(peer.getVersion(), -1, message.getFileId(), chunkInfo.getBody(),
                    Message.MessageType.PUTCHUNK, message.getChunkNo(), chunkInfo.getDesiredReplicationDeg());

            scheduledExecutorService.schedule( new BackupChunkInitiator(peerState, messagePUTCHUNK, peer.getMDBChannel()),
                    Utils.getRandom(0, Utils.MAX_DELAY_REMOVED), TimeUnit.MILLISECONDS);

            peerState.removeReclaimedChunk(fileChunk);
        }
        UI.printBoot("------------------------------------------------------");
    }

    /**
     * Handles an ACK_DELETE message.
     * Starts by checking if the peer is enhanced, aborting if otherwise.
     * Then, starts iterating through the peersBackingUpFile map. In each cycle it checks if the file was deleted.
     * If positive, checks if the sends id belongs to one of the peers that haven't send an ACK_DELETE when the file was
     * deleted, sending a DELETE message to the channel if it checks out.
     * @param message - the received CONTROL message
     */
    private void handleCONTROL(Message message){
        UI.printBoot("-------------- Received CONTROL Message ---------------");

        if(!peer.isEnhanced()){
            UI.printBoot("-------------------------------------------------------");
            return;
        }

        Set<String> deletedFiles = peerState.getDeletedFiles();
        ConcurrentHashMap<String, Set<Integer>> peersBackingUpFile = peerState.getPeersBackingUpFile();

        for (Map.Entry<String, Set<Integer>> entry : peersBackingUpFile.entrySet()) {
            String fileId = entry.getKey();
            if(deletedFiles.contains(fileId)){
                Set<Integer> peers = entry.getValue();
                if (peers.contains(message.getSenderId())) {
                    Message messageDELETE = new Message(peer.getVersion(), peer.getServerId(), fileId, null, Message.MessageType.DELETE);
                    peer.getMCChannel().sendMessage(messageDELETE);
                    UI.printOK("Sending DELETE message");
                }
            }
        }
        UI.printBoot("-------------------------------------------------------");
    }

    /**
     * Handles an ACK_DELETE message.
     * Starts by checking if the peer is enhanced, aborting if otherwise.
     * Then, checks if the peer deleted a file with that Id and didn't receive an ACK from all the peers that were
     * backing it up.
     * Finally, checks if the message belongs to one of the peers that were missing the ACK, removing it if positive.
     * @param message - the received ACK_DELETE message
     */
    private void handleACK_DELETE(Message message){
        UI.printBoot("------------- Received ACK_DELETE Message ------------");

        if(!peer.isEnhanced()){
            UI.printBoot("------------------------------------------------------");
            return;
        }

        String fileId = message.getFileId();
        Set<String> deletedFiles = peerState.getDeletedFiles();
        if(!deletedFiles.contains(fileId)){
            UI.printBoot("-------------------------------------------------------");
            return;
        }

        ConcurrentHashMap<String, Set<Integer>> peersBackingUpFile = peerState.getPeersBackingUpFile();
        if(peersBackingUpFile.containsKey(fileId)){
            peerState.removePeerBackingUpFile(fileId, message.getSenderId());
        }
        UI.printBoot("------------------------------------------------------");
    }
}
