package integration;

import constants.TangleJSONConstants;
import iam.IAMIndex;
import oracle.*;
import oracle.statements.result.ResultStatementIAMIndex;
import org.json.JSONObject;
import org.junit.Test;
import qlvm.InterQubicResultFetcher;
import qubic.EditableQubicSpecification;
import qubic.QubicReader;
import qubic.QubicWriter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tangle.TryteTool;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class IntegrationTest {

    private final static Logger LOGGER = LogManager.getLogger(IntegrationTest.class);

    @Test
    public void integrationTest() throws InterruptedException {

        LOGGER.debug("Start integration test");

        LOGGER.debug("Create Qubic");
        QubicWriter qubicWriter = new QubicWriter();
        EditableQubicSpecification specification = qubicWriter.getEditable();
        specification.setExecutionStartToSecondsInFuture(60);
        specification.setResultPeriodDuration(30);
        specification.setHashPeriodDuration(30);
        specification.setRuntimeLimit(10);
        specification.setCode("return(epoch^2);");

        try (PrintWriter out = new PrintWriter("tempint.txt")) {
            out.println(20);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        LOGGER.debug("Publish Qubic Transaction to Tangle Address: " + TryteTool.TEST_ADDRESS_1);
        qubicWriter.publishQubicTransaction();
        LOGGER.debug("Qubic Transaction Hash: " + qubicWriter.getQubicTransactionHash());
        String qubicId = qubicWriter.getID();
        LOGGER.debug("Qubic ID (IAM Identify): " + qubicId);

        List<OracleWriter> oracleWriters = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            LOGGER.debug("Create Oracle " + i);
            LOGGER.debug("1. Create Qubic Reader");
            QubicReader qubicReader = new QubicReader(qubicId);
            LOGGER.debug("2. Create Oracle Writer");
            OracleWriter oracleWriter = new OracleWriter(qubicReader, "Oracle" + i);
            oracleWriters.add(oracleWriter);
            LOGGER.debug("Create Oracle Manager");
            OracleManager om = new OracleManager(oracleWriter, "OracleManager" + i);
            LOGGER.debug("Start Oracle Lifecycle");
            om.start();
            LOGGER.debug("Oracle node: " + oracleWriter.getID());
        }

        LOGGER.debug("Wait for Oracles to Subscribe");
        Thread.sleep(45000);

        LOGGER.debug("Fetch Application");
        List<JSONObject> applications = qubicWriter.fetchApplications();
        if (applications.size() == 0) {
            LOGGER.debug("No Applications");
        }

        LOGGER.debug("Add Applicants to Assembly:");
        for (JSONObject application : applications) {
            String oracleID = application.getString(TangleJSONConstants.ORACLE_ID);
            LOGGER.debug(oracleID);
            qubicWriter.getAssembly().add(oracleID);
        }

        LOGGER.debug("Publish Assembly Transaction");
        qubicWriter.publishAssemblyTransaction();
        LOGGER.debug("Assembly Transaction Hash: " + qubicWriter.getAssemblyTransactionHash());

        LOGGER.debug("Create Qubic Reader");
        QubicReader qubicReader = new QubicReader(qubicId);
        LOGGER.debug("Oracles in Assembly");
        for (String oracleId : qubicReader.getAssemblyList()) {
            LOGGER.debug(oracleId);
        }

        for (int epoch = 0; epoch < 10; epoch++) {
            LOGGER.debug("Waiting for Epoch " + epoch + " to Complete");
            while (qubicReader.lastCompletedEpoch() < epoch) {
                Thread.sleep(1000);
            }

            LOGGER.debug("Epoch " + epoch + " completed");
            LOGGER.debug("Fetch quorum based result for Epoch");
            IAMIndex iamIndex = new ResultStatementIAMIndex(epoch);
            QuorumBasedResult qbr = InterQubicResultFetcher.fetchQubicConsensus(qubicId, iamIndex);

            double quorum = qbr.getQuorum();
            double quorumMax = qbr.getQuorumMax();
            double percentage = Math.round(1000 * quorum / quorumMax) / 10;

            LOGGER.debug("INDEX:  " + (iamIndex.getKeyword().length() > 0 ? iamIndex.getKeyword() + " : " : "") + iamIndex.getPosition());
            LOGGER.debug("RESULT: " + qbr.getResult());
            LOGGER.debug("QUORUM: " + quorum + " / " + quorumMax + " ("+percentage+"%)");
        }
    }
}

