package iam;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;

public abstract class IAMStream {

    public static final int MAX_FRAGMENTS_PER_IAM_PACKET = 5;

    /**
     * Builds the stream's address for a specific index.
     * @param index the IAM index for which the address shall be built
     * @return the address built
     * */
    public String buildAddress(IAMIndex index) {
        return cutStreamID() + index + StringUtils.repeat('9', 9);
    }

    private String cutStreamID() {
        return StringUtils.substring(getID(), 0, IAMIndex.STREAM_HANDLE_LENGTH);
    }

    /**
     * @param index   index position to which the message is attached in the IAM stream
     * @param message the custom message of the IAM packet.
     * @return string that is required to be signed in the signature field of an IAM packet
     * */
    String buildStringToSignForIAMPacket(IAMIndex index, JSONObject message) {
        return buildAddress(index) + String.valueOf(message);
    }

    /**
     * @return IAMStream ID = transaction hash of public key attachment
     * */
    public abstract String getID();
}
