package dev.fromcommandfile;

import edu.sysu.pmglab.suranyi.commandParser.CommandMatcher;
import edu.sysu.pmglab.suranyi.commandParser.CommandParser;

enum ExtractParser {
    /**
     * single instance
     */
    INSTANCE;

    final CommandParser parser;

    public static CommandParser getParser() {
        return INSTANCE.parser;
    }

    public static CommandMatcher parse(String... args) {
        return INSTANCE.parser.parse(args);
    }

    public static void toFile(String fileName) {
        INSTANCE.parser.toFile(fileName);
    }

    ExtractParser() {
        CommandParser tempParser;
        try {
            tempParser = CommandParser.loadFromInnerResource(ExtractParser.class, "/dev/fromcommandfile/command/extract.cp");
        } catch (Exception ignored) {
            parser = null;
            return;
        }

        parser = tempParser;
    }
}
