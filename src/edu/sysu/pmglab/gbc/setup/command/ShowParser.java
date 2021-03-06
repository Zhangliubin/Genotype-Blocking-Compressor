package edu.sysu.pmglab.gbc.setup.command;

import edu.sysu.pmglab.gbc.constant.ChromosomeTags;
import edu.sysu.pmglab.commandParser.CommandMatcher;
import edu.sysu.pmglab.commandParser.CommandParser;
import edu.sysu.pmglab.commandParser.converter.array.StringArrayConverter;
import edu.sysu.pmglab.commandParser.converter.value.PassedInConverter;
import edu.sysu.pmglab.commandParser.converter.value.StringConverter;
import edu.sysu.pmglab.commandParser.validator.EnsureFileExistsValidator;
import edu.sysu.pmglab.commandParser.validator.EnsureFileIsNotDirectoryValidator;

import static edu.sysu.pmglab.commandParser.CommandOptions.*;
import static edu.sysu.pmglab.commandParser.CommandRuleType.AT_MOST_ONE;
import static edu.sysu.pmglab.commandParser.CommandRuleType.PRECONDITION;

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
                .defaultTo(ChromosomeTags.DEFAULT_FILE)
                .validateWith(EnsureFileExistsValidator.INSTANCE, EnsureFileIsNotDirectoryValidator.INSTANCE)
                .setOptionGroup("Common Options")
                .setDescription("Specify the corresponding contig file.")
                .setFormat("'--contig <file>'");
        parser.register("--assign-chromosome")
                .arity(1)
                .convertTo(new StringArrayConverter(","))
                .setOptionGroup("Common Options")
                .setDescription("Print information of the specified chromosome.")
                .setFormat("'--assign-chromosome <string>,<string>,...'");
        parser.register("--list-md5")
                .arity(0)
                .convertTo(new PassedInConverter())
                .setOptionGroup("Summary View Options")
                .setDescription("Print the message-digest fingerprint (checksum) for file (which may take a long time to calculating for huge files).");
        parser.register("--list-baseInfo")
                .arity(0)
                .convertTo(new PassedInConverter())
                .setOptionGroup("Summary View Options")
                .setDescription("Print the basic information of the GTB file (by default, only print the state of phased and random access enable).");
        parser.register("--list-subject")
                .arity(0)
                .convertTo(new PassedInConverter())
                .setOptionGroup("Summary View Options")
                .setDescription("Print subjects names of the GTB file.");
        parser.register("--list-tree")
                .arity(0)
                .convertTo(new PassedInConverter())
                .setOptionGroup("Summary View Options")
                .setDescription("Print information of the GTBTrees (chromosome only by default).");
        parser.register("--list-node")
                .arity(0)
                .convertTo(new PassedInConverter())
                .setOptionGroup("Summary View Options")
                .setDescription("Print information of the GTBNodes.");
        parser.register("--full", "-f")
                .arity(0)
                .convertTo(new PassedInConverter())
                .setOptionGroup("Summary View Options")
                .setDescription("Print all abstract information of the GTB file (i.e., --list-baseInfo, --list-subject, --list-node).");
        parser.register("--list-subject-only")
                .arity(0)
                .convertTo(new PassedInConverter())
                .setOptionGroup("GTB View Options")
                .setDescription("Print subjects names of the GTB file only.");
        parser.register("--list-position-only")
                .arity(0)
                .convertTo(new PassedInConverter())
                .setOptionGroup("GTB View Options")
                .setDescription("Print coordinates (i.e., CHROM,POSITION) of the GTB file only.");
        parser.register("--list-site")
                .arity(0)
                .convertTo(new PassedInConverter())
                .setOptionGroup("GTB View Options")
                .setDescription("Print coordinates, alleles and INFOs (i.e., CHROM,POSITION,REF,ALT,INFO) of the GTB file.");
        parser.register("--list-gt")
                .arity(0)
                .convertTo(new PassedInConverter())
                .setOptionGroup("GTB View Options")
                .setDescription("Print genotype frequency of the GTB file (used with '--list-site').");

        // add commandRules
        parser.registerRule("--list-subject-only", new String[]{"--list-md5", "--list-baseInfo", "--list-subject", "--list-tree", "--list-node", "--assign-chromosome", "--full"}, AT_MOST_ONE);
        parser.registerRule("--list-position-only", new String[]{"--list-md5", "--list-baseInfo", "--list-subject", "--list-tree", "--list-node", "--full", "--list-subject-only"}, AT_MOST_ONE);
        parser.registerRule("--list-site", new String[]{"--list-md5", "--list-baseInfo", "--list-subject", "--list-tree", "--list-node", "--full", "--list-subject-only", "--list-position-only"}, AT_MOST_ONE);
        parser.registerRule("--full", new String[]{"--list-baseInfo", "--list-subject", "--list-tree", "--list-node", "--assign-chromosome"}, AT_MOST_ONE);
        parser.registerRule("--list-site", "--list-gt", PRECONDITION);
    }
}
