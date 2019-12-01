package org.qubiclite.qlite.oracle.statements;

import org.qubiclite.qlite.iam.IAMKeywordWriter;
import org.qubiclite.qlite.iam.IAMWriter;

public class StatementWriter<T extends Statement> {

    private final IAMKeywordWriter writer;

    public StatementWriter(IAMWriter generalWriter, StatementType statementType) {
        writer = new IAMKeywordWriter(generalWriter, statementType.getIAMKeyword());
    }

    public void write(T statement) {
        writer.publish(statement.getEpochIndex(), statement.toJSON());
    }
}