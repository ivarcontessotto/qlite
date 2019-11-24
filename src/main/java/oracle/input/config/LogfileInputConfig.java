package oracle.input.config;

import java.nio.file.Path;
import java.util.regex.Pattern;

public class LogfileInputConfig extends OracleInputConfig {

    private Path logfilePath;
    private Pattern valueRegex;

    /**
     * Initializes an instance of the LogfileInputConfig class.
     *
     * @param valueType the input data type.
     * @param logfilePath the path to the input logfile.
     * @param valueRegex the regex to match the input value from a log line.
     */
    public LogfileInputConfig(ValueType valueType, Path logfilePath, Pattern valueRegex) {
        super(valueType);
        this.logfilePath = logfilePath;
        this.valueRegex = valueRegex;
    }

    /**
     * Gets the path to the input logfile.
     * @return The logfile path.
     */
    public Path getLogfilePath() {
        return this.logfilePath;
    }

    /**
     * Gets the regex to match the input value from a log line.
     * @return The regex pattern.
     */
    public Pattern getValueRegex() {
        return this.valueRegex;
    }
}
