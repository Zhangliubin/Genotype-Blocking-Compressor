package dev.fromcommandfile;

import edu.sysu.pmglab.suranyi.commandParser.CommandMatcher;
import edu.sysu.pmglab.suranyi.commandParser.CommandParser;

enum ConvertParser {
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

    ConvertParser() {
        CommandParser tempParser;
        try {
            tempParser = CommandParser.loadFromInnerResource(ConvertParser.class, "/dev/fromcommandfile/command/convert.cp");
        } catch (Exception ignored) {
            parser = null;
            return;
        }

        parser = tempParser;
    }
}
