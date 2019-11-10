package fullworkflow;

import oracle.OracleManager;
import oracle.OracleReader;
import oracle.OracleWriter;
import org.json.JSONObject;
import org.junit.Test;
import qubic.EditableQubicSpecification;
import qubic.QubicReader;
import qubic.QubicSpecification;
import qubic.QubicWriter;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class SubscribeToQubicTest {

    @Test
    public void testReadQubicTransaction() {

        QubicWriter qubicWriter = new QubicWriter();
        EditableQubicSpecification eqs = qubicWriter.getEditable();

        final int runtimeLimit = 9;
        final int hashPeriodDuration = 17;
        final String code = "return(33);";

        eqs.setRuntimeLimit(runtimeLimit);
        eqs.setHashPeriodDuration(hashPeriodDuration);
        eqs.setCode(code);

        qubicWriter.publishQubicTransaction();
        String qubicTransactionHash = qubicWriter.getQubicTransactionHash();
        String qubicId = qubicWriter.getID();
        QubicReader qr = new QubicReader(qubicId);
        OracleWriter oracleWriter = new OracleWriter(qr);


        OracleManager om = new OracleManager(oracleWriter);
        om.start();

        do{
            List<JSONObject> possibleSubscribers = qubicWriter.fetchApplications();
        } while (qubicWriter.fetchApplications().isEmpty());


//        qubicWriter.getAssembly().add(oracleWriter.getID());
//        qubicWriter.publishAssemblyTransaction();
//        oracleWriter.doHashStatement(position);
//        oracleWriter.doResultStatement();
//        OracleReader oracleReader = new OracleReader(oracleWriter.getID());
//        oracleReader.getHashStatementReader().read(position);
//        oracleReader.getResultStatementReader().read(position);




        String assertMessage = "qubic transaction hash: " + qubicWriter.getQubicTransactionHash();
        QubicReader qubicReader = new QubicReader(qubicWriter.getID());
        QubicSpecification read_qs = qubicReader.getSpecification();

        assertEquals(assertMessage, runtimeLimit, read_qs.getRuntimeLimit());
        assertEquals(assertMessage, hashPeriodDuration, read_qs.getHashPeriodDuration());
        assertEquals(assertMessage, code, read_qs.getCode());
    }
}
