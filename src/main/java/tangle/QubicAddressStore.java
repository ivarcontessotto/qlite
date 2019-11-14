package tangle;

import org.iota.jota.IotaAPI;
import org.iota.jota.builder.AddressRequest;
import org.iota.jota.dto.response.GetNewAddressResponse;
import org.iota.jota.dto.response.SendTransferResponse;
import org.iota.jota.model.Transaction;
import org.iota.jota.model.Transfer;
import org.iota.jota.utils.TrytesConverter;

import java.util.ArrayList;
import java.util.List;

public class QubicAddressStore {
    private IotaAPI api;
    private String seed = "NUJAEKXBZSMRNTRPMCD9PSXWDUJ9ZAWIQGKBKLLFAZNGSMBUGDSHLMZEAGREJARTWSKBVJSALXJLSSLEW";

    public QubicAddressStore(){
        this.api = new IotaAPI.Builder()
                .protocol("https")
                .host("nodes.devnet.thetangle.org")
                .port(443)
                .build();
    }

    public void GetAIotaMessage(){
        // keyword has to be short, no special characters
        String keyword = "WIPROFUNN";
        this.SendQubicAddressToTangleByKeyword("0123456789", keyword);
        String wiproTest = this.GetQubicAddressByKeyword(keyword);
    }

    public void SendQubicAddressToTangleByKeyword(String address, String keyword){
        int index = 0;
        AddressRequest addressRequest = new AddressRequest.Builder(seed, 2).index(index).amount(1).checksum(true).build();
        GetNewAddressResponse getNewAddressResponse = this.api.generateNewAddresses(addressRequest);

        String adress = getNewAddressResponse.getAddresses().get(0);
        String payload = TrytesConverter.asciiToTrytes(address);
        String tag = TrytesConverter.asciiToTrytes(keyword);

        List<Transfer> transfers = new ArrayList<Transfer>();
        transfers.add(new Transfer(adress, 0, payload, tag));
        SendTransferResponse str = api.sendTransfer(this.seed, 2, 4, 14, transfers, null, null, false, false, null);
    }

    public String GetQubicAddressByKeyword(String keyword){
        String tag = TrytesConverter.asciiToTrytes(keyword);
        List<Transaction> transactions =  this.api.findTransactionObjectsByTag(tag);
        Transaction transaction = transactions.get(0);
        return TrytesConverter.trytesToAscii(transaction.getSignatureFragments().split("9999")[0]);
    }
}

