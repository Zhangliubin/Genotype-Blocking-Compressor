package edu.sysu.pmglab.suranyi.gbc.setup.command;

import edu.sysu.pmglab.suranyi.commandParser.CommandMatcher;
import edu.sysu.pmglab.suranyi.commandParser.CommandParser;
import edu.sysu.pmglab.suranyi.commandParser.converter.array.StringArrayConverter;
import edu.sysu.pmglab.suranyi.commandParser.converter.value.PassedInConverter;
import edu.sysu.pmglab.suranyi.commandParser.converter.value.StringConverter;
import edu.sysu.pmglab.suranyi.commandParser.validator.EnsureFileExistsValidator;
import edu.sysu.pmglab.suranyi.commandParser.validator.EnsureFileIsNotDirectoryValidator;
import edu.sysu.pmglab.suranyi.gbc.constant.ChromosomeInfo;

import static edu.sysu.pmglab.suranyi.commandParser.CommandOptions.*;
import static edu.sysu.pmglab.suranyi.commandParser.CommandRuleType.AT_MOST_ONE;

enum ShowParser {
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

    ShowParser() {
        // global options
        parser = new CommandParser(false);
        parser.setProgramName("show <input>");
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
        parser.register("show")
              .addOptions(REQUEST, HIDDEN)
              .arity(1)
              .convertTo(new StringConverter())
              .validateWith(EnsureFileExistsValidator.INSTANCE, EnsureFileIsNotDirectoryValidator.INSTANCE)
              .setOptionGroup("Options");
        parser.register("--contig")
                .arity(1)
                .convertTo(new StringConverter())
                .defaultTo(ChromosomeInfo.DEFAULT_FILE)
                .validateWith(EnsureFileExistsValidator.INSTANCE, EnsureFileIsNotDirectoryValidator.INSTANCE)
                .setOptionGroup("Options")
                .setDescription("Specify the corresponding contig file.")
                .setFormat("'--contig <file>'");
        parser.register("--list-md5")
              .arity(0)
              .convertTo(new PassedInConverter())
              .setOptionGroup("Options")
              .setDescription("Print the message-digest fingerprint (checksum) for file (which may take a long time to calculating for huge files).");
        parser.register("--list-baseInfo")
              .arity(0)
              .convertTo(new PassedInConverter())
              .setOptionGroup("Options")
              .setDescription("Print the basic information of the GTB file (by default, only print the state of phased and random access enable).");
        parser.register("--list-subject")
              .arity(0)
              .convertTo(new PassedInConverter())
              .setOptionGroup("Options")
              .setDescription("Print subjects names of the GTB file.");
        parser.register("--list-subject-only")
              .arity(0)
              .convertTo(new PassedInConverter())
              .setOptionGroup("Options")
              .setDescription("Print subjects names of the GTB file only.");
        parser.register("--list-tree")
              .arity(0)
              .convertTo(new PassedInConverter())
              .setOptionGroup("Options")
              .setDescription("Print information of the GTBTrees (chromosome only by default).");
        parser.register("--list-node")
              .arity(0)
              .convertTo(new PassedInConverter())
              .setOptionGroup("Options")
              .setDescription("Print information of the GTBNodes.");
        parser.register("--assign-chromosome")
              .arity(1)
              .convertTo(new StringArrayConverter(","))
              .setOptionGroup("Options")
              .setDescription("Print information of the specified chromosome nodes.")
              .setFormat("'--assign-chromosome <string>,<string>,...'");
        parser.register("--full", "-f")
              .arity(0)
              .convertTo(new PassedInConverter())
              .setOptionGroup("Options")
              .setDescription("Print all information of the GTB file (except md5).");

        // add commandRules
        parser.registerRule("--list-md5", "--list-subject-only", AT_MOST_ONE);
        parser.registerRule("--list-baseInfo", "--list-subject-only", AT_MOST_ONE);
        parser.registerRule("--list-subject", "--list-subject-only", AT_MOST_ONE);
        parser.registerRule("--list-subject-only", "--list-tree", AT_MOST_ONE);
        parser.registerRule("--list-subject-only", "--list-node", AT_MOST_ONE);
        parser.registerRule("--list-subject-only", "--assign-chromosome", AT_MOST_ONE);
        parser.registerRule("--list-subject-only", "--full", AT_MOST_ONE);
        parser.registerRule("--list-baseInfo", "--full", AT_MOST_ONE);
        parser.registerRule("--list-subject", "--full", AT_MOST_ONE);
        parser.registerRule("--list-tree", "--full", AT_MOST_ONE);
        parser.registerRule("--list-node", "--full", AT_MOST_ONE);
        parser.registerRule("--assign-chromosome", "--full", AT_MOST_ONE);
    }
}
