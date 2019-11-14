package tangle;

import java.util.ArrayList;
import java.util.List;

public class QubicPromotion {

    public static String StoreQubicAddressToTangleWithKeyword(String publishAddress, String qubicId, String keyword){
        return TangleAPI.getInstance().sendMessageWithTag(publishAddress, qubicId, keyword);
    }

    public static List<String> GetQubicAddressesByKeyword(String keyword){
        List<String> transactionMessages = TangleAPI.getInstance().findTransactionMessagesByKeyword(keyword, true);
        if (transactionMessages.isEmpty()) {
            return new ArrayList<>();
        }
        return transactionMessages;
    }
}

