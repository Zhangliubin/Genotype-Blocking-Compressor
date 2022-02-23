package edu.sysu.pmglab.suranyi.gbc.setup.command;

import edu.sysu.pmglab.suranyi.commandParser.CommandMatcher;
import edu.sysu.pmglab.suranyi.commandParser.CommandParser;
import edu.sysu.pmglab.suranyi.commandParser.converter.array.StringArrayConverter;
import edu.sysu.pmglab.suranyi.commandParser.converter.map.KVConverter;
import edu.sysu.pmglab.suranyi.commandParser.converter.value.PassedInConverter;
import edu.sysu.pmglab.suranyi.commandParser.converter.value.StringConverter;
import edu.sysu.pmglab.suranyi.commandParser.exception.ParameterException;
import edu.sysu.pmglab.suranyi.commandParser.validator.EnsureFileExistsValidator;
import edu.sysu.pmglab.suranyi.commandParser.validator.EnsureFileIsNotDirectoryValidator;
import edu.sysu.pmglab.suranyi.gbc.constant.ChromosomeInfo;

import java.util.HashMap;

import static edu.sysu.pmglab.suranyi.commandParser.CommandOptions.*;
import static edu.sysu.pmglab.suranyi.commandParser.CommandRuleType.AT_MOST_ONE;

enum EditParser {
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

    EditParser() {
        // global options
        parser = new CommandParser(false);
        parser.setProgramName("edit <input>");
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
        parser.register("edit")
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
        parser.register("--output", "-o")
                .arity(1)
                .convertTo(new StringConverter())
                .setOptionGroup("Options")
                .setDescription("Set the output file.")
                .setFormat("'-o <file>'");
        parser.register("--unique")
                .arity(0)
                .convertTo(new PassedInConverter())
                .setOptionGroup("Options")
                .setDescription("Retain the unique GTBNodes.");
        parser.register("--delete")
                .arity(1)
                .convertTo(new KVConverter<String, int[]>("chrom", "node") {
                    @Override
                    public HashMap<String, int[]> convert(String... params) {
                        HashMap<String, String> KV = super.parseKV(params);
                        HashMap<String, int[]> result = new HashMap<>();

                        if (!KV.containsKey("chrom") || (KV.get("chrom") == null) || (KV.get("chrom").length() == 0)) {
                            throw new ParameterException("no chromosomes specified");
                        } else {
                            String[] chromosomes = KV.get("chrom").split(",");
                            if (KV.containsKey("node") && (KV.get("node") != null) && (KV.get("node").length() != 0)) {
                                String[] nodeIndexesStr = KV.get("node").split(",");
                                int[] nodeIndexes = new int[nodeIndexesStr.length];

                                for (int i = 0; i < nodeIndexes.length; i++) {
                                    nodeIndexes[i] = Integer.parseInt(nodeIndexesStr[i]);
                                }

                                for (String chromosome : chromosomes) {
                                    result.put(chromosome, nodeIndexes);
                                }
                            } else {
                                for (String chromosome : chromosomes) {
                                    result.put(chromosome, null);
                                }
                            }
                        }
                        return result;
                    }
                })
                .setOptionGroup("Options")
                .setDescription("Delete the specified GTBNodes.")
                .setFormat("'--delete chrom=<string>,<string>,...' or '--delete chrom=<string>,<string>,...;node=<int>,<int>,...'");
        parser.register("--retain")
                .arity(1)
                .convertTo(new KVConverter<String, int[]>("chrom", "node") {
                    @Override
                    public HashMap<String, int[]> convert(String... params) {
                        HashMap<String, String> KV = super.parseKV(params);
                        HashMap<String, int[]> result = new HashMap<>();

                        if (!KV.containsKey("chrom") || (KV.get("chrom") == null) || (KV.get("chrom").length() == 0)) {
                            throw new ParameterException("no chromosomes specified");
                        } else {
                            String[] chromosomes = KV.get("chrom").split(",");
                            if (KV.containsKey("node") && (KV.get("node") != null) && (KV.get("node").length() != 0)) {
                                String[] nodeIndexesStr = KV.get("node").split(",");
                                int[] nodeIndexes = new int[nodeIndexesStr.length];

                                for (int i = 0; i < nodeIndexes.length; i++) {
                                    nodeIndexes[i] = Integer.parseInt(nodeIndexesStr[i]);
                                }

                                for (String chromosome : chromosomes) {
                                    result.put(chromosome, nodeIndexes);
                                }
                            } else {
                                for (String chromosome : chromosomes) {
                                    result.put(chromosome, null);
                                }
                            }
                        }
                        return result;
                    }
                })
                .setOptionGroup("Options")
                .setDescription("Retain the specified GTBNodes.")
                .setFormat("'--retain chrom=<string>,<string>,...' or '--retain chrom=<string>,<string>,...;node=<int>,<int>,...'");
        parser.register("--concat")
                .arity(-1)
                .convertTo(new StringArrayConverter())
                .validateWith(EnsureFileExistsValidator.INSTANCE, EnsureFileIsNotDirectoryValidator.INSTANCE)
                .setOptionGroup("Options")
                .setDescription("Concatenate multiple VCF files. All source files must have the same subjects columns appearing in the same order with entirely different sites, and all files must have to be the same in parameters of the status")
                .setFormat("'--concat <file> <file> ...'");
        parser.register("--reset-subject")
                .arity(1)
                .convertTo(new StringArrayConverter(","))
                .setOptionGroup("Options")
                .setDescription("Reset subject names (request that same subject number and no duplicated names) for gtb file directly. Subject names can be stored in a file with ',' delimited form, and pass in via '--reset-subject @file'")
                .setFormat("'--reset-subject <string>,<string>,...' or '--reset-subject @<file>'");
        parser.register("--split")
                .arity(0)
                .convertTo(new PassedInConverter())
                .setOptionGroup("Options")
                .setDescription("Set the format of the output file (split by the chromosome or not).");
        parser.register("--yes", "-y")
                .arity(0)
                .convertTo(new PassedInConverter())
                .setOptionGroup("Options")
                .setDescription("Overwrite output file without asking.");

        // add commandRules
        parser.registerRule("--delete", "--retain", AT_MOST_ONE);
    }
}
