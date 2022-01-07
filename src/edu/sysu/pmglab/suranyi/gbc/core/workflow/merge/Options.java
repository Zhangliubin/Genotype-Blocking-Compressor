package edu.sysu.pmglab.suranyi.gbc.core.workflow.merge;

import edu.sysu.pmglab.suranyi.check.Assert;
import edu.sysu.pmglab.suranyi.commandParser.CommandParser;
import edu.sysu.pmglab.suranyi.commandParser.CommandMatcher;
import edu.sysu.pmglab.suranyi.commandParser.converter.array.*;
import edu.sysu.pmglab.suranyi.commandParser.converter.value.*;
import edu.sysu.pmglab.suranyi.easytools.ByteCode;


import java.util.HashMap;

import static edu.sysu.pmglab.suranyi.commandParser.CommandOptions.*;

enum Options {
    /**
     * single instance
     */
    INSTANCE;

    final CommandParser parser;

    /**
     * 变量
     */
    static String[] inputFileNames;
    static String outputDir;
    static int fileNums;
    static boolean phased;

    static byte[] missAllele = new byte[]{ByteCode.PERIOD};
    static HashMap<Integer, Integer> complementaryBase = new HashMap<>();

    static {
        int A = 65;
        int T = 84;
        int C = 67;
        int G = 71;
        complementaryBase.put(A, T);
        complementaryBase.put(T, A);
        complementaryBase.put(C, G);
        complementaryBase.put(G, C);
    }

    public static CommandParser getParser() {
        return INSTANCE.parser;
    }

    public static CommandMatcher parse(String... args) {
        CommandMatcher options = INSTANCE.parser.parse(args);
        Assert.that(((String[]) options.get("--input")).length >= 2);

        for (String input : ((String[]) options.get("--input"))) {
            Assert.that(!input.equals(options.get("--output")));
        }

        inputFileNames = ((String[]) options.get("--input"));
        outputDir = ((String) options.get("--output"));
        fileNums = inputFileNames.length;
        phased = options.isPassedIn("--phased");
        return options;
    }

    public static void toFile(String fileName) {
        INSTANCE.parser.toFile(fileName);
    }

    Options() {
        // global options
        parser = new CommandParser(false);
        parser.setProgramName("<main class>");
        parser.offset(0);
        parser.debug(false);
        parser.usingAt(true);
        parser.registerGlobalRule(null);

        // add commandItems
        parser.register("--help", "-help", "-h")
                .addOptions(HIDDEN, HELP)
                .arity(0)
                .convertTo(new PassedInConverter())
                .setOptionGroup("Options");
        parser.register("--input", "-i")
                .addOptions(REQUEST)
                .arity(-1)
                .convertTo(new StringArrayConverter())
                .setOptionGroup("Options");
        parser.register("--output", "-o")
                .addOptions(REQUEST)
                .arity(1)
                .convertTo(new StringConverter())
                .setOptionGroup("Options")
                .setDescription("Set the workspace.");
        parser.register("--phased", "-p")
                .arity(0)
                .convertTo(new PassedInConverter())
                .setOptionGroup("Compressor Options")
                .setDescription("Set the status of genotype to phased.");
    }
}
