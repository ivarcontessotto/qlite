package iam;

import constants.TangleJSONConstants;
import org.json.JSONObject;
import tangle.TryteTool;

import org.junit.Test;
import static org.junit.Assert.*;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.InvalidParameterException;

public class IAMWriterTest {

    private final Method predictAmountOfFragmentsMethod;
    private final int MAX_CHARS_PER_FRAGMENT;
    private final int MIN_CHARS_OF_HASHBLOCK;

    private final IAMWriter iamWriter = new IAMWriter();

    // === CONSTRUCTION ===

    public IAMWriterTest() throws NoSuchMethodException, IllegalAccessException, NoSuchFieldException {
        this.predictAmountOfFragmentsMethod = getPredictAmountOfFragmentsMethod();
        this.MAX_CHARS_PER_FRAGMENT = getMaxCharsPerFragment();
        this.MIN_CHARS_OF_HASHBLOCK = getMinCharsOfHashBlock();
    }

    private static Method getPredictAmountOfFragmentsMethod() throws NoSuchMethodException {
        Method predictAmountOfFragments = IAMWriter.class.getDeclaredMethod("predictAmountOfFragments", Integer.TYPE, Integer.TYPE);
        predictAmountOfFragments.setAccessible(true);
        return predictAmountOfFragments;
    }

    private static int getMaxCharsPerFragment() throws NoSuchFieldException, IllegalAccessException {
        Field maxCharsPerFragmentField = IAMWriter.class.getDeclaredField("MAX_CHARS_PER_FRAGMENT");
        maxCharsPerFragmentField.setAccessible(true);
        return maxCharsPerFragmentField.getInt(null);
    }

    private static int getMinCharsOfHashBlock() {
        JSONObject hashBlock = new JSONObject();
        hashBlock.put(TangleJSONConstants.IAM_PACKET_HASHBLOCK, "");
        return hashBlock.toString().length();
    }

    // === testPredictAmountOfFragments() ===

    @Test
    public void testPredictAmountOfFragments() {
        for(int i = 0; i < 10; i++)
            testCriticalThresholdForPredictAmountOfFragments(i);
    }

    private void testCriticalThresholdForPredictAmountOfFragments(int subThresholdExpectedAmountOfFragments) {
        int subThresholdContentLength = determineMaxTotalMessageLength(subThresholdExpectedAmountOfFragments);
        testSinglePredictAmountOfFragmentValuePair(subThresholdContentLength, subThresholdExpectedAmountOfFragments);
        testSinglePredictAmountOfFragmentValuePair(subThresholdContentLength+1, subThresholdExpectedAmountOfFragments+1);
    }

    private int determineMaxTotalMessageLength(int fragments) {
        final int hashBlockLength = determineHashBlockLength(fragments);
        final int messageLength = MAX_CHARS_PER_FRAGMENT*fragments;
        return messageLength-hashBlockLength;
    }

    private int determineHashBlockLength(int fragments) {
        return MIN_CHARS_OF_HASHBLOCK + Math.max(0, TryteTool.TRYTES_PER_HASH*(fragments-1));
    }

    private void testSinglePredictAmountOfFragmentValuePair(int contentLength, int expectedAmountOfFragments) {
        String message = "content length: " + contentLength;
        int actual = predictAmountOfFragments(contentLength);
        assertEquals(message, expectedAmountOfFragments, actual);
    }

    private int predictAmountOfFragments(int contentLength) {
        try {
            return (int)predictAmountOfFragmentsMethod.invoke(null, contentLength, MIN_CHARS_OF_HASHBLOCK);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    // === test testNonAsciiMessage ===

    @Test(expected = InvalidParameterException.class)
    public void testNonAsciiMessage() {
        JSONObject message = new JSONObject();
        message.put("no ascii", "ä");
        iamWriter.write(new IAMIndex(0), message);
    }
}