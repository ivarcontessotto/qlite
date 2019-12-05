package org.qubiclite.qlite.qlvm.functions.data;

import org.qubiclite.qlite.qlvm.QLVM;
import org.qubiclite.qlite.qlvm.functions.Function;

public class FunctionRound extends Function {

    @Override
    public String getName() { return "round"; }

    @Override
    public String call(QLVM qlvm, String[] par) {
        if (par == null || par.length == 0){
            return "NoValueGiven";
        }

        String value = par[0];
        if(value == null || value.isEmpty()) {
            return "NoValidNumberGiven";
        }

        try {
            Double doubleValue = Double.parseDouble(value);
            if (doubleValue != null){
                return Long.toString(Math.round(doubleValue));
            }
            else{
                return "NoValidNumberGiven";
            }
        }
        catch (NumberFormatException ex){
            // dont return a number, because this could be valid numbers and the result should not be valid after an exception
            return "NumberFromatException";
        }
    }
}
