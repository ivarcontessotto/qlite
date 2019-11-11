package FullTest;

import oracle.*;
import org.json.JSONObject;
import org.junit.Test;
import qubic.EditableQubicSpecification;
import qubic.QubicReader;
import qubic.QubicWriter;

import java.util.ArrayList;
import java.util.List;

public class FullTest {

    @Test
    public void subscribeToQubic() {
        QubicWriter qubicWriter = new QubicWriter();
        EditableQubicSpecification eqs = qubicWriter.getEditable();

        final int runtimeLimit = 9;
        final int hashPeriodDuration = 17;
        final String code = "return(epoch^2);";

        eqs.setRuntimeLimit(runtimeLimit);
        eqs.setHashPeriodDuration(hashPeriodDuration);
        eqs.setCode(code);

        qubicWriter.publishQubicTransaction();
        String qubicTransactionHash = qubicWriter.getQubicTransactionHash();
        String qubicId = qubicWriter.getID();
        QubicReader qubicReader = new QubicReader(qubicId);
        List<OracleWriter> oracleWriters = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            OracleWriter oracleWriter = new OracleWriter(qubicReader);
//            oracleWriter.subscribeOracleListener(new OracleListener(){
//                @Override
//                public void onReceiveEpochResult(int epochIndex, QuorumBasedResult qbr) {
//                    super.onReceiveEpochResult(epochIndex, qbr);
//                    oracleWriter.doResultStatement();
//                }
//            });

            OracleManager om = new OracleManager(oracleWriter);
            om.start();

            oracleWriters.add(oracleWriter);
        }

        List<JSONObject> applicants = qubicWriter.fetchApplications();
        while(applicants.isEmpty()) {
            applicants = qubicWriter.fetchApplications();
        }

        List<OracleReader> assemblyOracleReaders = new ArrayList<>();
        for (JSONObject application : applicants) {
            String oracle_Id = application.get("oracle_id").toString();
            assemblyOracleReaders.add(new OracleReader(oracle_Id));
            qubicWriter.getAssembly().add(oracle_Id);
        }

        qubicWriter.publishAssemblyTransaction();
        String assemblyId = qubicWriter.getAssemblyTransactionHash();

//        for (OracleWriter oracleWriter1 : oracleWriters){
//            if (oracleWriter1.assemble()){
//            }
//        }

        while (true){
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

//        while (qubicReader.lastCompletedEpoch() < 2){
//            try {
//                Thread.sleep(1000);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//        }
//
//        int lastEpoch = qubicReader.lastCompletedEpoch();
//
//        for (OracleWriter oracleWriter1 : oracleWriters){
//            oracleWriter1.doHashStatement(lastEpoch);
//            oracleWriter1.doResultStatement();
//        }
//
//        Assembly assemblyy = new Assembly(qubicReader);
//        ConsensusBuilder consensusBuilderr = new ConsensusBuilder(assemblyy);
//        QuorumBasedResult quorumBasedResultt = consensusBuilderr.buildConsensus(assemblyOracleReaders, 1);

//        int currentEpoch = -1;
//        while (true){
//            int lastEpoch = qubicReader.lastCompletedEpoch();
//            if (currentEpoch != lastEpoch){
//                currentEpoch = lastEpoch;
//                for (OracleWriter oracleWriter1 : oracleWriters){
//                    oracleWriter1.doHashStatement(currentEpoch);
//                    oracleWriter1.doResultStatement();
//                }
//
//                Assembly assembly = new Assembly(qubicReader);
//                ConsensusBuilder consensusBuilder = new ConsensusBuilder(assembly);
//                QuorumBasedResult quorumBasedResult = consensusBuilder.buildConsensus(assemblyOracleReaders, currentEpoch);
//                System.out.println((currentEpoch) + " has result: " + quorumBasedResult.getResult());
//                if (lastEpoch == 100){
//                    break;
//                }
//            }
//        }

//        ResultStatement epocheOne = oracleReader.getResultStatementReader().read(1);
//        ResultStatement epocheTwo = oracleReader.getResultStatementReader().read(2);
//        ResultStatement epocheThree = oracleReader.getResultStatementReader().read(3);
//        ResultStatement epocheFour = oracleReader.getResultStatementReader().read(4);
    }
}

