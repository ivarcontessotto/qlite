package oracle.input.provider;

import oracle.input.config.MamStreamInputConfig;
import oracle.input.config.OracleInputConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.LinkedList;
import java.util.concurrent.TimeUnit;

public class MamStreamInputProvider implements OracleInputProvider, Runnable {

    private final static String LOCALHOST = "127.0.0.1";

    private final Logger logger = LogManager.getLogger(MamStreamInputProvider.class);
    MamStreamInputConfig config;
    private final Thread listenToService;
    private final Logger serviceLogger = LogManager.getLogger("MAMService");
    private Thread logServiceProcessOutput;
    private volatile String latestInput;

    /**
     * Initializes an instance of the MamStreamInputProvider class.
     *
     * @param config The configuration.
     */
    public MamStreamInputProvider(MamStreamInputConfig config) {
        this.config = config;
        this.listenToService = new Thread(this);
        this.listenToService.start();
        LoudJSONPathLogger.shutUp();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OracleInputConfig getConfig() {
        return this.config;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getInput() {
        return this.latestInput;
    }

    @Override
    public void run() {

        Process serviceProcess = this.startMamFetchService();
        this.listenForMamMessages();
        this.killMamFetchService(serviceProcess);
    }

    private Process startMamFetchService() {
        this.logger.debug("Start MAM fetch service process.");
        try {
            ProcessBuilder processBuilder = new ProcessBuilder("node", "nodejs/MAMFetchService.js",
                    String.valueOf(this.config.getServicePort()),
                    String.valueOf(this.config.getServicePollingInterval()),
                    config.getNodeProviderUrl(),
                    config.getRootAddress());

            Process serviceProcess = processBuilder.start();

            this.logServiceProcessOutput = new Thread(
                    new StreamGobbler(serviceProcess.getInputStream(), this.serviceLogger::info));
            this.logServiceProcessOutput.start();

            return serviceProcess;

        } catch (IOException e) {
            this.logger.error("Error starting MAM fetch service process.", e);
            return null;
        }
    }

    private void listenForMamMessages() {
        try (Socket socket = new Socket(LOCALHOST, this.config.getServicePort());
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            in.lines().forEach(message -> {
                this.latestInput = JSONPathHelper.find(message, new LinkedList<>(this.config.getValueQueries()));
            });

        } catch (IOException e) {
            this.logger.error("Error", e);
        }
    }

    private void killMamFetchService(Process serviceProcess) {
        if (serviceProcess == null) {
            return;
        }
        this.logger.debug("Kill MAM fetch service process.");
        try {
            serviceProcess.destroy();
            serviceProcess.waitFor(20, TimeUnit.SECONDS);
            if (serviceProcess.isAlive()) {
                serviceProcess.destroyForcibly();
            }
        } catch (InterruptedException e) {
            this.logger.error("Error killing MAM fetch service process.", e);
        }
    }
}
