package org.qubiclite.qlite.iam.exceptions;

import org.qubiclite.qlite.iam.IAMStream;

public class IAMPacketSizeLimitExceeded extends RuntimeException {

    public IAMPacketSizeLimitExceeded() {
        super("failed to publish IAM packet because content did not fit into " + IAMStream.MAX_FRAGMENTS_PER_IAM_PACKET + " fragments");
    }
}