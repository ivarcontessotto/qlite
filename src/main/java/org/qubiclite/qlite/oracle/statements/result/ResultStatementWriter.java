package org.qubiclite.qlite.oracle.statements.result;

import org.qubiclite.qlite.iam.IAMWriter;
import org.qubiclite.qlite.oracle.statements.StatementType;
import org.qubiclite.qlite.oracle.statements.StatementWriter;

public class ResultStatementWriter extends StatementWriter<ResultStatement> {

    public ResultStatementWriter(IAMWriter generalWriter) {
        super(generalWriter, StatementType.RESULT_STATEMENT);
    }
}
