package org.qubiclite.qlite.oracle.input.provider;

import org.qubiclite.qlite.oracle.input.config.LogfileInputConfig;
import org.qubiclite.qlite.oracle.input.config.OracleInputConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.util.regex.Matcher;

public class LogfileInputProvider implements OracleInputProvider, Runnable{

    private final Logger logger = LogManager.getLogger(LogfileInputProvider.class);
    private final LogfileInputConfig config;
    private final Thread tailLogfile;
    private volatile String latestInput;
    private String lastLine = null;

    /**
     * Initializes an instance of the LogfileInputProvider class.
     *
     * @param config The configuration.
     */
    public LogfileInputProvider(LogfileInputConfig config) {
        this.config = config;
        this.tailLogfile = new Thread(this);
        this.tailLogfile.start();
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

        File logfile = this.config.getLogfilePath().toFile();

        try(BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(logfile)))) {

            while(true) {
                this.skipToLastLine(in);
                if (this.lastLine != null) {
                    this.findInputInLastLine();
                }
                Thread.sleep(1000);
            }

        } catch (IOException | InterruptedException e) {
            logger.error("Error reading input logfile.", e);
        }
    }

    private void skipToLastLine(BufferedReader in) {
        this.lastLine = null;
        in.lines().forEach(line -> this.lastLine = line);
    }

    private void findInputInLastLine() {
        Matcher matcher = this.config.getValueRegex().matcher(this.lastLine);
        if (matcher.find()) {
            this.latestInput = matcher.group(1);
            logger.info("New latest input: " + this.latestInput);
        }
    }
}
