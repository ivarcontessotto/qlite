package org.qubiclite.qlite.qlvm.functions.qubic;

import org.qubiclite.qlite.iam.IAMIndex;
import org.qubiclite.qlite.qlvm.InterQubicResultFetcher;
import org.qubiclite.qlite.qlvm.QLVM;
import org.qubiclite.qlite.qlvm.functions.Function;

public class FunctionQubicConsensus extends Function {

    @Override
    public String getName() { return "qubic_consensus"; }

    @Override
    public String call(QLVM qlvm, String[] par) {
        
        String qubicID = par[0];
        qubicID = qubicID.substring(1, qubicID.length()-1);

        String iamIndexKeyword = par[1];
        iamIndexKeyword = iamIndexKeyword.substring(1, iamIndexKeyword.length()-1);

        int iamIndexPosition = parseStringToNumber(par[2]).intValue();

        String result = InterQubicResultFetcher.fetchIAMConsensus(qubicID, new IAMIndex(iamIndexKeyword, iamIndexPosition)).getResult();
        return result == null ? null : "'"+result+"'";
    }
}
