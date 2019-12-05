package org.qubiclite.qlite;

import org.qubiclite.qlite.constants.TangleJSONConstants;
import org.qubiclite.qlite.oracle.*;
import org.json.JSONObject;
import org.junit.Ignore;
import org.junit.Test;
import org.qubiclite.qlite.oracle.input.QueueInputProvider;
import org.qubiclite.qlite.oracle.input.QueueInputConfig;
import org.qubiclite.qlite.oracle.input.ValueType;
import org.qubiclite.qlite.qlvm.InterQubicResultFetcher;
import org.qubiclite.qlite.qubic.EditableQubicSpecification;
import org.qubiclite.qlite.qubic.QubicReader;
import org.qubiclite.qlite.qubic.QubicWriter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.qubiclite.qlite.tangle.QubicPromotion;
import org.qubiclite.qlite.tangle.TangleAPI;
import org.qubiclite.qlite.tangle.TryteTool;

import java.util.*;

public class IntegrationTest {

    private final static Logger LOGGER = LogManager.getLogger(IntegrationTest.class);

    @Ignore // Manual Test
    @Test
    public void testEpochConsensus() throws InterruptedException {

        LOGGER.info("Generat Root Address for Test");
        String rootAddressForTest = TangleAPI.getInstance().getNextUnspentAddressFromSeed(TryteTool.TEST_SEED);
        LOGGER.info(rootAddressForTest);

        int secondsToExecutionStart = 120;
        int secondsUntilAssemble = 90;
        int secondsResultPeriod = 30;
        int secondsHashPeriod = 30;
        int secondsRuntimeLimit = 10;

        LOGGER.info("Create Qubic");
        QubicWriter qubicWriter = new QubicWriter(rootAddressForTest);
        EditableQubicSpecification specification = qubicWriter.getEditable();
        specification.setExecutionStartToSecondsInFuture(secondsToExecutionStart);
        specification.setResultPeriodDuration(secondsResultPeriod);
        specification.setHashPeriodDuration(secondsHashPeriod);
        specification.setRuntimeLimit(secondsRuntimeLimit);
//        specification.setCode("kmh=GetInput();(kmh<=10){traffic='stau';}else{traffic='normal';}return(traffic);");
        specification.setCode("rawTemp=GetInput();roundedTemp=Round(rawTemp);return(roundedTemp);");

        LOGGER.info("Publish Qubic Transaction to Tangle Address: " + rootAddressForTest);
        qubicWriter.publishQubicTransaction();
        String qubicId = qubicWriter.getID();
        LOGGER.info("Qubic ID (IAM Identity): " + qubicId);
        LOGGER.info("Qubic Transaction Hash: " + qubicWriter.getQubicTransactionHash());

        LOGGER.info("Promote Qubic");
        String keyword = "Wiprofun";
        qubicWriter.promote(rootAddressForTest, keyword);
        Thread.sleep(3000);

        List<Queue<String>> oracleInputSequences = new ArrayList<>();
        oracleInputSequences.add(new LinkedList<>(Arrays.asList("10.8", "4.0", "3.1", "1.5", "20.8", "55.6")));
//        oracleInputSequences.add(new LinkedList<>(Arrays.asList("45", "49", "2", "1", "3", "60")));
//        oracleInputSequences.add(new LinkedList<>(Arrays.asList("47", "43", "11", "0", "15", "61")));

        for (int i = 0; i < 3; i++) {
            LOGGER.info("Find promoted Qubic");
            List<String> promotedQubics = QubicPromotion.GetQubicAddressesByKeyword(keyword);

            QubicReader qubicReader;
            if (promotedQubics.size() > 0) {
                String firstPromotedQubicId = promotedQubics.get(0);
                LOGGER.info("First Promoted Qubic found: " + firstPromotedQubicId + " for Oracle " + i);
                qubicReader = new QubicReader(firstPromotedQubicId);
            }
            else {
                LOGGER.info("No promoted Qubics found for Oracle " + i);
                continue;
            }

            LOGGER.info("Create Oracle " + i);
            OracleWriter oracleWriter = new OracleWriter(
                    rootAddressForTest,
                    qubicReader,
                    new QueueInputProvider(new QueueInputConfig(ValueType.DOUBLE, oracleInputSequences.get(i))));

            LOGGER.info("Oracle ID (IAM Identity): " + oracleWriter.getID());
            OracleManager oracleManager = new OracleManager(oracleWriter, "OracleManager" + i);
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

        for (int epoch = 0; epoch < 12; epoch++) {
            LOGGER.info("Waiting for Epoch " + epoch + " to complete");
            while (qubicReader.lastCompletedEpoch() < epoch) {
                Thread.sleep(1000);
            }
            LOGGER.info("Epoch " + epoch + " completed");
            Thread.sleep(1000);

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

