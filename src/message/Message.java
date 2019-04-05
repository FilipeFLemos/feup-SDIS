package message;

import java.io.*;
import java.util.Arrays;


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
     * Creates a Message from the DatagramPacket's data received.
     * @param data - the received data.
     */
    public Message(byte[] data) {
        String header = extractHeader(data);
        parseHeader(header);

        int nBytes = data.length - header.length() - 4;
        body = new byte[nBytes];
        ByteArrayInputStream message = new ByteArrayInputStream(data, header.length() + 4, nBytes);
        message.read(body, 0, nBytes);
    }

    private String extractHeader(byte[] data) {
        ByteArrayInputStream stream = new ByteArrayInputStream(data);
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));

        String header = "";

        try {
            header = reader.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return header;
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
    public void parseHeader(String str) {
        String[] header = str.split("\\s+");
        System.out.println(str);
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

    /**
     * Retrieves the message packet (header + body).
     * @param sendBody - if the body should be added or not to the packet (RESTORE ENHANCEMENT)
     * @return - the message packet
     */
    public byte[] getPacket(boolean sendBody) {
        String header = buildHeader();

        byte[] headerBytes = header.getBytes();

        int bodyLength = 0;
        if(body != null && sendBody) {
            bodyLength = body.length;
        }

        byte[] packet = new byte[headerBytes.length + bodyLength];
        System.arraycopy(headerBytes, 0, packet, 0, headerBytes.length);

        if(bodyLength != 0) {
            System.arraycopy(this.body, 0, packet, header.length(), bodyLength);
        }

        return packet;
    }

    /**
     * Generates the message header.
     * @return the message header.
     */
    private String buildHeader() {
        String header = "";

        switch(messageType){
            case PUTCHUNK:
                header += "PUTCHUNK ";
                break;
            case STORED:
                header += "STORED ";
                break;
            case GETCHUNK:
                header += "GETCHUNK ";
                break;
            case CHUNK:
                header += "CHUNK ";
                break;
            case DELETE:
                header += "DELETE ";
                break;
            case REMOVED:
                header += "REMOVED ";
                break;
            case CONTROL:
                header += "CONTROL ";
                break;
            case ACK_DELETE:
                header += "ACK_DELETE ";
                break;
            default:
                header += "NOT_VALID";
                break;
        }

        header += version + " " + senderId + " ";

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

    public boolean hasBody() {
        return (body != null && body.length > 0);
    }

    public String getVersion() {
        return version;
    }

    public Integer getSenderId() {
        return senderId;
    }

    public String getFileId() {
        return fileId;
    }

    public Integer getChunkNo() {
        return chunkNo;
    }

    public Integer getReplicationDeg() {
        return replicationDeg;
    }

    public byte[] getBody() {
        return body;
    }

    public MessageType getMessageType() {
        return messageType;
    }

    public void setMessageType(MessageType messageType) {
        this.messageType = messageType;
    }

    public void setReplicationDeg(int replicationDeg) {
        this.replicationDeg = replicationDeg;
    }

    @Override
    public String toString() {
        String message = buildHeader();
        if(body != null) {
            message += new String(this.body);
        }

        return message;
    }

    @Override
    public int compareTo(Object obj) {
        Message other = (Message) obj;
        return this.chunkNo - other.getChunkNo();
    }
}
