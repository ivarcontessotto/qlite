package qubic;

import static org.junit.Assert.*;

import org.junit.Test;
import tangle.TryteTool;

public class QubicWriterTest {

    @Test(expected = IllegalStateException.class)
    public void pretendDoubleQubicTransaction() {
        QubicWriter qwriter = new QubicWriter(TryteTool.TEST_ADDRESS_2);
        try {
            qwriter.publishQubicTransaction();
        } catch (IllegalStateException e) {
            fail("this first qubic transaction should have worked");
        }
        qwriter.publishQubicTransaction();
    }
}