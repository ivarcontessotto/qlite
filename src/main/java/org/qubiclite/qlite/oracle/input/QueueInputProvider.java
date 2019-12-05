package org.qubiclite.qlite.oracle.input;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Queue;

/**
 * Reference implementation of the OracleInputProvider interface.
 * Can also be used for quick testing.
 */
public class QueueInputProvider implements OracleInputProvider {

    private QueueInputConfig config;
    private Queue<String> inputSequence;
    private final Logger logger = LogManager.getLogger(QueueInputProvider.class);

    public QueueInputProvider(QueueInputConfig config) {
        this.config = config;
        this.inputSequence = config.getInputSequence();
    }

    @Override
    public OracleInputConfig getConfig() {
        return this.config;
    }

    @Override
    public String getInput() {
        if (this.inputSequence.size() == 0) {
            this.inputSequence = this.config.getInputSequence();
        }
        String latestInput = this.inputSequence.poll();
        this.logger.info("getInput: " + latestInput);
        return latestInput;
    }
}
