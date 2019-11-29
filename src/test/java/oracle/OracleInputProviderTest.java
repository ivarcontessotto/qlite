package oracle;

import com.jayway.jsonpath.JsonPath;
import integration.IntegrationTest;
import oracle.input.config.LogfileInputConfig;
import oracle.input.config.MamStreamInputConfig;
import oracle.input.config.ValueType;
import oracle.input.config.WebServiceInputConfig;
import oracle.input.provider.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Ignore;
import org.junit.Test;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.regex.Pattern;

public class OracleInputProviderTest {

    private final static Logger LOGGER = LogManager.getLogger(IntegrationTest.class);

    @Ignore // Manual Test
    @Test
    public void testGetOracleInputFromLogfile() throws InterruptedException {

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

        for (int i = 0; i < 60; i++) {
            LOGGER.info("Last Input: " + inputProvider.getInput());
            Thread.sleep(1000);
        }
    }

    @Ignore // Manual Test
    @Test
    public void testGetOracleInputFromFromWebservice() throws MalformedURLException, InterruptedException {

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

    @Ignore // Manual Test
    @Test
    public void testGetOracleInputFromMamStream() throws InterruptedException {

        String rootWithArrayMessages = "MY9FIMUONFTFBOTXNVOHNTEEVNQQNSCQQNETOUKBTRAKXKCHTRIVUCIISGTTBACBRACHPK9BEASCAXFUX";
        String rootWithSingleMessages = "FJ9RBUSLIDBZGL9LYKESCPHWSKGWVHUWZOXZKVD9FZNASXOCUMDWZLLNZ9X9FNFMRNCEKRPOQFFXKTX9A";

        Queue<JsonPath> valueQueries = new LinkedList<>();
        valueQueries.add(JsonPath.compile("$[-1]"));
        valueQueries.add(JsonPath.compile("$.data.Value"));

        MamStreamInputConfig config = new MamStreamInputConfig(
                ValueType.DOUBLE,
                3000,
                15,
                "https://nodes.devnet.iota.org:443",
                // rootWithArrayMessages,
                //rootWithSingleMessages,
                "ZQSIPZYOINPROESTRZXHPOVQSAXBCIQRDMWFDNHGDSXAWFOAUNFJBDCKYOLRQWEHHYSGXRDEUMCRFCHLX",
                valueQueries
        );

        OracleInputProvider inputProvider = new MamStreamInputProvider(config);

        for (int i = 0; i < 100000; i++) {
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
