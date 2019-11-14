package tangle;

import org.iota.jota.connection.HttpConnector;
import org.iota.jota.pow.pearldiver.PearlDiverLocalPoW;
import org.iota.jota.IotaAPI;
import org.iota.jota.dto.response.GetBalancesResponse;
import org.iota.jota.dto.response.SendTransferResponse;
import org.iota.jota.error.ArgumentException;
import org.iota.jota.model.Transaction;
import org.iota.jota.model.Transfer;
import org.iota.jota.utils.TrytesConverter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.MalformedURLException;
import java.security.InvalidParameterException;
import java.util.*;

/**
 * @author microhash
 *
 * The TangleAPI is the interface between the QLite library and the
 * IotaAPI of the iota library. It takes care of all tangle requests.
 * */
public class TangleAPI {

    private static TangleAPI instance = new TangleAPI(new NodeAddress("https://nodes.devnet.thetangle.org:443"), 9, true);

    private static final String TAG = "QLITE9999999999999999999999";

    private final Logger logger = LogManager.getLogger(TangleAPI.class);
    private final IotaAPI wrappedAPI;
    private int mwm;

    public static TangleAPI getInstance() {
        return instance;
    }

    /**
     * Changes the node used
     * @param nodeAddress address of the node to connect to
     * @param mwm         min weight magnitude (14 on mainnet, 9 on testnet)
     * @param localPow    TRUE: perform proof-of-work locally, FALSE: perform pow on remote iota node
     * */
    public static void changeNode(NodeAddress nodeAddress, int mwm, boolean localPow) throws MalformedURLException {
        instance = new TangleAPI(nodeAddress, mwm, localPow);
    }

    private TangleAPI(NodeAddress nodeAddress, int mwm, boolean localPow) {

        IotaAPI.Builder builder = new IotaAPI.Builder();

        try {
            builder.addNode(new HttpConnector(
                    nodeAddress.getProtocol(),
                    nodeAddress.getHost(),
                    nodeAddress.getPort(), 500));
        } catch (MalformedURLException e) {
            throw new ArgumentException("Malformed node url.");
        }

        if(localPow)
            builder.localPoW(new PearlDiverLocalPoW());

        wrappedAPI = new WrappedIotaAPI(builder, 3);
        this.mwm = mwm;
    }

    /**
     * Sends a data transaction to the tangle. Keeps trying until there is no error.
     * @param address the address to which the transaction shall be attached
     * @param tryteMessage the transaction message (in trytes)
     * @return transaction hash of sent transaction
     * */
    public String sendTrytes(String address, String tryteMessage) {

        List<Transfer> transfers = new LinkedList<>();
        transfers.add(new Transfer(address, 0, tryteMessage, TAG));

        while (true) {
            try {
                logger.debug("Send Trytes\nAddress: " + address + "\nMessage: " + tryteMessage);
                SendTransferResponse response = wrappedAPI.sendTransfer("", 1, 3, mwm, transfers,
                        null, null, true, false, null);
                return response.getTransactions().get(0).getHash();
            } catch (ArgumentException e) {
                e.printStackTrace();
                return null;
            } catch (NullPointerException e) {
                StackTraceElement ste = e.getStackTrace()[0];
                System.err.println("NullPointerException in file " + ste.getFileName() + " at line #" + ste.getLineNumber());
            }

            try { Thread.sleep(1000); } catch (InterruptedException e) { e.printStackTrace(); }
        }
    }

    public String sendTrytes(String tryteMessage) {
        return sendTrytes(TryteTool.TEST_ADDRESS_1, tryteMessage);
    }

    public String sendMessage(String message) {
        return sendMessage(TryteTool.TEST_ADDRESS_1, message);
    }

    public String sendMessage(String address, String message) {
        logger.debug("Send Message\nAddress: " + address + "\nMessage: " + message);
        return sendTrytes(address, TrytesConverter.asciiToTrytes(message));
    }

    /**
     * Finds all transactions published to a certain address.
     * @param addresses the addresses to check
     * @return hashes of found transactions
     * */
    public List<Transaction> findTransactionsByAddresses(String[] addresses) {
        try {
            this.logger.debug("Find transactions by addresses:" + getAddressLogLines(addresses));
            List<Transaction> foundTransactions = wrappedAPI.findTransactionObjectsByAddresses(addresses);
            this.logger.debug("Found transactions:" + getHashLogLines(foundTransactions));
            return foundTransactions;
        } catch (ArgumentException e) {
            logger.error("Error reading transactions by addresses", e);
        } catch (NullPointerException e) {
            logger.error("Error reading transactions by addresses", e);
        }
        return null;
    }

