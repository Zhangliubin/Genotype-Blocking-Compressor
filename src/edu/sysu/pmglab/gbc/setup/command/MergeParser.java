package edu.sysu.pmglab.gbc.setup.command;

import edu.sysu.pmglab.gbc.coder.CoderConfig;
import edu.sysu.pmglab.gbc.constant.ChromosomeTags;
import edu.sysu.pmglab.gbc.core.ITask;
import edu.sysu.pmglab.gbc.core.common.allelechecker.AlleleChecker;
import edu.sysu.pmglab.gbc.core.common.qualitycontrol.variant.VariantAllelesNumController;
import edu.sysu.pmglab.check.Assert;
import edu.sysu.pmglab.commandParser.CommandMatcher;
import edu.sysu.pmglab.commandParser.CommandParser;
import edu.sysu.pmglab.commandParser.converter.array.StringArrayConverter;
import edu.sysu.pmglab.commandParser.converter.map.NaturalDoubleRangeConverter;
import edu.sysu.pmglab.commandParser.converter.map.NaturalIntRangeConverter;
import edu.sysu.pmglab.commandParser.converter.value.DoubleConverter;
import edu.sysu.pmglab.commandParser.converter.value.IntConverter;
import edu.sysu.pmglab.commandParser.converter.value.PassedInConverter;
import edu.sysu.pmglab.commandParser.converter.value.StringConverter;
import edu.sysu.pmglab.commandParser.validator.ElementValidator;
import edu.sysu.pmglab.commandParser.validator.EnsureFileExistsValidator;
import edu.sysu.pmglab.commandParser.validator.EnsureFileIsNotDirectoryValidator;
import edu.sysu.pmglab.commandParser.validator.RangeValidator;
import edu.sysu.pmglab.compressor.ICompressor;
import edu.sysu.pmglab.gbc.core.build.BlockSizeParameter;
import edu.sysu.pmglab.gbc.core.common.allelechecker.Chi2TestChecker;
import edu.sysu.pmglab.gbc.core.common.allelechecker.LDTestChecker;
import edu.sysu.pmglab.gbc.core.common.qualitycontrol.allele.AlleleACController;
import edu.sysu.pmglab.gbc.core.common.qualitycontrol.allele.AlleleAFController;
import edu.sysu.pmglab.gbc.core.common.qualitycontrol.allele.AlleleANController;
import edu.sysu.pmglab.gbc.core.common.switcher.ISwitcher;

import static edu.sysu.pmglab.commandParser.CommandOptions.*;
import static edu.sysu.pmglab.commandParser.CommandRuleType.AT_MOST_ONE;
import static edu.sysu.pmglab.commandParser.CommandRuleType.PRECONDITION;

enum MergeParser {
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

