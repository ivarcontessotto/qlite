package org.qubiclite.qlite.oracle.input.provider;

import org.qubiclite.qlite.oracle.input.config.OracleInputConfig;
import org.qubiclite.qlite.oracle.input.config.WebServiceInputConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.util.LinkedList;

public class WebServiceInputProvider implements OracleInputProvider, Runnable {

    private final Logger logger = LogManager.getLogger(WebServiceInputProvider.class);
    private WebServiceInputConfig config;
    private final Thread pollWebdata;
    private volatile String latestInput;

    /**
     * Initializes an instance of the WebServiceInputProvider class.
     *
     * @param config The configuration.
     */
    public WebServiceInputProvider(WebServiceInputConfig config) {
        this.config = config;
        this.pollWebdata = new Thread(this);
        this.pollWebdata.start();
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
        try {
            final int waitTimeMilliseconds = this.config.getServicePollingInterval() * 1000;

            while(true) {
                String response = this.readFromService();
                if (response != null) {
                    this.findInputInResponse(response);
                }
                Thread.sleep(waitTimeMilliseconds);
            }

        } catch (IOException | InterruptedException e) {
            this.logger.error("Error reading input from service.", e);
        }
    }

    private String readFromService() throws IOException {

        HttpURLConnection connection = (HttpURLConnection)this.config.getServiceUrl().openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Content-Type", "application/json");

        // Hack because HttpURLConnection is does not implement AutoClosable.
        try (AutoCloseable con = connection::disconnect;
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {

            final StringBuilder response = new StringBuilder();
            in.lines().forEach(response::append);
            return response.toString();

        } catch (Exception e) {
            this.logger.error("Error while reading from webservice.", e);
            return null;
        }
    }

    private void findInputInResponse(String response) {
        String found = JSONPathHelper.find(response, new LinkedList<>(config.getValueQueries()));
        if (found != null) {
            this.latestInput = found;
            logger.info("New latest input: " + this.latestInput);
        }
    }
}
