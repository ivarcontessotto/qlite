package oracle;

import constants.GeneralConstants;
import iam.IAMIndex;
import iam.IAMReader;
import oracle.statements.result.ResultStatement;
import org.json.JSONObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ConsensusBuilder {

    private final static Logger LOGGER = LogManager.getLogger(ConsensusBuilder.class);

    private static final double QUORUM_MIN = 2D/3D;
    private final Assembly assembly;

    private final Map<Integer, QuorumBasedResult> alreadyDeterminedQuorumBasedResults = new HashMap<>();

    public ConsensusBuilder(Assembly assembly) {
        this.assembly = assembly;
    }

    /**
     * Determines the quorum based result for a specific epoch.
     * @param epochIndex index of the epoch for which the result shall be determined
     * @return quorum based result
     * */
    public QuorumBasedResult buildConsensus(int epochIndex) {
        return buildConsensus(null, epochIndex);
    }

    /**
     * Determines the quorum based result for a specific epoch.
     * @param selection  a selection of the whole assembly based on which the quorum will be determined probabilisticly
     * @param epochIndex index of the epoch for which the result shall be determined
     * @return quorum based result
     * */
    public QuorumBasedResult buildConsensus(List<OracleReader> selection, int epochIndex) {
        LOGGER.debug("Build Consensus");

        if(selection == null)
            selection = assembly.selectRandomOracleReaders(GeneralConstants.QUORUM_MAX_ORACLE_SELECTION_SIZE);

        // if epoch is ongoing or hasn't even started yet
        if(epochIndex < 0 || epochIndex > assembly.getQubicReader().lastCompletedEpoch()) {
            LOGGER.debug("Consensus not ready yet. Epoch not finished");
            return new QuorumBasedResult(0, selection.size(),null);
        }

        // return result from history if already determined -> increases efficiency
        if(alreadyDeterminedQuorumBasedResults.keySet().contains(epochIndex)) {
            return alreadyDeterminedQuorumBasedResults.get(epochIndex);
        }

        // empty assembly
        if(selection.size() == 0) {
            LOGGER.debug("Consensus null because assembly size 0");
            return new QuorumBasedResult(0, 0, null);
        }

        // determine result
        QuorumBasedResult quorumBasedResult = findVotingQuorum(accumulateEpochVotings(selection, epochIndex), selection.size());

        // add result to list of already known results
        alreadyDeterminedQuorumBasedResults.put(epochIndex, quorumBasedResult);

        LOGGER.debug("Quorum Bassed Result is: " + quorumBasedResult.getResult());
        return quorumBasedResult;
    }

    public boolean hasAlreadyDeterminedQuorumBasedResult(int epochIndex) {
        return alreadyDeterminedQuorumBasedResults.containsKey(epochIndex);
    }

    public QuorumBasedResult buildIAMConsensus(IAMIndex index) {
        List<IAMReader> selection = assembly.selectRandomOracleReaders(GeneralConstants.QUORUM_MAX_ORACLE_SELECTION_SIZE)
                .stream()
                .map(OracleReader::getReader)
                .collect(Collectors.toList());

        logSelectedReaders(selection);
        return findVotingQuorum(accumulateIAMVotings(selection, index), selection.size());
    }

    private void logSelectedReaders(List<IAMReader> readers) {
        StringBuilder sb = new StringBuilder("Randomly selected Readers for consensus:");
        readers.forEach(r -> sb.append("\n" + r.getID()));
        LOGGER.debug(sb.toString());
    }

    private static Map<String, Double> accumulateEpochVotings(List<OracleReader> voters, int epochIndex) {
        Map<String, Double> quorumVoting = new HashMap<>();
        for(OracleReader oracleReader : voters)
            addOraclesVoteToVoting(oracleReader, epochIndex, quorumVoting);
        return quorumVoting;
    }

    private static Map<String, Double> accumulateIAMVotings(List<IAMReader> voters, IAMIndex index) {
        Map<String, Double> quorumVoting = new HashMap<>();
        for(IAMReader voter : voters) {
            JSONObject vote = voter.read(index);
            addVote(quorumVoting, vote.toString());
        }
        return quorumVoting;
    }

    private static void addOraclesVoteToVoting(OracleReader oracleReader, int epochIndex, Map<String, Double> voting) {
        oracleReader.getHashStatementReader().read(epochIndex);
        ResultStatement resultStatement = oracleReader.getResultStatementReader().read(epochIndex);

        if(resultStatement != null && resultStatement.isHashStatementValid())
            addVote(voting, resultStatement.getContent());
    }

    private static void addVote(Map<String, Double> quorumVoting, String votedFor) {
        double count = quorumVoting.containsKey(votedFor) ? quorumVoting.get(votedFor) : 0;
        quorumVoting.put(votedFor, count+1); // TODO allow individual voting weights
    }

    private static QuorumBasedResult findVotingQuorum(Map<String, Double> voting, double totalVotesAllowed) {

        // init score variables
        String highScoreResult = null;
        double highScore = 0;

        // search for result with highest voting score
        for(String result : voting.keySet()) {
            double score = voting.get(result);

            // new high score result found?
            if(score > highScore) {
                highScoreResult = result;
                highScore = score;
            }
        }

        // return result with highest score or NULL if it doesn't have at least 2/3 of votes
        highScoreResult = highScore >= totalVotesAllowed * QUORUM_MIN ? highScoreResult : null;

        return new QuorumBasedResult(highScore, totalVotesAllowed, highScoreResult);
    }
}
