package integration;

import constants.TangleJSONConstants;
import oracle.*;
import org.json.JSONObject;
import org.junit.Ignore;
import org.junit.Test;
import qlvm.InterQubicResultFetcher;
import qlvm.QLVM;
import qubic.EditableQubicSpecification;
import qubic.QubicReader;
import qubic.QubicWriter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tangle.QubicPromotion;
import tangle.TangleAPI;
import tangle.TryteTool;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class IntegrationTest {

    private final static Logger LOGGER = LogManager.getLogger(IntegrationTest.class);

    @Ignore
    @Test
    public void testEpochResultsIT() throws InterruptedException {

        LOGGER.info("Generat Root Address for Test");
        String rootAddressForTest = TangleAPI.getInstance().getNextUnspentAddressFromSeed(TryteTool.TEST_SEED);
        LOGGER.info(rootAddressForTest);

        int secondsToExecutionStart = 30;
        int secondsUntilAssemble = 20;
        int secondsResultPeriod = 15;
        int secondsHashPeriod = 15;
        int secondsRuntimeLimit = 5;

        LOGGER.info("Create Qubic");
        QubicWriter qubicWriter = new QubicWriter(rootAddressForTest);
        EditableQubicSpecification specification = qubicWriter.getEditable();
        specification.setExecutionStartToSecondsInFuture(secondsToExecutionStart);
        specification.setResultPeriodDuration(secondsResultPeriod);
        specification.setHashPeriodDuration(secondsHashPeriod);
        specification.setRuntimeLimit(secondsRuntimeLimit);
        //specification.setCode("kmh=GetArgs(0);if(kmh<=10){traffic='stau';}else{traffic='normal';}return(traffic);");
        specification.setCode("return(5 + mam_read())");


        LOGGER.info("Publish Qubic Transaction to Tangle Address: " + rootAddressForTest);
        qubicWriter.publishQubicTransaction();
        String qubicId = qubicWriter.getID();
        LOGGER.info("Qubic ID (IAM Identity): " + qubicId);
        LOGGER.info("Qubic Transaction Hash: " + qubicWriter.getQubicTransactionHash());

        LOGGER.info("Promote Qubic");
        String keyword = "Wiprofun";
        qubicWriter.promote(rootAddressForTest, keyword);
        Thread.sleep(3000);

        List<OracleManager> oracleManagers = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
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

            Path argsFilePath = Paths.get("argsfile" + i + ".txt");
            LOGGER.info("Create Args File: " + argsFilePath.toAbsolutePath().toString());
            createArgsFile(argsFilePath, Arrays.asList(50));

            LOGGER.info("Create Oracle " + i);
            OracleWriter oracleWriter = new OracleWriter(rootAddressForTest, qubicReader, argsFilePath, "Oracle" + i);
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

    private static void createArgsFile(Path argsFilePath, List<Integer> argsList) {
        File argsFile = argsFilePath.toFile();
        if (argsFile.exists()) {
            argsFile.delete();
        }

        try (PrintWriter out = new PrintWriter(argsFilePath.toFile())) {
            argsList.forEach(a -> out.println(a.toString()));
        } catch (IOException e) {
            LOGGER.error("Could not create new argsfile", e);

        }
    }

    @Ignore
    @Test
    public void testMamRead() {
        ScriptEngine scriptEngine = new ScriptEngineManager().getEngineByName("graal.js");

        try {
            scriptEngine.eval(new FileReader("src/main/javascript/service.js"));

            String provider = "https://nodes.devnet.iota.org:443";
            String root = "QZIFJWSFOXPMWNDUXSFSOOAZFANHCNSOFWEVLYKMLUA9ZVSRLCQ99QYJ9PTUMTWPDTLALGIBHUNTZUAYN";
            String mode = "public";
            String key = null;

            Invocable invocable = (Invocable) scriptEngine;
            Object result = invocable.invokeFunction("fetchLastMessage", provider, root, mode, key);
            LOGGER.info(result);
        } catch (ScriptException | NoSuchMethodException | FileNotFoundException e) {
            System.out.println(e.getMessage());
        }
    }
}

