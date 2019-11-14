package oracle;

import constants.TangleJSONConstants;
import iam.IAMIndex;
import iam.IAMWriter;
import oracle.statements.*;
import oracle.statements.hash.HashStatement;
import oracle.statements.hash.HashStatementIAMIndex;
import oracle.statements.hash.HastStatementWriter;
import oracle.statements.result.ResultStatement;
import oracle.statements.result.ResultStatementIAMIndex;
import oracle.statements.result.ResultStatementWriter;
import org.json.JSONException;
import qlvm.QLVM;
import org.json.JSONObject;
import qubic.QubicReader;
import tangle.TangleAPI;
import tangle.TryteTool;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;

public class OracleWriter {

    private final Path argsFilePath;
    private OracleManager manager;
    private ResultStatement currentlyProcessedResult;
    private final QubicReader qubicReader;
    private final Assembly assembly;

    private final IAMWriter writer;
    private final HastStatementWriter hashStatementWriter;
    private final ResultStatementWriter resultStatementWriter;

    private String name = "ql-node";
    private final LinkedList<OracleListener> oracleListeners = new LinkedList<>();

    private final Logger logger;

    /**
     * Creates a new IAMStream identity for this oracle.
     * @param publishAddress the address where to publish the oracle id.
     * @param qubicReader qubic to be processed
     * @param argsFilePath path to the args file
     * */
    public OracleWriter(String publishAddress, QubicReader qubicReader, Path argsFilePath) {
        this(publishAddress, qubicReader, argsFilePath, "")
;    }

    /**
     * Creates a new IAMStream identity for this oracle.
     * @param publishAddress the address where to publish the oracle id.
     * @param qubicReader qubic to be processed
     * @param argsFilePath path to the args file
     * @param loggerName name of the oracle writer logger.
     * */
    public OracleWriter(String publishAddress, QubicReader qubicReader, Path argsFilePath, String loggerName) {
        this.argsFilePath = argsFilePath;
        this.logger = loggerName.equals("") ? LogManager.getLogger(OracleWriter.class) : LogManager.getLogger(loggerName);
        this.qubicReader = qubicReader;
        assembly = new Assembly(qubicReader);
        writer = new IAMWriter(publishAddress);
        hashStatementWriter = new HastStatementWriter(writer);
        resultStatementWriter = new ResultStatementWriter(writer);
    }

    /**
     * Recreates an already existing Qubic by its IAMStream identity.
     * @param qubicReader       qubic to be processed
     * @param writer            IAM writer of the oracle
     * @param argsFilePath path to the args file
     * */
    public OracleWriter(QubicReader qubicReader, IAMWriter writer, Path argsFilePath) {
        this.argsFilePath = argsFilePath;
        this.logger = LogManager.getLogger(OracleWriter.class);
        this.qubicReader = qubicReader;
        assembly = new Assembly(qubicReader);
        this.writer = writer;
        hashStatementWriter = new HastStatementWriter(writer);
        resultStatementWriter = new ResultStatementWriter(writer);
    }

    /**
     * Lets assembly fetch ResultStatements from last epoch, then creates and publishes the HashStatement
     * for the current epoch. Calculates the result for the subsequent ResultStatement.
     *
     * @param epochIndex index of the current epoch
     *
     * Results from last epoch may be needed in calculation of current epoch result.
     * Publishing hash statement of result prevents oracles from just copying the results from other oracles.
     * They have to publish the hash of their result before the actual quorum based result can be revealed
     * */
    public void doHashStatement(int epochIndex) {
        // Results from last epoch may be needed for calculating new result.
        if(epochIndex > 0) {
            fetchStatements(new ResultStatementIAMIndex(epochIndex-1));
        }

        logger.debug("Calculate current Epoch Result and Hash");
        this.currentlyProcessedResult = new ResultStatement(epochIndex, calcResult(epochIndex));
        String hash = ResultHasher.hash(this.currentlyProcessedResult);
        logger.debug("Result: " + this.currentlyProcessedResult.getContent() + " Hash: " + hash);

        int[] ratings = assembly.getRatings();
        logger.debug("Write Hash Statement");
        hashStatementWriter.write(new HashStatement(epochIndex, hash, ratings));
    }

    private void fetchStatements(StatementIAMIndex index) {
        assembly.fetchStatements(index);
        if(index.getStatementType() == StatementType.HASH_STATEMENT)
            updateListenersWithPreviousEpoch(index.getEpoch());
    }

    private void updateListenersWithPreviousEpoch(int previousEpochIndex) {
        QuorumBasedResult qbr = assembly.getConsensusBuilder().buildConsensus(previousEpochIndex-1);
        for (OracleListener qf : oracleListeners){
            qf.onReceiveEpochResult(previousEpochIndex, qbr);
        }
    }

