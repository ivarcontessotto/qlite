package org.qubiclite.qlite.oracle.input;

public interface OracleInputProvider {

    /**
     * Gets the oracle input provider configuration.
     * @return the configuration.
     */
    OracleInputConfig getConfig();

    /**
     * Gets the external oracle input.
     * @return The input.
     */
    String getInput();
}
