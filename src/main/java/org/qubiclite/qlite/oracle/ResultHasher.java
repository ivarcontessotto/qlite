package org.qubiclite.qlite.oracle;

import org.qubiclite.qlite.oracle.statements.result.ResultStatement;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.util.encoders.Hex;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class ResultHasher {

    private static MessageDigest digest;
    private final static Logger LOGGER = LogManager.getLogger(ResultHasher.class);

    static {
        try {
            digest = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    /**
     * Creates hash for HashStatement.
     **/
    public static String hash(ResultStatement resultStatement) {
        String nonced = resultStatement.getNonce()+resultStatement.getContent();
        byte[] noncedBytes = nonced.getBytes(StandardCharsets.US_ASCII);
        LOGGER.debug("nonced bytes: " + new String(noncedBytes));
        byte[] hash = digest.digest(noncedBytes);
        LOGGER.debug("hash bytes: " + new String(hash));
        String hexEncodedHash = new String(Hex.encode(hash));
        return hexEncodedHash;
    }
}