package utils;

import java.nio.charset.StandardCharsets;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import user_interface.UI;

public class Utils {


    public static final int MAX_THREADS = 50;
    public static int MAX_CHUNK_SIZE = 64000;
    public static int MAX_PUTCHUNK_TRIES = 5;
    public static int MAX_DELAY_STORED = 400;
    public static int MAX_DELAY_CHUNK = 400;
    public static int MAX_DELAY_REMOVED = 400;
    public static int MAX_DELAY_BACKUP_ENH = 1000;
    public static long MAX_STORAGE_SPACE = (long) (8*Math.pow(10,9));
    public static int SAVING_INTERVAL = 3;

    private final static char[] hex = "0123456789ABCDEF".toCharArray();

    /**
     * Parses the accessPoint to retrieve the host, address and port
     * @param accessPoint - the provided peer access point
     * @return a string with the host, address and port
     */
    public static String[] parseRMI(String accessPoint, boolean isTestApp) {
        Pattern rmiPattern;
        if (!isTestApp) {
            rmiPattern = Pattern.compile("//([\\w.]+)(?::(\\d+))?/(\\w+)?");
        } else {
            rmiPattern = Pattern.compile("//([\\w.]+)(?::(\\d+))?/(\\w+)");
        }
        Matcher m = rmiPattern.matcher(accessPoint);
        String[] peer_ap = null;

        if (m.find()) {
            peer_ap = new String[]{m.group(1), m.group(2), m.group(3)};
        } else {
            UI.print("Invalid Access Point!");
        }

        return peer_ap;
    }

    /**
     * Generates a SHA256 hash for the provided file path
     *
     * @param filePath - the file path
     * @return - hashed fileId
     */
    public static String getFileID(String filePath) {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            UI.printError("Error hashing filepath name");
            return null;
        }

        return bytesToHex(digest.digest(filePath.getBytes(StandardCharsets.UTF_8)));
    }

    /**
     * Converts bytes into hex chars
     * @param bytes - the hashed bytes
     * @return the hex chars
     */
    private static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hex[v >>> 4];
            hexChars[j * 2 + 1] = hex[v & 0x0F];
        }
        return new String(hexChars);
    }

    /**
     * Generates a random number.
     *
     * @param min the min - the minimum number
     * @param max the max - the maximum number
     * @return the random number between them
     */
    public static int getRandom(int min, int max) {
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }
}
