package org.qubiclite.qlite.qlvm;

import org.qubiclite.qlite.constants.GeneralConstants;
import org.qubiclite.qlite.iam.IAMIndex;
import org.qubiclite.qlite.oracle.Assembly;
import org.qubiclite.qlite.oracle.OracleReader;
import org.qubiclite.qlite.oracle.QuorumBasedResult;
import org.qubiclite.qlite.oracle.statements.hash.HashStatementIAMIndex;
import org.qubiclite.qlite.oracle.statements.result.ResultStatementIAMIndex;
import org.qubiclite.qlite.qubic.QubicReader;

import java.util.HashMap;
import java.util.List;

/**
 * @author microhash
 *
 * This class allows to fetch results from other qubics not watched by the OracleWriter.
 * */
public class InterQubicResultFetcher {

    private static final HashMap<String, Assembly> knownAssemblies = new HashMap<>();

    /**
     * Fetches the QuorumBasedResult from any qubic.
     * @param qubicId     iam stream id of qubic
     * @param epochIndex  index of the epoch of which the result shall be determined
     * @return the fetched QuorumBasedResult
     * */
    public static QuorumBasedResult fetchResultConsensus(String qubicId, int epochIndex) {
        Assembly assembly = getAssembly(qubicId);
        return findConsensus(assembly, epochIndex);
    }

    /**
     * Fetches the QuorumBasedResult from any qubic.
     * @param qubicReader QubicReader for qubic to fetch from
     * @param epochIndex  index of the epoch of which the result shall be determined
     * @return the fetched QuorumBasedResult
     * */
    public static QuorumBasedResult fetchResultConsensus(QubicReader qubicReader, int epochIndex) {
        Assembly assembly = getAssembly(qubicReader);
        return findConsensus(assembly, epochIndex);
    }

    /**
     * Fetches the QuorumBasedResult from any IAM Index of the qubics assembly.
     * Operates on the IAM message level. Does not work to get the qubics result consensus!
     * @param qubicId
     * @param index
     * @return
     */
    public static QuorumBasedResult fetchIAMConsensus(String qubicId, IAMIndex index) {
        Assembly assembly = getAssembly(qubicId);
        return assembly.getConsensusBuilder().buildIAMConsensus(index);
    }

    private static QuorumBasedResult findConsensus(Assembly assembly, int epochIndex) {
        List<OracleReader> selection = assembly.selectRandomOracleReaders(GeneralConstants.QUORUM_MAX_ORACLE_SELECTION_SIZE);
        if(!assembly.getConsensusBuilder().hasAlreadyDeterminedQuorumBasedResult(epochIndex)) {
            assembly.fetchStatements(selection, new HashStatementIAMIndex(epochIndex));
            assembly.fetchStatements(selection, new ResultStatementIAMIndex(epochIndex));
        }
        return assembly.getConsensusBuilder().buildConsensus(selection, epochIndex);
    }

    private static Assembly getAssembly(String qubicID) {
        return knownAssemblies.containsKey(qubicID)
            ? knownAssemblies.get(qubicID)
            : createAssembly(new QubicReader(qubicID));
    }

    private static Assembly getAssembly(QubicReader qubicReader) {
        return knownAssemblies.containsKey(qubicReader.getID())
                ? knownAssemblies.get(qubicReader.getID())
                : createAssembly(qubicReader);
    }

    private static Assembly createAssembly(QubicReader qr) {
        List<String> assemblyList = qr.getAssemblyList();
        Assembly assembly = new Assembly(qr);
        assembly.addOracles(assemblyList);
        knownAssemblies.put(qr.getID(), assembly);
        return assembly;
    }
}