package oracle;

import org.iota.jota.model.Transaction;
import oracle.statements.StatementType;
import oracle.statements.result.ResultStatement;
import oracle.statements.StatementIAMIndex;
import qubic.QubicReader;
import tangle.TangleAPI;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

/**
 * @author microhash
 *
 * The Assembly models all oracleReaders for a specific qubic. It is the class reponsible
 * to determine the quorum based results.
 * */
public class Assembly {

    private final Logger logger;
    private final QubicReader qubicReader;
    private final List<OracleReader> oracleReaders = new LinkedList<>();
    private final ConsensusBuilder consensusBuilder = new ConsensusBuilder(this);
    private int[] ratings;

    private int firstEpochIndex = -1; // epoch at which the oracle started monitoring the qubic epochs. necessary to decide when to use InterQubicResultFetcher for own assembly

    public Assembly(QubicReader qubicReader) {
        this.logger = LogManager.getLogger(Assembly.class);
        this.qubicReader = qubicReader;
    }

    /**
     * Adds oracleReaders to the assembly.
     * @param oracleIDs the oracle ids of each respective oracle to add
     * @throws NullPointerException if oracleIDs is null
     * */
    public void addOracles(List<String> oracleIDs) {
        if(oracleIDs == null)
            throw new NullPointerException("parameter 'oracleIDs' is null");
        for(String oracleID : oracleIDs)
            oracleReaders.add(new OracleReader(oracleID));
    }

    /**
     * Ensures that every oracle in the assembly has its Statement for a certain epoch available.
     * @param selection         a selection of the whole assembly (allows probabilisticly determined quorum)
     * TODO optimize fetching by putting all findTransaction() requests into a single API call
     * */
    public void fetchStatements(List<OracleReader> selection, StatementIAMIndex index) {
        if(firstEpochIndex < 0 && index.getStatementType() == StatementType.RESULT_STATEMENT)
            firstEpochIndex = index.getEpoch();

        String[] addresses = buildStatementAddresses(selection, index);

        List<Transaction> preload = TangleAPI.getInstance().findTransactionsByAddresses(addresses);

        for (OracleReader o : selection)
            o.read(preload, index);
    }

    private static String[] buildStatementAddresses(List<OracleReader> selection, StatementIAMIndex index) {
        String[] addresses = new String[selection.size()];
        for (int i = 0; i < addresses.length; i++) {
            OracleReader oracleReader = selection.get(i);
            addresses[i] = oracleReader.getReader().buildAddress(index);
        }
        return addresses;
    }

    /**
     * Ensures that every oracle in the assembly has its statement for a certain epoch available.
     * */
    public void fetchStatements(StatementIAMIndex index) {
        fetchStatements(oracleReaders, index);
    }

    /**
     * Rates every oracle in the assembly by its contribution during the last epoch.
     * The ratings are published in the subseqeuent epoches HashStatement. This allows
     * a future reconstruction to prevent oracles from publishing results retrospectively.
     *
     * ratings: -1 = negative, 0 = neutral, 1 = positive
     *
     * @param epochIndex   index of epoch that shall be rated
     * @param quorumResult quorum based result (determines which nodes are correct)
     *
     * TODO actually call from somewhere
     * */
    protected void rate(int epochIndex, String quorumResult) {
        // reset ratings
        ratings = new int[oracleReaders.size()];

        // rate every oracle
        for(int i = 0; i < oracleReaders.size(); i++) {
            OracleReader oracleReader = oracleReaders.get(i);

            // neutral rating for qnode.statements that were ignored in the last epoch
            // due to not being existent or following an invalid hash statement
            ResultStatement resultEpoch = oracleReader.getResultStatementReader().read(epochIndex);
            if(resultEpoch == null || !resultEpoch.isHashStatementValid()) {
                ratings[i] = 0;
                continue;
            }

            // otherwise rate based on correctness of published result
            ratings[i] = quorumResult.equals(resultEpoch.getContent()) ? 1 : -1;
        }

    }

    /**
     * Filters out random oracles. Used to determine quorum deterministically.
     * @param amount amount of oracles to select
     * @return random selection of oracleReaders from the assembly (no double entries)
     * */
    public List<OracleReader> selectRandomOracleReaders(int amount) {
        logger.debug("Select " + amount + " Random Oracles from:" + getOracleIdLogLines(oracleReaders));

        if(amount < 0)
            throw new IllegalArgumentException("parameter amount cannot be negative");

        amount = Math.min(amount, oracleReaders.size());

        List<OracleReader> selectFrom = new ArrayList<>(oracleReaders);
        List<OracleReader> selection = new LinkedList<>();

        Random random = new Random();
        for (int i = amount; i > 0; i--) {
            selection.add(selectFrom.remove(random.nextInt(selectFrom.size())));
        }

        logger.debug("Selected Oracles:" + getOracleIdLogLines(selection));
        return selection;
    }

    private String getOracleIdLogLines(List<OracleReader> oracleReaders) {
        StringBuilder stringBuilder = new StringBuilder();
        oracleReaders.forEach(r -> stringBuilder.append("\n").append(r.getID()));
        return stringBuilder.toString();
    }

    public boolean hasMonitoredEpoch(int epochIndex) {
        return firstEpochIndex >= 0 && firstEpochIndex <= epochIndex;
    }

    /**
     * @return amount of oracles in the assembly
     * */
    public int size() {
        return oracleReaders.size();
    }

    int[] getRatings() {
        return ratings == null ? new int[oracleReaders.size()] : ratings;
    }

    public QubicReader getQubicReader() {
        return qubicReader;
    }

    public ConsensusBuilder getConsensusBuilder() {
        return consensusBuilder;
    }
}
