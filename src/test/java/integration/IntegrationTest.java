package integration;

import constants.TangleJSONConstants;
import oracle.*;
import org.json.JSONObject;
import org.junit.Ignore;
import org.junit.Test;
import qlvm.InterQubicResultFetcher;
import qubic.EditableQubicSpecification;
import qubic.QubicReader;
import qubic.QubicWriter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tangle.TryteTool;

import java.util.ArrayList;
import java.util.List;

public class IntegrationTest {

    private final static Logger LOGGER = LogManager.getLogger(IntegrationTest.class);

    @Ignore
    @Test
    public void testEpochResultsIT() throws InterruptedException {
        int secondsToExecutionStart = 60;
        int secondsUntilAssemble = 45;
        int secondsResultPeriod = 30;
        int secondsHashPeriod = 30;
        int secondsRuntimeLimit = 10;

        LOGGER.debug("Create Qubic");
        QubicWriter qubicWriter = new QubicWriter();
        EditableQubicSpecification specification = qubicWriter.getEditable();
        specification.setExecutionStartToSecondsInFuture(secondsToExecutionStart);
        specification.setResultPeriodDuration(secondsResultPeriod);
        specification.setHashPeriodDuration(secondsHashPeriod);
        specification.setRuntimeLimit(secondsRuntimeLimit);
        specification.setCode("return(epoch^2);");

        LOGGER.debug("Publish Qubic Transaction to Tangle Address: " + TryteTool.TEST_ADDRESS_1);
        qubicWriter.publishQubicTransaction();
        String qubicId = qubicWriter.getID();
        LOGGER.debug("Qubic ID (IAM Identity): " + qubicId);
        LOGGER.debug("Qubic Transaction Hash: " + qubicWriter.getQubicTransactionHash());

        List<OracleManager> oracleManagers = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            LOGGER.debug("Create Oracle " + i);
            QubicReader qubicReader = new QubicReader(qubicId);
            OracleWriter oracleWriter = new OracleWriter(qubicReader, "Oracle" + i);
            LOGGER.debug("Oracle ID (IAM Identity): " + oracleWriter.getID());
            OracleManager oracleManager = new OracleManager(oracleWriter, "OracleManager" + i);
            oracleManagers.add(oracleManager);
            LOGGER.debug("Start Oracle Lifecycle");
            oracleManager.start();
        }

        LOGGER.debug("Wait for Oracles to Subscribe");
        Thread.sleep(secondsUntilAssemble);

        LOGGER.debug("Fetch Application");
        List<JSONObject> applications = qubicWriter.fetchApplications();
        if (applications.size() == 0) {
            LOGGER.debug("No Applications found");
        }

        for (JSONObject application : applications) {
            String oracleID = application.getString(TangleJSONConstants.ORACLE_ID);
            LOGGER.debug("Add Oracle to Assembly: " + oracleID);
            LOGGER.debug(oracleID);
            qubicWriter.getAssembly().add(oracleID);
        }

        LOGGER.debug("Publish Assembly Transaction");
        qubicWriter.publishAssemblyTransaction();
        LOGGER.debug("Assembly Transaction Hash: " + qubicWriter.getAssemblyTransactionHash());

        QubicReader qubicReader = new QubicReader(qubicId);
        LOGGER.debug("Read Assembly List");
        for (String oracleId : qubicReader.getAssemblyList()) {
            LOGGER.debug("Oracle Part of Assembly: " + oracleId);
        }

        for (int epoch = 0; epoch < 10; epoch++) {
            LOGGER.debug("Waiting for Epoch " + epoch + " to complete");
            while (qubicReader.lastCompletedEpoch() < epoch) {
                Thread.sleep(1000);
            }
            LOGGER.debug("Epoch " + epoch + " completed");

            LOGGER.debug("Fetch Quorum Based Result");
            QuorumBasedResult quorumBasedResult = InterQubicResultFetcher.fetchResultConsensus(qubicId, epoch);

            double quorum = quorumBasedResult.getQuorum();
            double quorumMax = quorumBasedResult.getQuorumMax();
            double percentage = Math.round(1000 * quorum / quorumMax) / 10;

            LOGGER.debug("EPOCH:  " + epoch);
            LOGGER.debug("RESULT: " + quorumBasedResult.getResult());
            LOGGER.debug("QUORUM: " + quorum + " / " + quorumMax + " ("+percentage+"%)");
        }
    }
}

