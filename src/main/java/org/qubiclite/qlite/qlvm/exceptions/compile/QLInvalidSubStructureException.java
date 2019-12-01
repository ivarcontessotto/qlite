package org.qubiclite.qlite.qlvm.exceptions.compile;

import org.qubiclite.qlite.qlvm.exceptions.compile.QLCompilerException;

public class QLInvalidSubStructureException extends QLCompilerException {

    public QLInvalidSubStructureException(String msg) {
        super(msg);
    }

    @Override
    public String getMessage() {
        return "invalid brackets/braces syntax";
    }
}