    MergeParser() {
        // global options
        parser = new CommandParser(false);
        parser.setProgramName("merge <inputs>");
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
        parser.register("merge")
                .addOptions(REQUEST, HIDDEN)
                .arity(-1)
                .convertTo(new StringArrayConverter())
                .validateWith(EnsureFileExistsValidator.INSTANCE, EnsureFileIsNotDirectoryValidator.INSTANCE)
                .setOptionGroup("Options");
        parser.register("--contig")
                .arity(1)
                .convertTo(new StringConverter())
                .defaultTo(ChromosomeTags.DEFAULT_FILE)
                .validateWith(EnsureFileExistsValidator.INSTANCE, EnsureFileIsNotDirectoryValidator.INSTANCE)
                .setOptionGroup("Compressor Options")
                .setDescription("Specify the corresponding contig file.")
                .setFormat("'--contig <file>'");
        parser.register("--method", "-m")
                .arity(1)
                .convertTo(params -> {
                    Assert.that(params.length == 1);

                    ElementValidator validator = new ElementValidator("union", "intersection");
                    validator.setAllowIndex(false);
                    validator.validate("--method", params[0]);
                    return params[0].toLowerCase();
                })
                .defaultTo("intersection")
                .setOptionGroup("Compressor Options")
                .setDescription("Method for handing coordinates in different files (union or intersection), the missing genotype is replaced by '.'.")
                .setFormat("'-m [union/intersection]'");
        parser.register("--biallelic")
                .arity(0)
                .convertTo(new PassedInConverter())
                .setOptionGroup("Compressor Options")
                .setDescription("Split multiallelic variants into multiple biallelic variants.");
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
        parser.register("--check-allele")
                .arity(0)
                .convertTo(new PassedInConverter())
                .setOptionGroup("Identify Inconsistent Allele Labels Options")
                .setDescription("Correct for potential complementary strand errors based on allele labels (A and C, T and G; only biallelic variants are supported). InputFiles will be resorted according the samples number of each GTB File.");
        parser.register("--p-value")
                .arity(1)
                .convertTo(new DoubleConverter())
                .defaultTo(Chi2TestChecker.DEFAULT_ALPHA)
                .validateWith(new RangeValidator(1e-6, 0.5))
                .setOptionGroup("Identify Inconsistent Allele Labels Options")
                .setDescription("Correct allele labels of rare variants (minor allele frequency < --maf) with the p-value of chi^2 test >= --p-value.")
                .setFormat("'--p-value <float, 0.000001~0.5>'");
        parser.register("--freq-gap")
                .arity(1)
                .convertTo(new DoubleConverter())
                .validateWith(new RangeValidator(1e-6, 0.5))
                .setOptionGroup("Identify Inconsistent Allele Labels Options")
                .setDescription("Correct allele labels of rare variants (minor allele frequency < --maf) with the allele frequency gap >= --freq-gap.")
                .setFormat("'--freq-gap <float, 0.000001~0.5>'");
        parser.register("--no-ld")
                .arity(0)
                .convertTo(new PassedInConverter())
                .setOptionGroup("Identify Inconsistent Allele Labels Options")
                .setDescription("By default, correct allele labels of common variants (minor allele frequency >= --maf) using the ld pattern in different files. Disable this function with option '--no-ld'.");
        parser.register("--min-r")
                .arity(1)
                .convertTo(new DoubleConverter())
                .defaultTo(LDTestChecker.DEFAULT_R_VALUE)
                .validateWith(new RangeValidator(0.5, 1.0))
                .setOptionGroup("Identify Inconsistent Allele Labels Options")
                .setDescription("Exclude pairs with genotypic LD correlation |R| values less than --min-r.")
                .setFormat("'--min-r <float, 0.5~1.0>'");
        parser.register("--flip-scan-threshold")
                .arity(1)
                .convertTo(new DoubleConverter())
                .defaultTo(LDTestChecker.DEFAULT_ALPHA)
                .validateWith(new RangeValidator(0.5, 1.0))
                .setOptionGroup("Identify Inconsistent Allele Labels Options")
                .setDescription("Variants with flipped ld patterns (strong correlation coefficients of opposite signs) that >= threshold ratio will be corrected.")
                .setFormat("'--flip-scan-threshold <float, 0.5~1.0>'");
        parser.register("--maf")
                .arity(1)
                .convertTo(new DoubleConverter())
                .defaultTo(AlleleChecker.DEFAULT_MAF)
                .validateWith(new RangeValidator(AlleleAFController.MIN, AlleleAFController.MAX))
                .setOptionGroup("Identify Inconsistent Allele Labels Options")
                .setDescription("For common variants (minor allele frequency >= --maf) use LD to identify inconsistent allele labels.")
                .setFormat("'--maf <float, 0~1>'");
        parser.register("--window-bp", "-bp")
                .arity(1)
                .convertTo(new IntConverter())
                .defaultTo(LDTestChecker.DEFAULT_WINDOW_SIZE_BP)
                .validateWith(new RangeValidator(1, Integer.MAX_VALUE))
                .setOptionGroup("Identify Inconsistent Allele Labels Options")
                .setDescription("The maximum number of physical bases between the variants being calculated for LD.")
                .setFormat("'-bp <int>' (>=1)");
        parser.register("--seq-ac")
                .arity(1)
                .convertTo(new NaturalIntRangeConverter())
                .validateWith(new RangeValidator(AlleleACController.MIN, AlleleACController.MAX))
                .setOptionGroup("Variant Selection Options")
                .setDescription("Exclude variants with the alternate allele count (AC) per variant out of the range [minAc, maxAc].")
                .setFormat("'--seq-ac <minAc>-', '--seq-ac -<maxAc>' or '--seq-ac <minAc>-<maxAc>' (minAc and maxAc are non-negative integers)");
        parser.register("--seq-af")
                .arity(1)
                .convertTo(new NaturalDoubleRangeConverter())
                .validateWith(new RangeValidator(AlleleAFController.MIN, AlleleAFController.MAX))
                .setOptionGroup("Variant Selection Options")
                .setDescription("Exclude variants with the alternate allele frequency (AF) per variant out of the range [minAf, maxAf].")
                .setFormat("'--seq-af <minAf>-', '--seq-af -<maxAf>' or '--seq-af <minAf>-<maxAf>' (minAf and maxAf are floating values between 0 and 1)");
        parser.register("--seq-an")
                .arity(1)
                .convertTo(new NaturalIntRangeConverter())
                .validateWith(new RangeValidator(AlleleANController.MIN, AlleleANController.MAX))
                .setOptionGroup("Variant Selection Options")
                .setDescription("Exclude variants with the non-missing allele number (AN) per variant out of the range [minAn, maxAn].")
                .setFormat("'--seq-an <minAn>-', '--seq-an -<maxAn>' or '--seq-an <minAn>-<maxAn>' (minAn and maxAn are non-negative integers)");
        parser.register("--max-allele")
                .arity(1)
                .convertTo(new IntConverter())
                .defaultTo(VariantAllelesNumController.DEFAULT)
                .validateWith(new RangeValidator(2, CoderConfig.MAX_ALLELE_NUM))
                .setOptionGroup("Variant Selection Options")
                .setDescription("Exclude variants with alleles over --max-allele.")
                .setFormat("'--max-allele <int, 2~15>'");

        // add commandRules
        parser.registerRule("--no-reordering", "--windowSize", AT_MOST_ONE);
        parser.registerRule("--phased", "--readyParas", AT_MOST_ONE);
        parser.registerRule("--blockSizeType", "--readyParas", AT_MOST_ONE);
        parser.registerRule("--compressor", "--readyParas", AT_MOST_ONE);
        parser.registerRule("--level", "--readyParas", AT_MOST_ONE);
        parser.registerRule("--check-allele", "--freq-gap", PRECONDITION);
        parser.registerRule("--check-allele", "--p-value", PRECONDITION);
        parser.registerRule("--check-allele", "--no-ld", PRECONDITION);
        parser.registerRule("--check-allele", "--min-r", PRECONDITION);
        parser.registerRule("--check-allele", "--flip-scan-threshold", PRECONDITION);
        parser.registerRule("--check-allele", "--maf", PRECONDITION);
        parser.registerRule("--check-allele", "--window-bp", PRECONDITION);
        parser.registerRule("--p-value", "--freq-gap", AT_MOST_ONE);
        parser.registerRule("--no-ld", "--min-r", AT_MOST_ONE);
        parser.registerRule("--no-ld", "--flip-scan-threshold", AT_MOST_ONE);
        parser.registerRule("--no-ld", "--window-bp", AT_MOST_ONE);
    }
}
