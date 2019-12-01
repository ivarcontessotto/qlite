package org.qubiclite.qlite.oracle.statements.hash;

import org.qubiclite.qlite.iam.IAMReader;
import org.iota.jota.model.Transaction;
import org.qubiclite.qlite.oracle.statements.StatementReader;
import org.qubiclite.qlite.oracle.statements.StatementType;

import java.util.List;

public class HashStatementReader extends StatementReader {

    public HashStatementReader(IAMReader generalReader) {
        super(generalReader, StatementType.HASH_STATEMENT);
    }

    public HashStatement read(int epoch) {
        return read(null, epoch);
    }

    @Override
    public HashStatement read(List<Transaction> preload, int epoch) {
        return (HashStatement) super.read(preload, epoch);
    }
}