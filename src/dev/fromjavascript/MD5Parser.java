package dev.fromjavascript;

import edu.sysu.pmglab.suranyi.commandParser.CommandMatcher;
import edu.sysu.pmglab.suranyi.commandParser.CommandParser;
import edu.sysu.pmglab.suranyi.commandParser.converter.value.PassedInConverter;
import edu.sysu.pmglab.suranyi.commandParser.converter.value.StringConverter;
import edu.sysu.pmglab.suranyi.commandParser.validator.EnsureFileExistsValidator;
import edu.sysu.pmglab.suranyi.commandParser.validator.EnsureFileIsNotDirectoryValidator;

import static edu.sysu.pmglab.suranyi.commandParser.CommandOptions.*;

enum MD5Parser {
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

    MD5Parser() {
        // global options
        parser = new CommandParser(false);
        parser.setProgramName("bgzip md5 <file>");
        parser.offset(1);
        parser.debug(false);
        parser.registerGlobalRule(null);

        // add commandItems
        parser.register("--help", "-help", "-h")
              .addOptions(HIDDEN, HELP)
              .arity(0)
              .convertTo(new PassedInConverter() {})
              .setOptionGroup("Options");
        parser.register("md5")
              .addOptions(REQUEST, HIDDEN)
              .arity(1)
              .convertTo(new StringConverter() {})
              .validateWith(EnsureFileExistsValidator.INSTANCE, EnsureFileIsNotDirectoryValidator.INSTANCE)
              .setOptionGroup("Options");
    }
}
