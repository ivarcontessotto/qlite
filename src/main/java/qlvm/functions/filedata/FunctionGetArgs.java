package qlvm.functions.filedata;

import qlvm.QLVM;
import qlvm.functions.Function;

import java.io.*;
import java.nio.file.Files;
import java.util.List;

public class FunctionGetArgs extends Function {
    @Override
    public String getName() { return "getargs"; }

    @Override
    public String call(QLVM qlvm, String[] par) {
        if (par.length != 1 || qlvm.getArgsFilePath() == null) {
            return null;
        }

        int argsIndex = Integer.parseInt(par[0]);

        try {
            List<String> argLines = Files.readAllLines(qlvm.getArgsFilePath().toAbsolutePath());
            if (argLines.size() > argsIndex) {
                return argLines.get(argsIndex);
            }
            else {
                return null;
            }
        } catch(IOException io){
            return null;
        }
    }
}