    /**
     * Reads the messages of all transaction published to a certain address.
     * @param preload resource of pre-fetched transactions for efficiency purposes, optional (set to null if not required)
     * @param address the address to check
     * @param convert convert the message trytes to ascii before returning?
     * @return transaction messages mapped by transaction hash of all transactions found
     * */
    public Map<String, String> readTransactionsByAddress(List<Transaction> preload, String address, boolean convert) {
        List<Transaction> transactions;
        if(preload != null) {
            logger.debug("Read transactions from preload by address: " + address);
            transactions = new LinkedList<>();
            for(Transaction t : preload) {
                if (t.getAddress().equals(address)) {
                    transactions.add(t);
                }
            }
            logger.debug("Found transactions:" + getHashLogLines(transactions));
        } else {
            transactions = findTransactionsByAddresses(new String[] {address});
        }

        Map<String, String> map = new HashMap<>();

        for(Transaction tx : transactions) {
            String trytes = tx.getSignatureFragments();
            trytes = trytes.split("99")[0];
            if(trytes.length()%2 == 1) trytes += "9";
            String message = convert ? TrytesConverter.trytesToAscii(trytes) : trytes;

            map.put(tx.getHash(), message);
        }
        return map;
    }

    public String readTransactionMessage(String hash) {
        logger.debug("Read Message of Transaction: " + hash);
        String transactionTrytes = readTransactionTrytes(hash);
        String message = transactionTrytes != null ? TrytesConverter.trytesToAscii(transactionTrytes) : null;
        logger.debug("Messsage found: " + message);
        return message;
    }

    /**
     * Finds the transaction with a certain hash.
     * @param hash    the hash of the requested transaction
     * @return transaction messages of the transaction found, NULL if not found
     * */
    public String readTransactionTrytes(String hash) {
        if(!TryteTool.isTryteSequence(hash))
            throw new InvalidParameterException("parameter hash is not a tryte sequence");
        if(hash.length() != 81)
            throw new InvalidParameterException("parameter hash is required to be exactly 81 trytes long");

        String[] hashes = {hash};
        List<Transaction> transactions = null;

        while (transactions == null) {
            try {
                logger.debug("Read Transaction by Hash: " + hash);
                transactions = wrappedAPI.findTransactionsObjectsByHashes(hashes);
            } catch (ArgumentException e) {
                e.printStackTrace();
                return null;
            } catch (NullPointerException e) {
                StackTraceElement ste = e.getStackTrace()[0];
                System.err.println("NullPointerException in file " + ste.getFileName() + " at line #" + ste.getLineNumber());
            }
        }

        // transaction not found
        if(transactions.size() == 0 || transactions.get(0).getHash().equals("999999999999999999999999999999999999999999999999999999999999999999999999999999999"))
            return null;

        String trytes = transactions.get(0).getSignatureFragments();
        // remove end
        trytes = trytes.substring(0, trytes.length()-1);
        trytes = trytes.split("99")[0];
        if(trytes.length()%2 == 1) trytes += "9";

        logger.debug("Found Transaction Trytes: " + trytes);
        return trytes;
    }

    /**
     * Requests the balance of a certain iota address.
     * @param address the address to check
     * @return the balance in iotas
     * */
    public long getBalance(String address) {
        LinkedList<String> addresses = new LinkedList<>();
        addresses.add(address);
        try {
            GetBalancesResponse balancesResponse = wrappedAPI.getBalances(1, addresses);
            return Long.parseLong(balancesResponse.getBalances()[0]);
        }
        catch (ArgumentException e) {
            e.printStackTrace();
            return -1;
        }
    }

    public int getMWM() {
        return mwm;
    }

    private String getAddressLogLines(String[] addresses) {
        StringBuilder stringBuilder = new StringBuilder();
        Arrays.stream(addresses).forEach(a -> stringBuilder.append("\n").append(a));
        return stringBuilder.toString();
    }

    private String getHashLogLines(List<Transaction> transactions) {
        StringBuilder stringBuilder = new StringBuilder();
        transactions.forEach(t -> stringBuilder.append("\n").append(t.getHash()));
        return stringBuilder.toString();
    }
}