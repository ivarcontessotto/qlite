package iam;

import constants.TangleJSONConstants;
import exceptions.IncompleteIAMChainException;
import iam.exceptions.IllegalIAMPacketSizeException;
import org.apache.commons.lang3.StringUtils;
import org.iota.jota.model.Transaction;
import org.iota.jota.utils.TrytesConverter;
import org.json.JSONException;
import org.json.JSONObject;
import tangle.TangleAPI;
import tangle.TryteTool;

import java.util.LinkedList;
import java.util.List;

class IAMPacketFilter {

    private boolean used = false;
    private final IAMReader iamReader;
    private final IAMIndex index;
    private List<Transaction> selection = null;

    IAMPacketFilter(IAMReader iamReader, IAMIndex index) {
        this.iamReader = iamReader;
        this.index = index;
    }

    void setSelection(List<Transaction> selection) {
        this.selection = selection;
    }

    LinkedList<IAMPacket> findAllValidIAMPackets() {
        preventObjectReuse();
        fetchSelectionIfItIsNull();
        return findAllValidIAMPacketsFromSelection();
    }

    private void fetchSelectionIfItIsNull() {
        String addressOfIndex = iamReader.buildAddress(index);
        if(selection == null)
            selection = TangleAPI.getInstance().findTransactionsByAddresses(new String[]{addressOfIndex});
    }

    private void preventObjectReuse() {
        if(used) throw new IllegalStateException("findAllValidIAMPackets() can only be called once per object lifetime");
        used = true;
    }

    private LinkedList<IAMPacket> findAllValidIAMPacketsFromSelection() {
        LinkedList<IAMPacket> validIAMPackets = new LinkedList<>();
        for(Transaction transaction : selection)
            appendTransactionIfValidIAMPacket(validIAMPackets, transaction);
        return validIAMPackets;
    }

    private void appendTransactionIfValidIAMPacket(LinkedList<IAMPacket> validIAMPackets, Transaction transaction) {
        IAMPacket iamPacket = fetchIAMPacket(transaction);
        if(iamReader.isValidIAMPacket(index, iamPacket)) {
            validIAMPackets.add(iamPacket);
        }
    }

    private IAMPacket fetchIAMPacket(Transaction rootTransaction) {
        try {
            String iamPacketJSONString = collectFragments(rootTransaction);
            return iamPacketJSONString != null ? new IAMPacket(new JSONObject(iamPacketJSONString)) : null;
        } catch (IncompleteIAMChainException | JSONException e) {
            return null;
        }
    }

    private String collectFragments(Transaction rootTransaction) {

        String baseTxMsg = TrytesConverter.trytesToAscii(rootTransaction.getSignatureFragments().substring(0, TryteTool.TRYTES_PER_TRANSACTION_MESSAGE -1));
        String[] split = SplitOffHashBlockIfFirstFragmentOfPacket(baseTxMsg);
        String hashBlock = split[0];
        String firstFragment = split[1];

        if (hashBlock == null) {
            return null;
        }

        String[] hashes = convertHashBlockToHashes(split[0]);
        if(hashes.length+1 > IAMStream.MAX_FRAGMENTS_PER_IAM_PACKET)
            throw new IllegalIAMPacketSizeException(rootTransaction.getHash());
        return firstFragment + fetchFragmentsFromHashes(hashes);
    }

    private static String[] SplitOffHashBlockIfFirstFragmentOfPacket(String baseTxMsg) {
        String[] split = new String[2];
        JSONObject hashBlockJson = null;
        int indexOfFirstClosingBracket = baseTxMsg.indexOf("}");
        if (baseTxMsg.startsWith("{") && indexOfFirstClosingBracket != -1) {
            String hashBlockSubstring = baseTxMsg.substring(0, indexOfFirstClosingBracket + 1);
            hashBlockJson = new JSONObject(hashBlockSubstring);
            if (hashBlockJson.has(TangleJSONConstants.IAM_PACKET_HASHBLOCK)) {
                split[0] = hashBlockJson.getString(TangleJSONConstants.IAM_PACKET_HASHBLOCK);
            }
        }

        if (split[0] != null) {
            split[1] = baseTxMsg.substring(hashBlockJson.toString().length());
        } else {
            split[1] = baseTxMsg;
        }

        return split;
    }

    private String fetchFragmentsFromHashes(String[] hashes) {
        StringBuilder fetchedMessage = new StringBuilder();
        for(String hash : hashes)
            fetchedMessage.append(TangleAPI.getInstance().readTransactionMessage(hash));
        return fetchedMessage.toString();
    }

    private String[] convertHashBlockToHashes(String hashBlock) {
        return hashBlock.isEmpty()
                ? new String[]{}
                : hashBlock.split("(?<=\\G.{"+TryteTool.TRYTES_PER_ADDRESS+"})"); // splits string into segments of length 81
    }
}

class IAMPacket {
    private final JSONObject message;
    private final String signature;

    IAMPacket(JSONObject iamPacketJSON) {
        signature = iamPacketJSON.getString(TangleJSONConstants.IAM_PACKET_SIGNATURE);
        message = iamPacketJSON.getJSONObject(TangleJSONConstants.IAM_PACKET_MESSAGE);
    }

    public JSONObject getMessage() {
        return message;
    }

    String getSignature() {
        return signature;
    }

    @Override
    public boolean equals(Object o) {
        if(o instanceof IAMPacket) {
            IAMPacket comparePacket = (IAMPacket)o;
            return comparePacket.getMessage().toString().equals(message.toString());
        } else {
            return false;
        }
    }
}