package org.qubiclite.qlite.qlvm.exceptions.runtime;

public abstract class QLRunTimeException extends QLException {

    protected QLRunTimeException(String msg) {
        super(msg);
    }
}