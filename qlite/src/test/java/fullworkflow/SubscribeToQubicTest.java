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
import static org.junit.Assert.assertTrue;

public class SubscribeToQubicTest {

    @Test
    public void testReadQubicTransaction() throws InterruptedException {

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
        QubicReader qubicReader = new QubicReader(qubicId);
        OracleWriter oracleWriter = new OracleWriter(qubicReader);


        OracleManager om = new OracleManager(oracleWriter);
        om.start();

        List<JSONObject> applicants = qubicWriter.fetchApplications();
        while(applicants.isEmpty()) {
            Thread.sleep(1000);
            applicants = qubicWriter.fetchApplications();
        }

        assertTrue(true);
    }
}
