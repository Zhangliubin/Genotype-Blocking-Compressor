package edu.sysu.pmglab.suranyi.gbc.setup.command;

import edu.sysu.pmglab.suranyi.commandParser.CommandMatcher;
import edu.sysu.pmglab.suranyi.commandParser.CommandParser;
import edu.sysu.pmglab.suranyi.commandParser.converter.array.StringArrayConverter;
import edu.sysu.pmglab.suranyi.commandParser.converter.map.KVConverter;
import edu.sysu.pmglab.suranyi.commandParser.converter.map.NaturalDoubleRangeConverter;
import edu.sysu.pmglab.suranyi.commandParser.converter.map.NaturalIntRangeConverter;
import edu.sysu.pmglab.suranyi.commandParser.converter.value.DoubleConverter;
import edu.sysu.pmglab.suranyi.commandParser.converter.value.IntConverter;
import edu.sysu.pmglab.suranyi.commandParser.converter.value.PassedInConverter;
import edu.sysu.pmglab.suranyi.commandParser.converter.value.StringConverter;
import edu.sysu.pmglab.suranyi.commandParser.exception.ParameterException;
import edu.sysu.pmglab.suranyi.commandParser.validator.ElementValidator;
import edu.sysu.pmglab.suranyi.commandParser.validator.EnsureFileExistsValidator;
import edu.sysu.pmglab.suranyi.commandParser.validator.EnsureFileIsNotDirectoryValidator;
import edu.sysu.pmglab.suranyi.commandParser.validator.RangeValidator;
import edu.sysu.pmglab.suranyi.compressor.ICompressor;
import edu.sysu.pmglab.suranyi.gbc.coder.CoderConfig;
import edu.sysu.pmglab.suranyi.gbc.constant.ChromosomeInfo;
import edu.sysu.pmglab.suranyi.gbc.core.ITask;
import edu.sysu.pmglab.suranyi.gbc.core.build.BlockSizeParameter;
import edu.sysu.pmglab.suranyi.gbc.core.common.allelechecker.Chi2TestChecker;
import edu.sysu.pmglab.suranyi.gbc.core.common.qualitycontrol.allele.AlleleACController;
import edu.sysu.pmglab.suranyi.gbc.core.common.qualitycontrol.allele.AlleleAFController;
import edu.sysu.pmglab.suranyi.gbc.core.common.qualitycontrol.allele.AlleleANController;
import edu.sysu.pmglab.suranyi.gbc.core.common.qualitycontrol.variant.VariantAllelesNumController;
import edu.sysu.pmglab.suranyi.gbc.core.common.switcher.ISwitcher;

import java.util.HashMap;

import static edu.sysu.pmglab.suranyi.commandParser.CommandOptions.*;
import static edu.sysu.pmglab.suranyi.commandParser.CommandRuleType.AT_MOST_ONE;

enum RebuildParser {
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

