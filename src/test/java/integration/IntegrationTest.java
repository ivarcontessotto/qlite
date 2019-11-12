package integration;

import constants.TangleJSONConstants;
import oracle.*;
import org.json.JSONObject;
import org.junit.Test;
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

    @Test
    public void integrationTest() throws InterruptedException {

        LOGGER.debug("Start integration test");

        LOGGER.debug("Create Qubic");
        QubicWriter qubicWriter = new QubicWriter();
        EditableQubicSpecification specification = qubicWriter.getEditable();
        specification.setExecutionStartToSecondsInFuture(30);
        specification.setResultPeriodDuration(10);
        specification.setHashPeriodDuration(10);
        specification.setRuntimeLimit(5);
        specification.setCode("return(epoch^2);");

        LOGGER.debug("Publish Qubic Transaction to Tangle Address: " + TryteTool.TEST_ADDRESS_1);
        qubicWriter.publishQubicTransaction();
        LOGGER.debug("Qubic Transaction Hash: " + qubicWriter.getQubicTransactionHash());
        String qubicId = qubicWriter.getID();
        LOGGER.debug("Qubic ID (IAM Identify): " + qubicId);

        List<OracleWriter> oracleWriters = new ArrayList<>();
        for (int i = 1; i <= 1; i++) {
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
        }

        LOGGER.debug("Wait for Oracles to Subscribe");
        Thread.sleep(15000);

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

        for (int epoch = 0; epoch < 10; epoch ++) {
            LOGGER.debug("Waiting for Epoch " + epoch + " to Complete");
            while (qubicReader.lastCompletedEpoch() < epoch) {
                Thread.sleep(1000);
            }
            LOGGER.debug("Epoch " + epoch + " completed");
        }

//        qubicReader.
//
//        while (qubicReader.getAssemblyList()){
//            try {
//                Thread.sleep(1000);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//        }
//
//        int lastEpoch = qubicReader.lastCompletedEpoch();
//
//        for (OracleWriter oracleWriter1 : oracleWriters){
//            oracleWriter1.doHashStatement(lastEpoch);
//            oracleWriter1.doResultStatement();
//        }
//
//        Assembly assemblyy = new Assembly(qubicReader);
//        ConsensusBuilder consensusBuilderr = new ConsensusBuilder(assemblyy);
//        QuorumBasedResult quorumBasedResultt = consensusBuilderr.buildConsensus(assemblyOracleReaders, 1);

//        int currentEpoch = -1;
//        while (true){
//            int lastEpoch = qubicReader.lastCompletedEpoch();
//            if (currentEpoch != lastEpoch){
//                currentEpoch = lastEpoch;
//                for (OracleWriter oracleWriter1 : oracleWriters){
//                    oracleWriter1.doHashStatement(currentEpoch);
//                    oracleWriter1.doResultStatement();
//                }
//
//                Assembly assembly = new Assembly(qubicReader);
//                ConsensusBuilder consensusBuilder = new ConsensusBuilder(assembly);
//                QuorumBasedResult quorumBasedResult = consensusBuilder.buildConsensus(assemblyOracleReaders, currentEpoch);
//                System.out.println((currentEpoch) + " has result: " + quorumBasedResult.getResult());
//                if (lastEpoch == 100){
//                    break;
//                }
//            }
//        }

//        ResultStatement epocheOne = oracleReader.getResultStatementReader().read(1);
//        ResultStatement epocheTwo = oracleReader.getResultStatementReader().read(2);
//        ResultStatement epocheThree = oracleReader.getResultStatementReader().read(3);
//        ResultStatement epocheFour = oracleReader.getResultStatementReader().read(4);
    }
}

