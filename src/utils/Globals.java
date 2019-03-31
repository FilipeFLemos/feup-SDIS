package utils;

public class Globals {

    public static final int MAX_TCP_SOCKET_THREADS = 50;
    /**
     * A chunk's max body size
     */
    public static int MAX_CHUNK_SIZE = 64000;

    /**
     * Maximum time waiting before processing a STORED message
     */
    public static int MAX_STORED_WAITING_TIME = 400;
    /**
     * Maximum time waiting before processing a CHUNK message
     */
    public static int MAX_CHUNK_WAITING_TIME = 400;

    /**
     * Maximum time waiting before processing a REMOVED message
     */
    public static int MAX_REMOVED_WAITING_TIME = 400;

    /**
     * A peer's maximum storage space
     */
    public static long MAX_PEER_STORAGE = 8*10^9;

    /**
     * Numbers of tries on the backup protocl
     */
    public static int MAX_PUTCHUNK_TRIES = 5;

    /**
     * Maximum time waiting before processing a PUTCHUNK message - for backup enhancement
     */
    public static int MAX_BACKUP_ENH_WAIT_TIME = 750;

    /**
     * Time between each peer information local saving (in seconds)
     */
    public static int PEER_CONTROLLER_SAVING_RATE = 3;

    /**
     * Directory where this host's peer files are saved
     */
    public static String PEER_FILESYSTEM_DIR = "peers";
}
