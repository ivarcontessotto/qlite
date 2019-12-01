package org.qubiclite.qlite.oracle.statements;

import org.qubiclite.qlite.exceptions.InvalidStatementException;
import org.qubiclite.qlite.iam.IAMKeywordReader;
import org.qubiclite.qlite.iam.IAMReader;
import org.iota.jota.model.Transaction;
import org.qubiclite.qlite.oracle.statements.hash.HashStatement;
import org.qubiclite.qlite.oracle.statements.result.ResultStatement;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;

public class StatementReader {

    private final StatementType statementType;
    private final IAMKeywordReader reader;
    private final HashMap<Integer, Statement> knownStatementsByEpoch = new HashMap<>();

    public StatementReader(IAMReader generalReader, StatementType statementType) {
        reader = new IAMKeywordReader(generalReader, statementType.getIAMKeyword());
        this.statementType = statementType;
    }

    public Statement read(List<Transaction> preload, int epoch) {

        if(knownStatementsByEpoch.containsKey(epoch))
            return knownStatementsByEpoch.get(epoch);

        // read JSONObject from tangle stream
        JSONObject jsonObject = preload != null ? reader.readFromSelection(epoch, preload) : reader.read(epoch);

        if(jsonObject == null)
            return null;

        Statement statement;

        try {
            statement = buildStatementFromJSON(jsonObject);
        } catch (InvalidStatementException e) {
            return null;
        }

        knownStatementsByEpoch.put(epoch, statement);
        return statement;
    }

    private Statement buildStatementFromJSON(JSONObject jsonObject) {
        switch (statementType) {
            case HASH_STATEMENT:
                return HashStatement.fromJSON(jsonObject);
            case RESULT_STATEMENT:
                return ResultStatement.fromJSON(jsonObject);
            default:
                throw new IllegalStateException("unknown statement type: " + statementType.name());
        }
    }
}