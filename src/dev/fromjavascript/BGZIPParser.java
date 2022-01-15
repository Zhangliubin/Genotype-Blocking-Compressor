// TODO: package path
package dev.fromjavascript;

import edu.sysu.pmglab.suranyi.commandParser.CommandMatcher;
import edu.sysu.pmglab.suranyi.commandParser.CommandParser;
import edu.sysu.pmglab.suranyi.commandParser.converter.array.StringArrayConverter;
import edu.sysu.pmglab.suranyi.commandParser.converter.value.PassedInConverter;
import edu.sysu.pmglab.suranyi.commandParser.converter.value.StringConverter;

import static edu.sysu.pmglab.suranyi.commandParser.CommandOptions.HELP;
import static edu.sysu.pmglab.suranyi.commandParser.CommandOptions.HIDDEN;
import static edu.sysu.pmglab.suranyi.commandParser.CommandRuleType.REQUEST_ONE;

enum BGZIPParser {
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

    BGZIPParser() {
        // global options
        parser = new CommandParser(false);
        parser.setProgramName("bgzip <mode>");
        parser.offset(1);
        parser.debug(false);
        parser.registerGlobalRule(REQUEST_ONE);

        // add commandItems
        parser.register("--help", "-help", "-h")
              .addOptions(HIDDEN, HELP)
              .arity(0)
              .convertTo(new PassedInConverter() {})
              .setOptionGroup("Options");
        parser.register("compress")
              .arity(-1)
              .convertTo(new StringArrayConverter() {})
              .setOptionGroup("Options")
              .setDescription("Compression using parallel-bgzip (supported by CLM algorithm).")
              .setFormat("compress <file>");
        parser.register("convert")
              .arity(-1)
              .convertTo(new StringArrayConverter() {})
              .setOptionGroup("Options")
              .setDescription("Convert *.gz format to *.bgz format.")
              .setFormat("convert <file>");
        parser.register("decompress")
              .arity(-1)
              .convertTo(new StringArrayConverter() {})
              .setOptionGroup("Options")
              .setDescription("Decompression.")
              .setFormat("decompress <file>");
        parser.register("extract")
              .arity(-1)
              .convertTo(new StringArrayConverter() {})
              .setOptionGroup("Options")
              .setDescription("Cut the bgzip file by pointer range (decompressed file).")
              .setFormat("extract <file> -r <start>-<end>");
        parser.register("concat")
              .arity(-1)
              .convertTo(new StringArrayConverter() {})
              .setOptionGroup("Options")
              .setDescription("Concatenate multiple files.")
              .setFormat("concat <file> <file> ...");
        parser.register("md5")
              .arity(1)
              .convertTo(new StringConverter() {})
              .setOptionGroup("Options")
              .setDescription("Calculate a message-digest fingerprint (checksum) for decompressed file.")
              .setFormat("md5 <file>");
    }
}
