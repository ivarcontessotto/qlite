package org.qubiclite.qlite.oracle.statements.result;

import org.qubiclite.qlite.oracle.statements.StatementIAMIndex;
import org.qubiclite.qlite.oracle.statements.StatementType;

public class ResultStatementIAMIndex extends StatementIAMIndex {

    public ResultStatementIAMIndex(long position) {
        super(StatementType.RESULT_STATEMENT, position);
    }
}