    RebuildParser() {
        // global options
        parser = new CommandParser(false);
        parser.setProgramName("rebuild <input>");
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
        parser.register("rebuild")
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
                .setOptionGroup("Compressor Options")
                .setDescription("Specify the corresponding contig file.")
                .setFormat("'--contig <file>'");
        parser.register("--output", "-o")
                .arity(1)
                .convertTo(new StringConverter())
                .setOptionGroup("Compressor Options")
                .setDescription("Set the output file.")
                .setFormat("'-o <file>'");
        parser.register("--threads", "-t")
                .arity(1)
                .convertTo(new IntConverter())
                .defaultTo(ITask.INIT_THREADS)
                .validateWith(new RangeValidator(ITask.MIN_THREADS, ITask.AVAILABLE_PROCESSORS))
                .setOptionGroup("Compressor Options")
                .setDescription("Set the number of threads.")
                .setFormat("'-t <int, " + ITask.MIN_THREADS + "~" + ITask.AVAILABLE_PROCESSORS + ">'");
        parser.register("--phased", "-p")
                .arity(0)
                .convertTo(new PassedInConverter())
                .setOptionGroup("Compressor Options")
                .setDescription("Set the status of genotype to phased.");
        parser.register("--blockSizeType", "-bs")
                .arity(1)
                .convertTo(new IntConverter())
                .defaultTo(BlockSizeParameter.DEFAULT_BLOCK_SIZE_TYPE)
                .validateWith(new RangeValidator(-1, BlockSizeParameter.MAX_BLOCK_SIZE_TYPE))
                .setOptionGroup("Compressor Options")
                .setDescription("Set the maximum size=2^(7+x) of each block.")
                .setFormat("'-bs <int, 0~7>' (-1 means auto-adjustment)");
        parser.register("--no-reordering", "-nr")
                .arity(0)
                .convertTo(new PassedInConverter())
                .setOptionGroup("Compressor Options")
                .setDescription("Disable the Approximate Minimum Discrepancy Ordering (AMDO) algorithm.");
        parser.register("--windowSize", "-ws")
                .arity(1)
                .convertTo(new IntConverter())
                .defaultTo(ISwitcher.DEFAULT_SIZE)
                .validateWith(new RangeValidator(ISwitcher.MIN, ISwitcher.MAX))
                .setOptionGroup("Compressor Options")
                .setDescription("Set the window size of the AMDO algorithm.")
                .setFormat("'-ws <int, 1~131072>'");
        parser.register("--compressor", "-c")
                .arity(1)
                .convertTo(new StringConverter() {
                    @Override
                    public String convert(String... params) {
                        String param = super.convert(params);
                        ElementValidator validator = new ElementValidator(ICompressor.getCompressorNames());
                        validator.validate("--compressor", param);
                        if (validator.indexOf(param) == -1) {
                            return validator.valueOf(Integer.parseInt(param));
                        } else {
                            return param.toUpperCase();
                        }
                    }
                })
                .defaultTo(ICompressor.getCompressorName(ICompressor.DEFAULT))
                .setOptionGroup("Compressor Options")
                .setDescription("Set the basic compressor for compressing processed data.")
                .setFormat("'-c [0/1]' or '-c [ZSTD/LZMA]'");
        parser.register("--level", "-l")
                .arity(1)
                .convertTo(new IntConverter())
                .defaultTo(-1)
                .validateWith(new RangeValidator(-1.0, 31.0))
                .setOptionGroup("Compressor Options")
                .setDescription("Compression level to use when basic compressor works.")
                .setFormat("'-l <int>' (ZSTD: 0~22, 16 as default; LZMA: 0~9, 3 as default)");
        parser.register("--readyParas", "-rp")
                .arity(1)
                .convertTo(new StringConverter())
                .validateWith(EnsureFileExistsValidator.INSTANCE, EnsureFileIsNotDirectoryValidator.INSTANCE)
                .setOptionGroup("Compressor Options")
                .setDescription("Import the template parameters (-p, -bs, -c, -l) from an external GTB file.")
                .setFormat("'-rp <file>'");
        parser.register("--yes", "-y")
                .arity(0)
                .convertTo(new PassedInConverter())
                .setOptionGroup("Compressor Options")
                .setDescription("Overwrite output file without asking.");

        parser.register("--align")
                .addOptions(DEBUG)
                .arity(0)
                .convertTo(new PassedInConverter())
                .setOptionGroup("Check Complementary Strand (Order GTB required)")
                .setDescription("Correct for potential complementary strand errors based on allele frequency (A and C, T and G; only biallelic variants are supported). InputFiles will be resorted according the samples number of each GTB File.");
        parser.register("--check-allele")
                .addOptions(DEBUG)
                .arity(0)
                .convertTo(new PassedInConverter())
                .setOptionGroup("Check Complementary Strand (Order GTB required)")
                .setDescription("Correct for potential complementary strand errors based on allele frequency (A and C, T and G; only biallelic variants are supported). InputFiles will be resorted according the samples number of each GTB File.");
        parser.register("--p-value")
                .addOptions(DEBUG)
                .arity(1)
                .convertTo(new DoubleConverter())
                .defaultTo(Chi2TestChecker.DEFAULT_ALPHA)
                .validateWith(new RangeValidator(1e-6, 0.5))
                .setOptionGroup("Check Complementary Strand (Order GTB required)")
                .setDescription("Correct allele of variants (potential complementary strand errors) with the p-value of chi^2 test >= --p-value.")
                .setFormat("--p-value <float, 0.000001~0.5>");
        parser.register("--freq-gap")
                .addOptions(DEBUG)
                .arity(1)
                .convertTo(new DoubleConverter())
                .validateWith(new RangeValidator(1e-6, 0.5))
                .setOptionGroup("Check Complementary Strand (Order GTB required)")
                .setDescription("Correct allele of variants (potential complementary strand errors) with the allele frequency gap >= --freq-gap.")
                .setFormat("--freq-gap <float, 0.000001~0.5>");

        parser.register("--subject", "-s")
                .arity(1)
                .convertTo(new StringArrayConverter(","))
                .setOptionGroup("Subset Selection Options")
                .setDescription("Rebuild the GTB for the specified subjects. Subject name can be stored in a file with ',' delimited form, and pass in via '-s @file'.")
                .setFormat("'-s <string>,<string>,...' or '-s @<file>'");
        parser.register("--delete")
                .arity(1)
                .convertTo(new KVConverter<Integer, int[]>("chrom", "node") {
                    @Override
                    public HashMap<Integer, int[]> convert(String... params) {
                        HashMap<String, String> KV = super.parseKV(params);
                        HashMap<Integer, int[]> result = new HashMap<>();

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
                                    result.put(ChromosomeInfo.getIndex(chromosome), nodeIndexes);
                                }
                            } else {
                                for (String chromosome : chromosomes) {
                                    result.put(ChromosomeInfo.getIndex(chromosome), null);
                                }
                            }
                        }
                        return result;
                    }
                })
                .setOptionGroup("Subset Selection Options")
                .setDescription("Delete the specified GTBNodes.")
                .setFormat("'--delete chrom=<string>,<string>,...' or '--delete chrom=<string>,<string>,...;node=<int>,<int>,...'");
        parser.register("--retain")
                .arity(1)
                .convertTo(new KVConverter<Integer, int[]>("chrom", "node") {
                    @Override
                    public HashMap<Integer, int[]> convert(String... params) {
                        HashMap<String, String> KV = super.parseKV(params);
                        HashMap<Integer, int[]> result = new HashMap<>();

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
                                    result.put(ChromosomeInfo.getIndex(chromosome), nodeIndexes);
                                }
                            } else {
                                for (String chromosome : chromosomes) {
                                    result.put(ChromosomeInfo.getIndex(chromosome), null);
                                }
                            }
                        }
                        return result;
                    }
                })
                .setOptionGroup("Subset Selection Options")
                .setDescription("Retain the specified GTBNodes.")
                .setFormat("'--retain chrom=<string>,<string>,...' or '--retain chrom=<string>,<string>,...;node=<int>,<int>,...'");
        parser.register("--seq-ac")
                .arity(1)
                .convertTo(new NaturalIntRangeConverter())
                .validateWith(new RangeValidator(AlleleACController.MIN, AlleleACController.MAX))
                .setOptionGroup("Quality Control Options")
                .setDescription("Exclude variants with the minimal alternate allele count per variant out of the range [minAc, maxAc].")
                .setFormat("'--seq-ac <minAc>-', '--seq-ac -<maxAc>' or '--seq-ac <minAc>-<maxAc>' (minAc and maxAc are non-negative integers)");
        parser.register("--seq-af")
                .arity(1)
                .convertTo(new NaturalDoubleRangeConverter())
                .validateWith(new RangeValidator(AlleleAFController.MIN, AlleleAFController.MAX))
                .setOptionGroup("Quality Control Options")
                .setDescription("Exclude variants with the minimal alternate allele frequency per variant out of the range [minAf, maxAf].")
                .setFormat("'--seq-af <minAf>-', '--seq-af -<maxAf>' or '--seq-af <minAf>-<maxAf>' (minAf and maxAf are floating values between 0 and 1)");
        parser.register("--seq-an")
                .arity(1)
                .convertTo(new NaturalIntRangeConverter())
                .validateWith(new RangeValidator(AlleleANController.MIN, AlleleANController.MAX))
                .setOptionGroup("Quality Control Options")
                .setDescription("Exclude variants with the minimum non-missing allele number per variant out of the range [minAn, maxAn].")
                .setFormat("'--seq-an <minAn>-', '--seq-an -<maxAn>' or '--seq-an <minAn>-<maxAn>' (minAn and maxAn are non-negative integers)");
        parser.register("--max-allele")
                .arity(1)
                .convertTo(new IntConverter())
                .defaultTo(VariantAllelesNumController.DEFAULT)
                .validateWith(new RangeValidator(2, CoderConfig.MAX_ALLELE_NUM))
                .setOptionGroup("Quality Control Options")
                .setDescription("Exclude variants with alleles over --max-allele.")
                .setFormat("'--max-allele <int, 2~15>'");

        // add commandRules
        parser.registerRule("--no-reordering", "--windowSize", AT_MOST_ONE);
        parser.registerRule("--phased", "--readyParas", AT_MOST_ONE);
        parser.registerRule("--blockSizeType", "--readyParas", AT_MOST_ONE);
        parser.registerRule("--compressor", "--readyParas", AT_MOST_ONE);
        parser.registerRule("--level", "--readyParas", AT_MOST_ONE);
        parser.registerRule("--delete", "--retain", AT_MOST_ONE);
    }
}
