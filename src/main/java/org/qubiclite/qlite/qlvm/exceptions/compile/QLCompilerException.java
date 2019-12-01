package org.qubiclite.qlite.qlvm.exceptions.compile;

import org.qubiclite.qlite.qlvm.exceptions.runtime.QLException;

public class QLCompilerException extends QLException {
    public QLCompilerException(String msg) {
        super(msg);
    }
}
