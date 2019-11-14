package oracle;

import oracle.statements.result.ResultStatement;
import org.junit.Test;
import qubic.EditableQubicSpecification;
import qubic.QubicReader;
import qubic.QubicWriter;
import tangle.TryteTool;

import static org.junit.Assert.*;

public class OracleReaderTest {

    @Test
    public void testResultStatement() {

        final int position = 7;
        final String code = "return({'epoch': epoch});", expected = "{'epoch': "+position+"}";

        QubicWriter qubicWriter = createQubicWriterWithPublishedQubicTransaction(code);
        OracleWriter oracleWriter = createOracleAndPublishResultStatement(qubicWriter, position);
        ResultStatement resultStatement = readResultStatement(oracleWriter.getID(), position);

        String assetMessage = "qubic ID: " + qubicWriter.getID() + ", oracle ID" + oracleWriter.getID();
        assertEquals(assetMessage, expected, resultStatement.getContent());
    }

//    @Test
//    public void testResultStatementSecond() {
//        final String code = "return(epoch);";
//
//        QubicWriter qubicWriter = createQubicWriterWithPublishedQubicTransaction(code);
//        QubicReader qubicReader = new QubicReader(qubicWriter.getID());
//        OracleWriter oracleWriter = new OracleWriter(qubicReader);
//        oracleWriter.subscribeOracleListener(new OracleListener(){
//                @Override
//                public void onReceiveEpochResult(int epochIndex, QuorumBasedResult qbr) {
//                    onReceivedEpochResult(epochIndex, qbr);
//                }
//            });
//        qubicWriter.getAssembly().add(oracleWriter.getID());
//        qubicWriter.publishAssemblyTransaction();
//        OracleManager oracleManager = new OracleManager(oracleWriter);
//        oracleManager.start();
//
//        while (true){
//            try {
//                Thread.sleep(10000);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//        }
//    }

    private void onReceivedEpochResult(int epoch, QuorumBasedResult qbr){
        System.out.println((epoch) + " has result: " + qbr.getResult());
    }

    private static QubicWriter createQubicWriterWithPublishedQubicTransaction(String code) {
        QubicWriter qubicWriter = new QubicWriter(TryteTool.TEST_ADDRESS_2);
        EditableQubicSpecification eqs =  qubicWriter.getEditable();
        eqs.setCode(code);
        qubicWriter.publishQubicTransaction();
        return qubicWriter;
    }

    private static OracleWriter createOracleAndPublishResultStatement(QubicWriter qubicWriter, int position) {
        QubicReader qubicReader = new QubicReader(qubicWriter.getID());
        OracleWriter oracleWriter = new OracleWriter(TryteTool.TEST_ADDRESS_2, qubicReader, null);
        qubicWriter.getAssembly().add(oracleWriter.getID());
        qubicWriter.publishAssemblyTransaction();
        oracleWriter.doHashStatement(position);
        oracleWriter.doResultStatement();
        return oracleWriter;
    }

    private static ResultStatement readResultStatement(String oracleID, int position) {
        OracleReader oracleReader = new OracleReader(oracleID);
        oracleReader.getHashStatementReader().read(position);
        return oracleReader.getResultStatementReader().read(position);
    }
}