package org.qubiclite.qlite.oracle.statements.hash;

import org.qubiclite.qlite.iam.IAMWriter;
import org.qubiclite.qlite.oracle.statements.StatementType;
import org.qubiclite.qlite.oracle.statements.StatementWriter;

public class HastStatementWriter extends StatementWriter<HashStatement> {

    public HastStatementWriter(IAMWriter generalWriter) {
        super(generalWriter, StatementType.HASH_STATEMENT);
    }
}
