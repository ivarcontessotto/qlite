package integration;

import com.jayway.jsonpath.JsonPath;
import constants.TangleJSONConstants;
import oracle.*;
import oracle.input.config.LogfileInputConfig;
import oracle.input.config.MamStreamInputConfig;
import oracle.input.config.ValueType;
import oracle.input.config.WebServiceInputConfig;
import oracle.input.provider.LogfileInputProvider;
import oracle.input.provider.MamStreamInputProvider;
import oracle.input.provider.OracleInputProvider;
import oracle.input.provider.WebServiceInputProvider;
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
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Pattern;

public class ManualIntegrationTest {

    private final static Logger LOGGER = LogManager.getLogger(ManualIntegrationTest.class);

    @Ignore
    @Test
    public void testEpochConsensusIT() throws InterruptedException {

        LOGGER.info("Generat Root Address for Test");
        String rootAddressForTest = TangleAPI.getInstance().getNextUnspentAddressFromSeed(TryteTool.TEST_SEED);
        LOGGER.info(rootAddressForTest);

        int secondsToExecutionStart = 40;
        int secondsUntilAssemble = 30;
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
        specification.setCode("kmh=GetInput();if(kmh<=10){traffic='stau';}else{traffic='normal';}return(traffic);");


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

            Path inputLogfilePath = Paths.get("src/test/res/InputLogfile" + i + ".log");
            LOGGER.info("Create Input Logfile: " + inputLogfilePath.toAbsolutePath().toString());
            createLogfile(inputLogfilePath, Arrays.asList("50", "41", "55"));

            LOGGER.info("Configure Oracle's external input");
            LogfileInputConfig inputConfig = new LogfileInputConfig(
                    ValueType.INTEGER,
                    inputLogfilePath,
                    Pattern.compile("(.*)"));

            OracleInputProvider inputProvider = new LogfileInputProvider(inputConfig);

            LOGGER.info("Create Oracle " + i);
            OracleWriter oracleWriter = new OracleWriter(rootAddressForTest, qubicReader, inputProvider);
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

    @Ignore
    @Test
    public void testGetOracleInputFromLogfileIT() throws InterruptedException {
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
        specification.setCode("kmh=GetInput();if(kmh<=10){traffic='stau';}else{traffic='normal';}return(traffic);");


        LOGGER.info("Publish Qubic Transaction to Tangle Address: " + rootAddressForTest);
        qubicWriter.publishQubicTransaction();
        String qubicId = qubicWriter.getID();
        LOGGER.info("Qubic ID (IAM Identity): " + qubicId);
        LOGGER.info("Qubic Transaction Hash: " + qubicWriter.getQubicTransactionHash());

        LOGGER.info("Promote Qubic");
        String keyword = "Wiprofun";
        qubicWriter.promote(rootAddressForTest, keyword);
        Thread.sleep(3000);

        LOGGER.info("Find promoted Qubic");
        List<String> promotedQubics = QubicPromotion.GetQubicAddressesByKeyword(keyword);

        QubicReader oraclesQubicReader;
        if (promotedQubics.size() > 0) {
            String firstPromotedQubicId = promotedQubics.get(0);
            LOGGER.info("First Promoted Qubic found: " + firstPromotedQubicId + " for Oracle");
            oraclesQubicReader = new QubicReader(firstPromotedQubicId);
        }
        else {
            LOGGER.info("No promoted Qubics found for Oracle");
            return;
        }

        Path inputLogfilePath = Paths.get("src/test/res/SomeLogfile.log");
        LOGGER.info("Create Input Logfile: " + inputLogfilePath.toAbsolutePath().toString());
        createLogfile(
                inputLogfilePath,
                Arrays.asList(
                        "2019-11-26 23:22:41,565 - Speed [km/h]: 50, Acceleration [m/s^2]: 0",
                        "2019-11-26 23:22:42,562 - Speed [km/h]: 49, Acceleration [m/s^2]: 0",
                        "2019-11-26 23:22:43,566 - Speed [km/h]: 51, Acceleration [m/s^2]: 0"));

        LOGGER.info("Configure Oracle's external input");
        LogfileInputConfig inputConfig = new LogfileInputConfig(
                ValueType.INTEGER,
                inputLogfilePath,
                Pattern.compile("Speed \\[km/h]: ([0-9]*),"));

        OracleInputProvider inputProvider = new LogfileInputProvider(inputConfig);

        LOGGER.info("Create Oracle");
        OracleWriter oracleWriter = new OracleWriter(rootAddressForTest, oraclesQubicReader, inputProvider);
        LOGGER.info("Oracle ID (IAM Identity): " + oracleWriter.getID());
        OracleManager oracleManager = new OracleManager(oracleWriter, "OracleManager");
        LOGGER.info("Start Oracle Lifecycle");
        oracleManager.start();

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

    @Ignore
    @Test
    public void testGetOracleInputFromFromWebserviceIT() throws MalformedURLException, InterruptedException {

        Queue<JsonPath> valueQueries = new LinkedList<>();
        valueQueries.add(JsonPath.compile("$.features[?(@.id=='CHZ')].properties.value"));
        valueQueries.add(JsonPath.compile("$[0]"));

        WebServiceInputConfig config = new WebServiceInputConfig(
                ValueType.DOUBLE,
                new URL("https://data.geo.admin.ch/ch.meteoschweiz.messwerte-lufttemperatur-10min/ch.meteoschweiz.messwerte-lufttemperatur-10min_de.json"),
                // new URL("https://data.geo.admin.ch/ch.meteoschweiz.messwerte-luftfeuchtigkeit-10min/ch.meteoschweiz.messwerte-luftfeuchtigkeit-10min_de.json"),
                10,
                valueQueries
        );
        OracleInputProvider inputProvider = new WebServiceInputProvider(config);

        for (int i = 0; i < 60; i++) {
            LOGGER.info("Last Input: " + inputProvider.getInput());
            Thread.sleep(1000);
        }
    }

    @Ignore
    @Test
    public void testGetOracleInputFromMamStreamIT() throws InterruptedException {

        String rootWithArrayMessages = "MY9FIMUONFTFBOTXNVOHNTEEVNQQNSCQQNETOUKBTRAKXKCHTRIVUCIISGTTBACBRACHPK9BEASCAXFUX";
        String rootWithSingleMessages = "FJ9RBUSLIDBZGL9LYKESCPHWSKGWVHUWZOXZKVD9FZNASXOCUMDWZLLNZ9X9FNFMRNCEKRPOQFFXKTX9A";

        Queue<JsonPath> valueQueries = new LinkedList<>();
        valueQueries.add(JsonPath.compile("$[-1]"));
        valueQueries.add(JsonPath.compile("$.data.Value"));

        MamStreamInputConfig config = new MamStreamInputConfig(
                ValueType.DOUBLE,
                3001,
                15,
                "https://nodes.devnet.iota.org:443",
                //rootWithArrayMessages,
                rootWithSingleMessages,
                valueQueries
        );
        OracleInputProvider inputProvider = new MamStreamInputProvider(config);

        for (int i = 0; i < 120; i++) {
            LOGGER.info("Last Input: " + inputProvider.getInput());
            Thread.sleep(1000);
        }
    }

    private static void createLogfile(Path argsFilePath, List<String> argsList) {
        File argsFile = argsFilePath.toFile();
        if (argsFile.exists()) {
            argsFile.delete();
        }

        try (PrintWriter out = new PrintWriter(argsFilePath.toFile())) {
            argsList.forEach(out::println);
        } catch (IOException e) {
            LOGGER.error("Could not create new argsfile", e);
        }
    }
}

