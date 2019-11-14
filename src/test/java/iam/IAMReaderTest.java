package iam;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import tangle.TryteTool;


import javax.swing.*;

import static org.junit.Assert.*;

public class IAMReaderTest {

    private final IAMWriter iamWriter = new IAMWriter(TryteTool.TEST_ADDRESS_2);
    private final IAMReader iamReader = new IAMReader(iamWriter.getID());

    @Test
    public void testPublishAndRead() throws InterruptedException {
        testPublishAndReadObject(0, null);
        testPublishAndReadObject(1, "");
        testPublishAndReadObject(2, 4);
        testPublishAndReadObject(3, new JSONArray("[{'age': 20}, 'some string']"));
        testPublishAndReadObject(4, StringUtils.repeat("ok!\"9\'$3", 200));
    }

    private void testPublishAndReadObject(int position, Object object) throws InterruptedException {
        IAMIndex index = new IAMIndex(position);
        JSONObject sent = new JSONObject().put("object", object);
        String hash = iamWriter.write(index, sent);
        Thread.sleep(1000);
        JSONObject read = iamReader.read(index);
        assertEquals("hash of failed iam package: " + hash, String.valueOf(sent), String.valueOf(read));
    }

    // Can't have multiple packets per index that have different values.
    @Test
    public void testMultipleIAMPacketConflicts() throws InterruptedException {
        JSONObject message1 = new JSONObject().put("planet", "mars");
        JSONObject message2 = new JSONObject().put("planet", "venus");

        iamWriter.write(new IAMIndex(100), message1);
        Thread.sleep(1000);
        assertNotNull(iamReader.read(new IAMIndex(100)));

        iamWriter.write(new IAMIndex(100), message1);
        Thread.sleep(1000);
        assertNotNull(iamReader.read(new IAMIndex(100)));

        iamWriter.write(new IAMIndex(100), message2);
        Thread.sleep(1000);
        assertNull(iamReader.read(new IAMIndex(100)));
    }
}