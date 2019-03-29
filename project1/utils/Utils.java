package utils;

import javax.xml.bind.DatatypeConverter;
import java.io.File;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Utility functions
 */
public class Utils {

    /**
     * Generates a SHA256 based hash given a file pathname (after also retrieving some of its metadata)
     *
     * @param file file pathname
     * @return hashed fileID
     */
    public static final String getFileID(File file) {
        String fileUniqueStr = file.lastModified() + file.getName();

        MessageDigest sha_256 = null;
        try {
            sha_256 = MessageDigest.getInstance("SHA-256");
        }
        catch(NoSuchAlgorithmException e) {
            System.out.println("SHA-256 not found");
            System.exit(-1);
        }
        byte[] hashed = sha_256.digest(fileUniqueStr.getBytes());

        return DatatypeConverter.printHexBinary(hashed);
    }

    /**
     * Gets random time up to a maximum value.
     *
     * @param max the max
     * @return the random time
     */
    public static final int getRandomTime(int max) {
        return ThreadLocalRandom.current().nextInt(max+1);
    }

    /**
     * Gets a random number in a specific range.
     *
     * @param min the min
     * @param max the max
     * @return the random between them
     */
    public static final int getRandomBetween(int min, int max) {
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }
}
