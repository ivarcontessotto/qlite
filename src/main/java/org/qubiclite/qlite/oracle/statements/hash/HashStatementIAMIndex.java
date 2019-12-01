package org.qubiclite.qlite.oracle.statements.hash;

import org.qubiclite.qlite.oracle.statements.StatementIAMIndex;
import org.qubiclite.qlite.oracle.statements.StatementType;

public class HashStatementIAMIndex extends StatementIAMIndex {

    public HashStatementIAMIndex(long position) {
        super(StatementType.HASH_STATEMENT, position);
    }
}