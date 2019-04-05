package message;

import java.io.Serializable;
import user_interface.UI;


public class Message implements Comparable, Serializable {

    private static final long serialVersionUID = 1L;

    public enum MessageType {
        PUTCHUNK,
        STORED,
        GETCHUNK,
        CHUNK,
        DELETE,
        REMOVED,
        CONTROL,
        ACK_DELETE
    }

    private MessageType messageType;
    private String version;
    private Integer senderId;
    private String fileId = null;
    private Integer chunkNo = null;
    private Integer replicationDeg = null;
    private byte[] body;

    /**
     * Processes a message given as a byte[] (directly from a DatagramPacket)
     *
     * @param message the message
     * @param size    the message size
     */
    public Message(byte[] message, int size) {
        int headerLength = 0;
        for (int i = 0; i < message.length; ++i) {
            if((char)message[i] == '\r' && (char)message[i+1] == '\n') {
                headerLength = i+4;
                break;
            }
        }

        String header = new String(message, 0, headerLength);
        processHeader(header);

        int bodyLength = size - headerLength;
        this.body = new byte[bodyLength];
        System.arraycopy(message, headerLength, this.body, 0, bodyLength);
    }


    /**
     * Constructor to create CONTROL messages
     * @param version
     * @param senderId
     * @param body
     * @param messageType
     */
    public Message(String version, Integer senderId, byte[] body, MessageType messageType){
        this.version = version;
        this.senderId = senderId;
        this.body = body;
        this.messageType = messageType;
    }

    /**
     * Constructor to create DELETED messages
     * @param version
     * @param senderId
     * @param fileId
     * @param body
     * @param messageType
     */
    public Message(String version, Integer senderId, String fileId, byte[] body, MessageType messageType) {
        this(version,senderId,body,messageType);
        this.fileId = fileId;
    }

    /**
     * Constructor to create CHUNK, GETCHUNK, REMOVED and STORED messages
     * @param version
     * @param senderId
     * @param fileId
     * @param body
     * @param messageType
     * @param chunkIndex
     */
    public Message(String version, Integer senderId, String fileId, byte[] body, MessageType messageType, int chunkIndex) {
        this(version, senderId, fileId, body, messageType);
        this.chunkNo = chunkIndex;
    }

    /**
     * Contructor to create PUTCHUNK Messages
     * @param version
     * @param senderId
     * @param fileId
     * @param body
     * @param messageType
     * @param chunkIndex
     * @param replicationDeg
     */
    public Message(String version, Integer senderId, String fileId, byte[] body, MessageType messageType, int chunkIndex, int replicationDeg) {
        this(version, senderId, fileId, body, messageType, chunkIndex);
        this.replicationDeg = replicationDeg;
    }

    /**
     * Process a message header given as a string.
     *
     * @param str the header
     */
    public void processHeader(String str) {
        String[] header = str.split("\\s+");
        switch(header[0]) {
            case "PUTCHUNK":
                this.messageType = MessageType.PUTCHUNK;
                this.replicationDeg = Integer.parseInt(header[5]);
                break;
            case "STORED":
                this.messageType = MessageType.STORED;
                break;
            case "GETCHUNK":
                this.messageType = MessageType.GETCHUNK;
                break;
            case "CHUNK":
                this.messageType = MessageType.CHUNK;
                break;
            case "DELETE":
                this.messageType = MessageType.DELETE;
                break;
            case "REMOVED":
                this.messageType = MessageType.REMOVED;
                break;
            case "CONTROL":
                this.messageType = MessageType.CONTROL;
                break;
            case "ACK_DELETE":
                this.messageType = MessageType.ACK_DELETE;
                break;
            default:
                break;
        }

        this.version = header[1];
        this.senderId = Integer.parseInt(header[2]);
        if(header.length > 3) {
            this.fileId = header[3];
        }
        if(header.length > 4) {
            this.chunkNo = Integer.parseInt(header[4]);
        }
    }

    public byte[] buildMessagePacket(boolean sendBody) {
        String header = buildHeader();

        byte[] headerBytes = header.getBytes();

        int bodyLength;
        if(body == null || !sendBody) {
            bodyLength = 0;
        } else {
            bodyLength = body.length;
        }

        byte[] packet = new byte[headerBytes.length + bodyLength];
        System.arraycopy(headerBytes, 0, packet, 0, headerBytes.length);

        if(bodyLength != 0)
            System.arraycopy(this.body, 0, packet, header.length(), bodyLength);

        return packet;
    }

    private String buildHeader() {
        String header = parseType() + version + " " + senderId + " ";

        if (this.fileId != null){
            header += fileId + " ";
        }

        if (this.chunkNo != null) {
            header += chunkNo + " ";
        }
        if (this.replicationDeg != null) {
            header += replicationDeg + " ";
        }

        String CRLF = "\r\n";

        header += CRLF + CRLF;
        return header;
    }

    @Override
    public String toString() {
        String message = buildHeader();
        if(body != null) {
            message += new String(this.body);
        }

        return message;
    }

    private String parseType() {
        switch(this.messageType){
            case PUTCHUNK:
                return "PUTCHUNK ";
            case STORED:
                return "STORED ";
            case GETCHUNK:
                return "GETCHUNK ";
            case CHUNK:
                return "CHUNK ";
            case DELETE:
                return "DELETE ";
            case REMOVED:
                return "REMOVED ";
            case CONTROL:
                return "CONTROL ";
            case ACK_DELETE:
                return "ACK_DELETE ";
            default:
                break;
        }

        return "NOT VALID";
    }

    /**
     * Gets the message messageType.
     *
     * @return the messageType
     */
    public MessageType getMessageType() {
        return messageType;
    }

    /**
     * Gets the message version.
     *
     * @return the version
     */
    public String getVersion() {
        return version;
    }

    /**
     * Gets sender peer id.
     *
     * @return the peer id
     */
    public Integer getSenderId() {
        return senderId;
    }

    /**
     * Gets file id.
     *
     * @return the file id
     */
    public String getFileId() {
        return fileId;
    }

    /**
     * Gets chunk index.
     *
     * @return the chunk index
     */
    public Integer getChunkNo() {
        return chunkNo;
    }

    /**
     * Gets rep degree.
     *
     * @return the rep degree
     */
    public Integer getReplicationDeg() {
        return replicationDeg;
    }

    /**
     * Get body byte [ ].
     *
     * @return the byte [ ]
     */
    public byte[] getBody() {
        return body;
    }

    /**
      * Sets the message messageType
      * @param messageType new messageType
      */
    public void setMessageType(MessageType messageType) {
        this.messageType = messageType;
    }

    /**
      * Sets the replication degree (used for backup)
      * @param degree new rep degree
      */
    public void setReplicationDeg(int degree) {
        this.replicationDeg = degree;
    }

    /**
     * Checks if message body has body
     * @return true if body size is greater than 0
     */
    public boolean hasBody() {
        return body.length > 0;
    }

    /**
      * Compares Message objects by the chunkNo
      * @param o object to compare to
      * @return difference between chunk numbers
      */
    @Override
    public int compareTo(Object o) {
        return this.chunkNo - ((Message) o).getChunkNo();
    }
}
