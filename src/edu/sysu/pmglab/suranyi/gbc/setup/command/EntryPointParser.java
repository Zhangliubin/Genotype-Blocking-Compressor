package edu.sysu.pmglab.suranyi.gbc.setup.command;

import edu.sysu.pmglab.suranyi.commandParser.CommandMatcher;
import edu.sysu.pmglab.suranyi.commandParser.CommandParser;
import edu.sysu.pmglab.suranyi.commandParser.converter.array.StringArrayConverter;
import edu.sysu.pmglab.suranyi.commandParser.converter.value.PassedInConverter;

import static edu.sysu.pmglab.suranyi.commandParser.CommandOptions.HELP;
import static edu.sysu.pmglab.suranyi.commandParser.CommandOptions.HIDDEN;
import static edu.sysu.pmglab.suranyi.commandParser.CommandRuleType.REQUEST_ONE;

enum EntryPointParser {
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

    EntryPointParser() {
        // global options
        parser = new CommandParser(false);
        parser.setProgramName("GBC [mode/tool]");
        parser.offset(0);
        parser.debug(false);
        parser.usingAt(true);
        parser.registerGlobalRule(REQUEST_ONE);

        // add commandItems
        parser.register("--help", "-help", "-h")
              .addOptions(HIDDEN, HELP)
              .arity(0)
              .convertTo(new PassedInConverter())
              .setOptionGroup("Options");
        parser.register("build")
              .arity(-1)
              .convertTo(new StringArrayConverter())
              .setOptionGroup("Mode")
              .setDescription("Compress and build *.gtb for vcf/vcf.gz files.");
        parser.register("rebuild")
              .arity(-1)
              .convertTo(new StringArrayConverter())
              .setOptionGroup("Mode")
              .setDescription("Rebuild *.gtb with new parameters.");
        parser.register("extract")
              .arity(-1)
              .convertTo(new StringArrayConverter())
              .setOptionGroup("Mode")
              .setDescription("Retrieve variant from *.gtb file.");
        parser.register("edit")
              .arity(-1)
              .convertTo(new StringArrayConverter())
              .setOptionGroup("Mode")
              .setDescription("Edit the GTB file directly.");
        parser.register("merge")
              .arity(-1)
              .convertTo(new StringArrayConverter())
              .setOptionGroup("Mode")
              .setDescription("Merge multiple GTB files (with non-overlapping subject sets) into a single GTB file.");
        parser.register("show")
              .arity(-1)
              .convertTo(new StringArrayConverter())
              .setOptionGroup("Mode")
              .setDescription("Display summary of the GTB File.");
        parser.register("index")
              .arity(-1)
              .convertTo(new StringArrayConverter())
              .setOptionGroup("Mode")
              .setDescription("Index contig file for specified VCF file or reset contig file for specified GTB file.");
        parser.register("ld")
              .arity(-1)
              .convertTo(new StringArrayConverter())
              .setOptionGroup("Mode")
              .setDescription("Calculate pairwise the linkage disequilibrium or genotypic correlation.");
        parser.register("version")
              .arity(-1)
              .convertTo(new StringArrayConverter())
              .setOptionGroup("Mode")
              .setDescription("Version of current GBC.");
        parser.register("bgzip")
              .arity(-1)
              .convertTo(new StringArrayConverter())
              .setOptionGroup("Tool")
              .setDescription("Use parallel bgzip to compress a single file.");
        parser.register("workflow")
                .arity(-1)
                .convertTo(new StringArrayConverter())
                .setOptionGroup("Tool")
                .setDescription("Utilities set developed based on GTB.");
        parser.register("md5")
              .arity(-1)
              .convertTo(new StringArrayConverter())
              .setOptionGroup("Tool")
              .setDescription("Calculate a message-digest fingerprint (checksum) for file.");
    }
}
