package org.qubiclite.qlite.iam.signing;

import org.junit.Test;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;


public class SignerTest {

    @Test(expected = InvalidKeySpecException.class)
    public void loadKeysFromTrytes() throws InvalidKeySpecException, NoSuchAlgorithmException {
        Signer signer = new Signer();
        signer.loadKeysFromTrytes("A", "B");
    }
}