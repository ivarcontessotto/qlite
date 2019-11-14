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
        int secondsToExecutionStart = 30;
        int secondsUntilAssemble = 20;
        int secondsResultPeriod = 15;
        int secondsHashPeriod = 15;
        int secondsRuntimeLimit = 5;

        LOGGER.info("Create Qubic");
        QubicWriter qubicWriter = new QubicWriter();
        EditableQubicSpecification specification = qubicWriter.getEditable();
        specification.setExecutionStartToSecondsInFuture(secondsToExecutionStart);
        specification.setResultPeriodDuration(secondsResultPeriod);
        specification.setHashPeriodDuration(secondsHashPeriod);
        specification.setRuntimeLimit(secondsRuntimeLimit);
        specification.setCode("return(epoch^2);");

        LOGGER.info("Publish Qubic Transaction to Tangle Address: " + TryteTool.TEST_ADDRESS_1);
        qubicWriter.publishQubicTransaction();
        String qubicId = qubicWriter.getID();
        LOGGER.info("Qubic ID (IAM Identity): " + qubicId);
        LOGGER.info("Qubic Transaction Hash: " + qubicWriter.getQubicTransactionHash());

        List<OracleManager> oracleManagers = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            LOGGER.info("Create Oracle " + i);
            QubicReader qubicReader = new QubicReader(qubicId);
            OracleWriter oracleWriter = new OracleWriter(qubicReader, "Oracle" + i);
            LOGGER.info("Oracle ID (IAM Identity): " + oracleWriter.getID());
            OracleManager oracleManager = new OracleManager(oracleWriter, "OracleManager" + i);
            oracleManagers.add(oracleManager);
            LOGGER.info("Start Oracle Lifecycle");
            oracleManager.start();
        }

        LOGGER.info("Wait for Oracles to Subscribe");
        Thread.sleep(secondsUntilAssemble * 1000);

        LOGGER.info("Fetch Application");
        List<JSONObject> applications = qubicWriter.fetchApplications();
        if (applications.size() == 0) {
            LOGGER.info("No Applications found");
        }

        for (JSONObject application : applications) {
            String oracleID = application.getString(TangleJSONConstants.ORACLE_ID);
            LOGGER.info("Add Oracle to Assembly: " + oracleID);
            qubicWriter.getAssembly().add(oracleID);
        }

        LOGGER.info("Publish Assembly Transaction");
        qubicWriter.publishAssemblyTransaction();
        LOGGER.info("Assembly Transaction Hash: " + qubicWriter.getAssemblyTransactionHash());

        QubicReader qubicReader = new QubicReader(qubicId);
        LOGGER.info("Read Assembly List");
        for (String oracleId : qubicReader.getAssemblyList()) {
            LOGGER.info("Oracle Part of Assembly: " + oracleId);
        }

        for (int epoch = 0; epoch < 10; epoch++) {
            LOGGER.info("Waiting for Epoch " + epoch + " to complete");
            while (qubicReader.lastCompletedEpoch() < epoch) {
                Thread.sleep(1000);
            }
            LOGGER.info("Epoch " + epoch + " completed");

            LOGGER.info("Fetch Quorum Based Result");
            QuorumBasedResult quorumBasedResult = InterQubicResultFetcher.fetchResultConsensus(qubicId, epoch);

            double quorum = quorumBasedResult.getQuorum();
            double quorumMax = quorumBasedResult.getQuorumMax();
            double percentage = Math.round(1000 * quorum / quorumMax) / 10;

            LOGGER.info("EPOCH: " + epoch +
                    ", RESULT: " + quorumBasedResult.getResult() +
                    ", QUORUM: "  +  quorum + " / " + quorumMax + " ("+percentage+"%)" );
        }
    }
}

