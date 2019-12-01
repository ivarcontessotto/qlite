package org.qubiclite.qlite.oracle.input.config;

public abstract class OracleInputConfig {

    private ValueType valueType;

    /**
     * Initializes an instance of the OracleInputConfig class.
     * @param valueType the input data type.
     */
    public OracleInputConfig(ValueType valueType) {
        this.valueType = valueType;
    }

    /**
     * Gets the input data type.
     * @return The data type identifier.
     */
    public final ValueType getValueType() {
        return this.valueType;
    }
}
