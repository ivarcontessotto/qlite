package org.qubiclite.qlite.iam.exceptions;

public class CorruptIAMStreamException extends RuntimeException {

    public CorruptIAMStreamException(String message, Throwable t) {
        super(message, t);
    }
}
