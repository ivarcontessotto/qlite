package org.qubiclite.qlite.qlvm.functions.qubic;

import org.qubiclite.qlite.oracle.OracleWriter;
import org.qubiclite.qlite.qlvm.InterQubicResultFetcher;
import org.qubiclite.qlite.oracle.QuorumBasedResult;
import org.qubiclite.qlite.qlvm.QLVM;
import org.qubiclite.qlite.qlvm.exceptions.runtime.UnknownFunctionException;
import org.qubiclite.qlite.qlvm.functions.Function;

public class FunctionQubicFetch extends Function {

    @Override
    public String getName() { return "qubic_fetch"; }

    @Override
    public String call(QLVM qlvm, String[] par) {

        if(qlvm.isInTestMode())
            throw new UnknownFunctionException("qubic_fetch");
        
        String qubicRoot = par[0];
        qubicRoot = qubicRoot.substring(1, qubicRoot.length()-1);

        int epochIndex = parseStringToNumber(par[1]).intValue();

        QuorumBasedResult qbr;

        OracleWriter oracleWriter = qlvm.getOracleWriter();

        if(qubicRoot.equals(oracleWriter.getQubicReader().getID()) && oracleWriter.getAssembly().hasMonitoredEpoch(epochIndex))
            qbr = oracleWriter.getAssembly().getConsensusBuilder().buildConsensus(epochIndex);
        else
            qbr = InterQubicResultFetcher.fetchResultConsensus(qubicRoot, epochIndex);

        return qbr.getResult();
    }
}
