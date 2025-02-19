package org.qubiclite.qlite.oracle.statements.result;

import org.qubiclite.qlite.constants.TangleJSONConstants;
import org.qubiclite.qlite.exceptions.InvalidStatementException;
import org.qubiclite.qlite.oracle.statements.Statement;
import org.qubiclite.qlite.oracle.statements.hash.HashStatement;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import org.qubiclite.qlite.oracle.ResultHasher;
import org.qubiclite.qlite.tangle.TryteTool;

/**
 * @author microhash
 *
 * During each epoch the oracle publishes a HashStatement followed by a ResultStatement.
 * The ResultStatements contains the result calculated by the oracle for the particular epoch.
 * @see Statement
 * @see HashStatement
 * */
public class ResultStatement extends Statement {

    private static final String CONTENT_TYPE = "result";
    private final Logger logger;

    private String nonce;
    private final String result;
    private HashStatement hashEpoch;

    /**
     * Creates a new ResultStatement from a JSONObject
     * @param jsonObject the JSONObject describing the ResultStatement
     * @return new ResultStatement created from the JSONObject
     * */
    public static ResultStatement fromJSON(JSONObject jsonObject) throws InvalidStatementException {

        if(jsonObject == null)
            throw new NullPointerException("parameter 'jsonObject' is null");

        try {
            return tryToParseFromJSON(jsonObject);
        } catch (JSONException e) {
            throw new InvalidStatementException(e.getClass().getName() + ": " + e.getMessage(), e);
        }
    }

    private static ResultStatement tryToParseFromJSON(JSONObject jsonObject) {
        int epochIndex = jsonObject.getInt(TangleJSONConstants.STATEMENT_EPOCH_INDEX);
        String result = jsonObject.getString(TangleJSONConstants.RESULT_STATEMENT_RESULT);
        String nonce = jsonObject.getString(TangleJSONConstants.RESULT_STATEMENT_NONCE);
        ResultStatement parsed = new ResultStatement(epochIndex, result);
        parsed.nonce = nonce;
        return parsed;
    }

    /**
     * @param epochIndex index of epoch in which this statement occured
     * @param result     result the oracle calculated for this particular epoch
     * */
    public ResultStatement(int epochIndex, String result) {
        super(epochIndex);
        this.result = result;
        this.nonce = genNonce();
        this.logger = LogManager.getLogger(ResultStatement.class);
    }

    private static String genNonce() {
        return TryteTool.generateRandom(30);
    }

    @Override
    public String getContent() {
        return result;
    }

    @Override
    public String getContentType() {
        return CONTENT_TYPE;
    }

    public void setHashStatement(HashStatement hashEpoch) {
        this.hashEpoch = hashEpoch;
    }

    /**
     * Checks whether the associated HashStatement has been set (this requires it to be
     * published in time) and contains the correct hash.
     * */
    public boolean isHashStatementValid() {
        String reHashedResult = ResultHasher.hash(this);
        logger.debug("Comparing hash statement content to re-hashed result: " + hashEpoch.getContent() + " <-> " + reHashedResult);
        return hashEpoch != null && hashEpoch.getContent().equals(reHashedResult);
    }

    @Override
    public JSONObject toJSON() {
        JSONObject o = super.toJSON();
        o.put(TangleJSONConstants.RESULT_STATEMENT_NONCE, nonce);
        return o;
    }

    public String getNonce() {
        return nonce;
    }

    public HashStatement getHashStatement() {
        return hashEpoch;
    }
}