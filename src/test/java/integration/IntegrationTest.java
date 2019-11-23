package integration;

import com.jayway.jsonpath.JsonPath;
import constants.TangleJSONConstants;
import meteo_data.Geo_Admin_Humidity;
import meteo_data.Geo_Admin_Temperature;
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
import tangle.QubicPromotion;
import tangle.TangleAPI;
import tangle.TryteTool;

import java.io.*;
import java.net.Socket;
import java.net.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class IntegrationTest {

    private final static Logger LOGGER = LogManager.getLogger(IntegrationTest.class);

    @Ignore
    @Test
    public void testEpochResultsIT() throws InterruptedException {

        String humidity = Geo_Admin_Humidity.GetData();
        String temperature = Geo_Admin_Temperature.GetData();

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
    public void testMamFetchService() throws InterruptedException {
        String localhost = "127.0.0.1";
        Integer port = 2001;
        String provider = "https://nodes.devnet.iota.org:443";
        String mamRoot = "FJ9RBUSLIDBZGL9LYKESCPHWSKGWVHUWZOXZKVD9FZNASXOCUMDWZLLNZ9X9FNFMRNCEKRPOQFFXKTX9A";

        try {
            LOGGER.info("Start service process.");
            ProcessBuilder pb = new ProcessBuilder("node", "service.js", port.toString(), provider, mamRoot);
            pb.directory(new File("./src/main/javascript/mamfetch/"));
            Process service = pb.start();

            LOGGER.info("Start listening for messages.");
            this.readFromService(localhost, port, 10);

            LOGGER.info("Killing service process.");
            this.killProcess(service);

        } catch (IOException e) {
            LOGGER.error("Error", e);
        }
    }

    private void readFromService(String host, int port, int times) {
        try (Socket socket = new Socket(host, port);
             BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            for (int i = 0; i < times; i++) {
                LOGGER.info(bufferedReader.readLine());
            }

        } catch (IOException e) {
            LOGGER.error("Error", e);
        }
    }

    private void killProcess(Process process) throws InterruptedException {
        process.destroy();
        process.waitFor(10, TimeUnit.SECONDS);
        if (process.isAlive()) {
            process.destroyForcibly();
        }
    }
}

