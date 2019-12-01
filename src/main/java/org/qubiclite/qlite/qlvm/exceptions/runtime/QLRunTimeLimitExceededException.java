package org.qubiclite.qlite.qlvm.exceptions.runtime;

public class QLRunTimeLimitExceededException extends QLRunTimeException {

    public QLRunTimeLimitExceededException() {
        super("qlvm run time limit (defined in qubic transaction) exceeded. abort program.");
    }
}
