package org.qubiclite.qlite.qlvm;

import org.qubiclite.qlite.constants.GeneralConstants;
import org.qubiclite.qlite.qlvm.exceptions.runtime.QLValueMaxLengthExceeded;
import org.qubiclite.qlite.qlvm.exceptions.runtime.UnknownFunctionException;
import org.qubiclite.qlite.qlvm.functions.*;
import org.qubiclite.qlite.qlvm.functions.data.FunctionSizeOf;
import org.qubiclite.qlite.qlvm.functions.data.FunctionType;
import org.qubiclite.qlite.qlvm.functions.input.FunctionGetInput;
import org.qubiclite.qlite.qlvm.functions.iam.FunctionIAMRead;
import org.qubiclite.qlite.qlvm.functions.iota.*;
import org.qubiclite.qlite.qlvm.functions.qubic.FunctionQubicConsensus;
import org.qubiclite.qlite.qlvm.functions.qubic.FunctionQubicFetch;
import org.qubiclite.qlite.qlvm.functions.string.FunctionHash;
import org.qubiclite.qlite.qlvm.functions.string.FunctionSubstr;

/**
 * @author microhash
 *
 * FunctionCall provides a way to call any function formally. It is easily extendable
 * and therefore was implemented to bundle and manage all Functions.
 * */
public final class FunctionCall {

    private static final Function[] functions = {
        new FunctionQubicFetch(),
        new FunctionQubicConsensus(),
        new FunctionIAMRead(),
        new FunctionIotaBalance(),
        new FunctionSizeOf(),
        new FunctionType(),
        new FunctionSubstr(),
        new FunctionHash(),
        new FunctionGetInput(),
    };

    private FunctionCall() {}

    /**
     * Calls a function on a specific QLVM.
     * @param qlvm the QLVM in which the function was called, provides the data for the actual function
     * @param functionName the name of the function (e.g. "qubic_fetch")
     * @param par normalized function parameters
     * @return return value of the actual function or NULL if function not found
     * */
    public static String call(QLVM qlvm, String functionName, String[] par) {

        for(Function f : functions)
            if(f.getName().equals(functionName)) {
                String ret = f.call(qlvm, par);
                if(ret != null && ret.length() > GeneralConstants.QLVM_MAX_VALUE_LENGTH)
                    throw new QLValueMaxLengthExceeded(ret);
                return ret;
            }

        throw new UnknownFunctionException(functionName);
    }
}