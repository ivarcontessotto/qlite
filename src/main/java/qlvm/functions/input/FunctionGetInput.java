package qlvm.functions.input;

import oracle.input.config.ValueType;
import oracle.input.provider.OracleInputProvider;
import qlvm.QLVM;
import qlvm.functions.Function;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class FunctionGetInput extends Function {

    private Logger logger = LogManager.getLogger(FunctionGetInput.class);

    @Override
    public String getName() { return "getinput"; }

    @Override
    public String call(QLVM qlvm, String[] par) {

        if (qlvm.getOracleInputProvider() == null) {
            throw new IllegalStateException("Oracle is not configured to provide external inputs.");
        }

        OracleInputProvider inputProvider = qlvm.getOracleInputProvider();
        ValueType valueType = inputProvider.getConfig().getValueType();
        String input = inputProvider.getInput();

        switch (valueType) {
            case INTEGER:
                return this.convertToQlvmInteger(input);
            case DOUBLE:
                return this.convertToQlvmDouble(input);
            case STRING:
                return this.convertToQlvmString(input);
            default:
                throw new IllegalStateException(String.format("Illegal configured input value type %s", valueType.name()));
        }
    }

    private String convertToQlvmInteger(String input) {
        try {
            Integer.parseInt(input);
            return input;
        } catch (NumberFormatException e) {
            this.logger.error(String.format("Input %s is not an Integer.", input));
            return null;
        }
    }

    private String convertToQlvmDouble(String input) {
        try {
            Double.parseDouble(input);
            return input;
        } catch (NumberFormatException e) {
            this.logger.error(String.format("Input %s is not a Double.", input));
            return null;
        }
    }

    private String convertToQlvmString(String input) {
        return String.format("'%s'", input);
    }
}