    /**
     * Lets assembly fetch HashStatements from current epoch, then creates and publishes the ResultStatement
     * for the current epoch. Result has already been calculated by doHashStatement().
     * */
    public void doResultStatement() {
        fetchStatements(new HashStatementIAMIndex(currentlyProcessedResult.getEpochIndex()));
        logger.debug("Write Result Statement");
        resultStatementWriter.write(currentlyProcessedResult);
        publishEpochLinkIfSet();
    }

    private void publishEpochLinkIfSet() {
        try {
            JSONObject result = new JSONObject(currentlyProcessedResult.getContent());
            if(result.has("epoch_link")) {
                JSONObject epoch_link = result.getJSONObject("epoch_link");
                long position = epoch_link.getLong("position");
                String keyword = epoch_link.getString("keyword");

                if(position < 0 || !TryteTool.isTryteSequence(keyword) || keyword.length() > IAMIndex.MAX_KEYWORD_LENGTH
                        || keyword.equals(StatementType.HASH_STATEMENT.getIAMKeyword())
                        || keyword.equals(StatementType.RESULT_STATEMENT.getIAMKeyword()))
                    return;

                JSONObject message = new JSONObject();
                message.put("type", "epoch link");
                message.put("qubic", qubicReader.getID());
                message.put("epoch", currentlyProcessedResult.getEpochIndex());
                writer.write(new IAMIndex(keyword, position), message);
            }
        } catch (JSONException e) {}
    }

    /**
     * Sends an application to the qubic's application address. The qubic owner might read
     * received applications on this address and consider adding the oracle to the assembly.
     * */
    public void apply() {
        throwExceptionIfTooLateToApply();
        sendApplication();
    }

    private void throwExceptionIfTooLateToApply() {
        if(qubicReader.getSpecification().timeUntilExecutionStart() <= 0)
            throw new IllegalStateException("applying aborted: qubic has already entered execution phase");
    }

    private void sendApplication() {
        JSONObject application = generateApplication();
        String applicationAddress = qubicReader.getApplicationAddress();
        logger.debug("Apply to Qubic. Application Address: " + applicationAddress);
        TangleAPI.getInstance().sendMessage(applicationAddress, application.toString());
    }

    private JSONObject generateApplication() {
        JSONObject application = new JSONObject();
        application.put(TangleJSONConstants.ORACLE_ID, getID());
        application.put(TangleJSONConstants.ORACLE_NAME, name);
        return application;
    }

    /**
     * Calculates the result string for the current epoch.
     * @return result string for current epoch
     * */
    private String calcResult(int epochIndex) {
        return QLVM.run(qubicReader.getSpecification().getCode(), OracleWriter.this, epochIndex, this.argsFilePath);
    }

    /**
     * Checks the assembly and adds all oracle mam roots listed in the assembly transaction to
     * its own assembly list in case it is part of the assembly.
     * @return TRUE = successfully made it into assembly, FALSE = did not make it into assembly
     * */
    public boolean assemble() {
        List<String> acceptedOracles = qubicReader.getAssemblyList();
        logger.debug("Read Assemby List:" + getOracleIdLogLines(acceptedOracles));
        boolean accepted = acceptedOracles != null && acceptedOracles.contains(getID());
        if(accepted && assembly.size() == 0) {
            assembly.addOracles(acceptedOracles);
        }
        logger.debug("Is accepted into Assembly: "  + accepted);
        return accepted;
    }

    private String getOracleIdLogLines(List<String> oracleIds) {
        StringBuilder stringBuilder = new StringBuilder();
        oracleIds.forEach(id -> stringBuilder.append("\n").append(id));
        return stringBuilder.toString();
    }

    public boolean isAcceptedIntoAssembly() {
        final QubicReader qubic = getQubicReader();
        final List<String> acceptedOracles = qubic.getAssemblyList();
        if(acceptedOracles == null) {
            if(qubic.getSpecification().ageOfExecutionPhase() < 0)
                throw new IllegalStateException("assembly transaction has not been published yet");
            return false;
        }
        return acceptedOracles.contains(getID());
    }

    /**
     * Registers a OracleListener to subscribe it to future events. Counterpart to unsubscribeOracleListener()
     * @param oracleListener the OracleListener to be registered
     * */
    public void subscribeOracleListener(OracleListener oracleListener) {
        oracleListeners.add(oracleListener);
    }

    /**
     * Unregisters a OracleListener from event subscription. Counterpart to subscribeOracleListener()
     * @param oracleListener the OracleListener to be unregistered
     * */
    public void unsubscribeOracleListener(OracleListener oracleListener) {
        oracleListeners.remove(oracleListener);
    }

    public IAMWriter getIAMWriter() { return writer; }

    public Assembly getAssembly() {
        return assembly;
    }

    public String getID() {
        return writer.getID();
    }

    public QubicReader getQubicReader() {
        return qubicReader;
    }

    public void setManager(OracleManager manager) {
        this.manager = manager;
    }

    public OracleManager getManager() {
        return manager;
    }

    public void setName(String name) {
        this.name = name;
    }
}