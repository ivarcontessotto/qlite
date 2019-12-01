package org.qubiclite.qlite.oracle.input.provider;

import org.qubiclite.qlite.oracle.input.config.OracleInputConfig;

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
