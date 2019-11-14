package qubic;

import constants.TangleJSONConstants;
import exceptions.NoQubicTransactionException;
import iam.IAMIndex;
import iam.IAMWriter;
import org.json.JSONException;
import org.json.JSONObject;
import tangle.QubicPromotion;
import tangle.TangleAPI;
import tangle.TryteTool;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * @author microhash
 *
 * The QubicWriter allows to easily create and manage a new qubic
 * from the qubic author's perspective. It's the writing counterpart to:
 * @see QubicReader
 * */

public class QubicWriter {

    static final IAMIndex QUBIC_TRANSACTION_IAM_INDEX = new IAMIndex(0);
    static final IAMIndex ASSEMBLY_TRANSACTION_IAM_INDEX = new IAMIndex(1);

    private final IAMWriter writer;
    private final List<String> assembly = new LinkedList<>();
    private String qubicTransactionHash, assemblyTransactionHash;
    private final EditableQubicSpecification editable;
    private QubicSpecification finalSpecification;
    private final Logger logger = LogManager.getLogger(QubicWriter.class);

    private QubicWriterState state = QubicWriterState.PRE_ASSEMBLY_PHASE;

    public QubicWriter() {
        writer = new IAMWriter();
        editable = new EditableQubicSpecification();
    }

    /**
     * Recreates an already existing Qubic by its IAMStream identity.
     * @param writer IAMStream of the qubic
     * */
    public QubicWriter(IAMWriter writer) {
        this.writer = writer;

        EditableQubicSpecification editable;

        try {
            QubicReader qr = new QubicReader(writer.getID());
            finalSpecification = qr.getSpecification();
            editable = new EditableQubicSpecification(qr.getSpecification());
            state = determineQubicWriterStateFromQubicReader(qr);
        } catch (NoQubicTransactionException e) {
            editable = new EditableQubicSpecification();
            state = QubicWriterState.PRE_ASSEMBLY_PHASE;
        }

        this.editable = editable;
    }

    /**
     * Publishes the qubic transaction to the IAMStream.
     * */
    public synchronized void publishQubicTransaction() {

        if(state != QubicWriterState.PRE_ASSEMBLY_PHASE)
            throw new IllegalStateException("qubic transaction can only be published if qubic is in state PRE_ASSEMBLY_PHASE, but qubic is in state " + state.name());
        editable.throwExceptionIfTooLateToPublish();

        finalSpecification = new QubicSpecification(editable);
        JSONObject qubicTransactionJSON = finalSpecification.generateQubicTransactionJSON();

        logger.debug("Write Qubic Transaction");
        qubicTransactionHash = writer.write(QUBIC_TRANSACTION_IAM_INDEX, qubicTransactionJSON);
        state = QubicWriterState.ASSEMBLY_PHASE;
    }

    /**
     * Publicly promotes the qubic transaction on the tangle to attract oracles for its assembly.
     * */
    public void promote() {
        String address = TryteTool.buildCurrentQubicPromotionAddress();
        logger.debug("Promote Qubic ID on Address: " + address);
        TangleAPI.getInstance().sendTrytes(address, writer.getID());
    }

    public void promote(String keyword) {
        QubicPromotion.StoreQubicAddressToTangleWithKeyword(getID(), keyword);
    }

    /**
     * Publishes the assembly transaction to the IAMStream. The assembly will consist
     * of all oracles added via addOracle().
     * */
    public synchronized void publishAssemblyTransaction() {
        throwExceptionIfCannotPublishAssemblyTransaction();
        JSONObject assemblyTransaction = generateAssemblyTransaction(assembly);
        logger.debug("Write Assemly Transaction");
        assemblyTransactionHash = writer.write(ASSEMBLY_TRANSACTION_IAM_INDEX, assemblyTransaction);
        state = QubicWriterState.EXECUTION_PHASE;
    }

    public String getID() {
        return writer.getID();
    }

    public String getApplicationAddress() {
        return getID() + TryteTool.DUMMY_CHECKSUM;
    }

    /**
     * Fetches all applications for this qubic
     * @return the fetched applications
     * */
    public List<JSONObject> fetchApplications() {
        logger.debug("Fetch Applications");
        Collection<String> transactionMessagesOnApplicationAddress = TangleAPI.getInstance().readTransactionsByAddress(
                null, getApplicationAddress(), true).values();
        List<JSONObject> applications = filterValidApplicationsFromTransactionMessages(transactionMessagesOnApplicationAddress);
        logger.debug("Found Applications:" + getApplicationLogLines(applications));
        return applications;
    }

    private String getApplicationLogLines(List<JSONObject> applications) {
        StringBuilder stringBuilder = new StringBuilder();
        applications.forEach(a -> stringBuilder.append("\n").append(a));
        return stringBuilder.toString();
    }

    private List<JSONObject> filterValidApplicationsFromTransactionMessages(Iterable<String> uncheckedTransactionMessages) {
        List<JSONObject> applications = new LinkedList<>();
        for(String transactionMessage : uncheckedTransactionMessages) {
            try {
                applications.add(new JSONObject(transactionMessage));
            } catch (JSONException e) {  }
        }
        return applications;
    }

    private void throwExceptionIfCannotPublishAssemblyTransaction() {
        if(state != QubicWriterState.ASSEMBLY_PHASE)
            throw new IllegalStateException("assembly transaction can only be published if qubic is in state ASSEMBLY_PHASE, but qubic is in state " + state.name());
        if(editable.timeUntilExecutionStart() <= 0)
            throw new IllegalStateException("the execution phase would have already started, it is too late to publish the assembly transaction now");
    }

    private JSONObject generateAssemblyTransaction(List<String> assembly) {

        JSONObject assemblyTransaction = new JSONObject();
        assemblyTransaction.put(TangleJSONConstants.TRANSACTION_TYPE, "assembly transaction");
        assemblyTransaction.put(TangleJSONConstants.QUBIC_ASSEMBLY, assembly);
        return assemblyTransaction;
    }

    private static QubicWriterState determineQubicWriterStateFromQubicReader(QubicReader qr) {
        if(qr.getSpecification() == null)
            return QubicWriterState.PRE_ASSEMBLY_PHASE;
        if(qr.getAssemblyList() != null)
            return QubicWriterState.EXECUTION_PHASE;
        return (qr.getSpecification().getExecutionStartUnix() < System.currentTimeMillis()/1000) ? QubicWriterState.ABORTED : QubicWriterState.ASSEMBLY_PHASE;
    }

    public String getQubicTransactionHash() {
        return qubicTransactionHash;
    }

    public String getAssemblyTransactionHash() {
        return assemblyTransactionHash;
    }

    public IAMWriter getIAMWriter() {
        return writer;
    }

    public List<String> getAssembly() {
        return assembly;
    }

    public EditableQubicSpecification getEditable() {
        if(state != QubicWriterState.PRE_ASSEMBLY_PHASE)
            throw new IllegalStateException("the specification cannot be edited anymore because the qubic transaction has already been published");
        return editable;
    }

    public QubicSpecification getSpecification() {
        return finalSpecification != null ? finalSpecification : editable;
    }

    public String getState() {
        if(state != QubicWriterState.EXECUTION_PHASE && editable.timeUntilExecutionStart() < 0)
            state = QubicWriterState.ABORTED;
        return state.name().toLowerCase().replace('_', ' ');
    }

    enum QubicWriterState {
        PRE_ASSEMBLY_PHASE, ASSEMBLY_PHASE, EXECUTION_PHASE, ABORTED;
    }
}