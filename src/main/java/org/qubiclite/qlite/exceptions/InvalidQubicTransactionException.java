package org.qubiclite.qlite.exceptions;

public class InvalidQubicTransactionException extends RuntimeException {

    public InvalidQubicTransactionException(String error, Throwable cause) {
        super(error, cause);
    }
}
