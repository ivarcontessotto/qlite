package tangle;

import org.iota.jota.model.Transaction;

import java.util.Comparator;


public class TransactionTimestampComparator implements Comparator<Transaction> {

    @Override
    public int compare(Transaction first, Transaction second) {
        if (first.getTimestamp() < second.getTimestamp()) {
            return -1;
        }
        else if (first.getTimestamp() > second.getTimestamp()) {
            return 1;
        }
        else {
            return 0;
        }
    }
}