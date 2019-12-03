package org.qubiclite.qlite.oracle.input;

import java.util.Queue;

/**
 * Reference implementation of the OracleInputProvider interface.
 * Can also be used for quick testing.
 */
public class QueueInputProvider implements OracleInputProvider {

    private QueueInputProviderConfig config;
    private Queue<String> inputSequence;

    public QueueInputProvider(QueueInputProviderConfig config) {
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
        return this.inputSequence.poll();
    }
}
