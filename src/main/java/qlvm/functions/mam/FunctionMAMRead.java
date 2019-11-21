package qlvm.functions.mam;

import iam.IAMIndex;
import org.iota.jota.model.Transaction;
import org.iota.jota.utils.TrytesConverter;
import org.json.JSONObject;
import qlvm.QLVM;
import qlvm.functions.Function;
import iam.IAMReader;
import tangle.TangleAPI;

import java.util.List;

public class FunctionMAMRead extends Function {

    private String lastAddress = "LEDRMPFVZRVFSWPNZJT9BANEFOZHZWWWYRRORBQMGQSLMTVZSBCUGSSNWDURFQXQHMCDRXLELZCDJZXTWXNOORDKSC";

    @Override
    public String getName() { return "mam_read"; }

    @Override
    public String call(QLVM qlvm, String[] par) {
        String address = this.lastAddress;
        int result = 0;

        List<Transaction> transactions;

        do {
            transactions = TangleAPI.getInstance().findTransactionsByAddresses(new String[]{address});
            if (transactions.size() > 0){
                this.lastAddress = address;

                String message = TrytesConverter.trytesToAscii(transactions.get(0).getSignatureFragments().split("9999")[0]);
                JSONObject object = new JSONObject(message);
                address = object.getString("next_root");
                result = object.getInt("temp");
            }
            else {
                return Integer.toString(result);
            }
        } while (transactions.size() > 0);

        return Integer.toString(result);
    }
}