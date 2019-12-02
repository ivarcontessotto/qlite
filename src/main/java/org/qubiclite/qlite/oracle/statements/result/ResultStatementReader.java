package org.qubiclite.qlite.oracle.statements.result;

import org.qubiclite.qlite.iam.IAMReader;
import org.iota.jota.model.Transaction;
import org.qubiclite.qlite.oracle.statements.StatementReader;
import org.qubiclite.qlite.oracle.statements.StatementType;
import org.qubiclite.qlite.oracle.statements.hash.HashStatementReader;

import java.util.List;

public class ResultStatementReader extends StatementReader {

    private HashStatementReader hashStatementReader;

    public ResultStatementReader(IAMReader generalReader, HashStatementReader hashStatementReader) {
        super(generalReader, StatementType.RESULT_STATEMENT);
        this.hashStatementReader = hashStatementReader;

        if(hashStatementReader == null)
            throw new NullPointerException("parameter 'hashStatementReader' is null");
    }

    public ResultStatement read(int epoch) {
        return read(null, epoch);
    }

    @Override
    public ResultStatement read(List<Transaction> preload, int epoch) {
        ResultStatement resultStatement = (ResultStatement)super.read(preload, epoch);
        if(resultStatement != null)
            resultStatement.setHashStatement(hashStatementReader.read(epoch));
        return resultStatement;
    }
}