package org.qubiclite.qlite.qlvm.exceptions.runtime;

import org.qubiclite.qlite.constants.GeneralConstants;

public class QLValueMaxLengthExceeded extends QLRunTimeException {

    public QLValueMaxLengthExceeded(String val) {
        super("value exceeded maximum length of "+ GeneralConstants.QLVM_MAX_VALUE_LENGTH+" characters: " + val.substring(0, 100) + "...");
    }
}
