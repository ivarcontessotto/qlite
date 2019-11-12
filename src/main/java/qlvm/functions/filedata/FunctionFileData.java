package qlvm.functions.filedata;

import qlvm.QLVM;
import qlvm.functions.Function;

import java.io.*;

public class FunctionFileData extends Function {
    @Override
    public String getName() { return "filedata"; }

    @Override
    public String call(QLVM qlvm, String[] par) {
        String fileName = par[0].substring(1, par[0].length() - 1);
        BufferedReader reader;
        try {
            reader = new BufferedReader(new FileReader(fileName));
            return reader.readLine();
        } catch(IOException io){
            return "0";
        }
    }
}
