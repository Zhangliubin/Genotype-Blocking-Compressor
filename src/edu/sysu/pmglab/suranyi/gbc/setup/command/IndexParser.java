package edu.sysu.pmglab.suranyi.gbc.setup.command;

import edu.sysu.pmglab.suranyi.commandParser.CommandMatcher;
import edu.sysu.pmglab.suranyi.commandParser.CommandParser;
import edu.sysu.pmglab.suranyi.commandParser.converter.value.PassedInConverter;
import edu.sysu.pmglab.suranyi.commandParser.converter.value.StringConverter;
import edu.sysu.pmglab.suranyi.commandParser.validator.EnsureFileExistsValidator;
import edu.sysu.pmglab.suranyi.commandParser.validator.EnsureFileIsNotDirectoryValidator;
import edu.sysu.pmglab.suranyi.gbc.constant.ChromosomeInfo;

import static edu.sysu.pmglab.suranyi.commandParser.CommandOptions.*;

enum IndexParser {
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

    IndexParser() {
        // global options
        parser = new CommandParser(false);
        parser.setProgramName("index <input>");
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
        parser.register("index")
                .addOptions(REQUEST, HIDDEN)
                .arity(1)
                .convertTo(new StringConverter())
                .validateWith(EnsureFileExistsValidator.INSTANCE, EnsureFileIsNotDirectoryValidator.INSTANCE)
                .setOptionGroup("Options");
        parser.register("--output", "-o")
                .arity(1)
                .convertTo(new StringConverter())
                .setOptionGroup("Options")
                .setDescription("Set the output file.")
                .setFormat("'-o <file>'");
        parser.register("--from-contig", "-from")
                .arity(1)
                .convertTo(new StringConverter())
                .defaultTo(ChromosomeInfo.DEFAULT_FILE)
                .validateWith(EnsureFileExistsValidator.INSTANCE, EnsureFileIsNotDirectoryValidator.INSTANCE)
                .setOptionGroup("Options")
                .setDescription("Specify the corresponding contig file.")
                .setFormat("'-from <file>'");
        parser.register("--to-contig", "-to")
                .arity(1)
                .convertTo(new StringConverter())
                .validateWith(EnsureFileExistsValidator.INSTANCE, EnsureFileIsNotDirectoryValidator.INSTANCE)
                .setOptionGroup("Options")
                .setDescription("Reset contig (chromosome marker in each gtb block header) for gtb file directly.")
                .setFormat("'-to <file>'");
        parser.register("--yes", "-y")
                .arity(0)
                .convertTo(new PassedInConverter())
                .setOptionGroup("Options")
                .setDescription("Overwrite output file without asking.");
    }
}
