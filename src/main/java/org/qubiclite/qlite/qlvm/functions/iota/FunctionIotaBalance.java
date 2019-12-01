package org.qubiclite.qlite.qlvm.functions.iota;

import org.qubiclite.qlite.qlvm.QLVM;
import org.qubiclite.qlite.qlvm.functions.Function;
import org.qubiclite.qlite.tangle.TangleAPI;

public class FunctionIotaBalance extends Function {

    @Override
    public String getName() { return "iota_balance"; }

    @Override
    public String call(QLVM qlvm, String[] par) {
        String address = qlvm.unescapeString(par[0]);
        return ""+ TangleAPI.getInstance().getBalance(address);
    }
}



