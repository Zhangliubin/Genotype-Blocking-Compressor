package edu.sysu.pmglab.suranyi.gbc.setup.command;

import edu.sysu.pmglab.suranyi.check.Assert;
import edu.sysu.pmglab.suranyi.commandParser.CommandMatcher;
import edu.sysu.pmglab.suranyi.commandParser.CommandParser;
import edu.sysu.pmglab.suranyi.commandParser.converter.IConverter;
import edu.sysu.pmglab.suranyi.commandParser.converter.array.StringArrayConverter;
import edu.sysu.pmglab.suranyi.commandParser.converter.map.RangeWithIndexConverter;
import edu.sysu.pmglab.suranyi.commandParser.converter.value.DoubleConverter;
import edu.sysu.pmglab.suranyi.commandParser.converter.value.IntConverter;
import edu.sysu.pmglab.suranyi.commandParser.converter.value.PassedInConverter;
import edu.sysu.pmglab.suranyi.commandParser.converter.value.StringConverter;
import edu.sysu.pmglab.suranyi.commandParser.exception.ParameterException;
import edu.sysu.pmglab.suranyi.commandParser.validator.ElementValidator;
import edu.sysu.pmglab.suranyi.commandParser.validator.EnsureFileExistsValidator;
import edu.sysu.pmglab.suranyi.commandParser.validator.EnsureFileIsNotDirectoryValidator;
import edu.sysu.pmglab.suranyi.commandParser.validator.RangeValidator;
import edu.sysu.pmglab.suranyi.gbc.constant.ChromosomeInfo;
import edu.sysu.pmglab.suranyi.gbc.core.ITask;
import edu.sysu.pmglab.suranyi.gbc.core.calculation.ld.ILDModel;
import edu.sysu.pmglab.suranyi.gbc.core.calculation.ld.LDTask;
import edu.sysu.pmglab.suranyi.gbc.core.common.qualitycontrol.allele.AlleleAFController;
import edu.sysu.pmglab.suranyi.unifyIO.partwriter.BGZOutputParam;

import java.util.Arrays;

import static edu.sysu.pmglab.suranyi.commandParser.CommandOptions.*;
import static edu.sysu.pmglab.suranyi.commandParser.CommandRuleType.AT_MOST_ONE;

enum LDParser {
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

    LDParser() {
        // global options
        parser = new CommandParser(false);
        parser.setProgramName("ld <input>");
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
        parser.register("ld")
                .addOptions(REQUEST, HIDDEN)
                .arity(1)
                .convertTo(new StringConverter())
                .validateWith(EnsureFileExistsValidator.INSTANCE, EnsureFileIsNotDirectoryValidator.INSTANCE)
                .setOptionGroup("Options");
        parser.register("--output", "-o")
                .arity(1)
                .convertTo(new StringConverter())
                .setOptionGroup("Output Options")
                .setDescription("Set the output file.")
                .setFormat("'-o <file>'");
        parser.register("--o-text")
                .arity(0)
                .convertTo(new PassedInConverter())
                .setOptionGroup("Output Options")
                .setDescription("Output LD file in text format. (this command will be executed automatically if '--o-bgz' is not passed in and the output file specified by '-o' is not end with '.gz')");
        parser.register("--o-bgz")
                .arity(0)
                .convertTo(new PassedInConverter())
                .setOptionGroup("Output Options")
                .setDescription("Output LD file in bgz format. (this command will be executed automatically if '--o-text' is not passed in and the output file specified by '-o' is end with '.gz')");
        parser.register("--level", "-l")
                .arity(1)
                .convertTo(new IntConverter())
                .defaultTo(BGZOutputParam.DEFAULT_LEVEL)
                .validateWith(new RangeValidator(BGZOutputParam.MIN_LEVEL, BGZOutputParam.MAX_LEVEL))
                .setOptionGroup("Output Options")
                .setDescription("Set the compression level. (Execute only if --o-bgz is passed in)")
                .setFormat("'-l <int, 0~9>'");
        parser.register("--threads", "-t")
                .arity(1)
                .convertTo(new IntConverter())
                .defaultTo(ITask.INIT_THREADS)
                .validateWith(new RangeValidator(ITask.MIN_THREADS, ITask.AVAILABLE_PROCESSORS))
                .setOptionGroup("Output Options")
                .setDescription("Set the number of threads.")
                .setFormat("'-t <int, " + ITask.MIN_THREADS + "~" + ITask.AVAILABLE_PROCESSORS + ">'");
        parser.register("--yes", "-y")
                .arity(0)
                .convertTo(new PassedInConverter())
                .setOptionGroup("Output Options")
                .setDescription("Overwrite output file without asking.");
        parser.register("--contig")
                .arity(1)
                .convertTo(new StringConverter())
                .defaultTo(ChromosomeInfo.DEFAULT_FILE)
                .validateWith(EnsureFileExistsValidator.INSTANCE, EnsureFileIsNotDirectoryValidator.INSTANCE)
                .setOptionGroup("LD Calculation Options")
                .setDescription("Specify the corresponding contig file.")
                .setFormat("'--contig <file>'");
        parser.register("--model", "-m")
                .arity(1)
                .convertTo((IConverter<ILDModel>) params -> {
                    Assert.that(params.length == 1);
                    ElementValidator validator = new ElementValidator("geno", "--geno", "--geno-ld", "--geno-r2", "hap", "--hap", "--hap-ld", "--hap-r2");
                    validator.validate("--model", params);

                    String param = params[0].toUpperCase();

                    if (param.equals("GENO") || param.equals("--GENO") || param.equals("--GENO-LD") || param.equals("--GENO-R2")) {
                        return ILDModel.GENOTYPE_LD;
                    } else if (param.equals("HAP") || param.equals("--HAP") || param.equals("--HAP-LD") || param.equals("--HAP-R2")) {
                        return ILDModel.HAPLOTYPE_LD;
                    } else {
                        throw new ParameterException("unable convert " + Arrays.toString(params) + " to " + ILDModel.class);
                    }
                })
                .setOptionGroup("LD Calculation Options")
                .setDescription("Calculate pairwise the linkage disequilibrium (hap, --hap, --hap-ld, --hap-r2) or genotypic correlation (geno, --geno, --geno-ld, --geno-r2)")
                .setFormat("'-m <string>'");
        parser.register("--window-bp", "-bp")
                .arity(1)
                .convertTo(new IntConverter())
                .defaultTo(LDTask.DEFAULT_WINDOW_SIZE_BP)
                .validateWith(new RangeValidator(1, Integer.MAX_VALUE))
                .setOptionGroup("LD Calculation Options")
                .setDescription("The maximum number of physical bases between the variants being calculated for LD.")
                .setFormat("'-bp <int>' (>=1)");
        parser.register("--window-kb", "-kb")
                .arity(1)
                .convertTo(new IntConverter())
                .defaultTo(LDTask.DEFAULT_WINDOW_SIZE_BP / 1000)
                .validateWith(new RangeValidator(1, Integer.MAX_VALUE / 1000))
                .setOptionGroup("LD Calculation Options")
                .setDescription("The maximum number of physical bases between the variants being calculated for LD (1kb=1000bp).")
                .setFormat("'-kb <int>' (>=1)");
        parser.register("--min-r2")
                .arity(1)
                .convertTo(new DoubleConverter())
                .defaultTo(LDTask.DEFAULT_MIN_R2)
                .validateWith(new RangeValidator(0.0, 1.0))
                .setOptionGroup("LD Calculation Options")
                .setDescription("Exclude pairs with R2 values less than --min-r2.")
                .setFormat("'--min-r2 <float, 0~1>'");
        parser.register("--maf")
                .arity(1)
                .convertTo(new DoubleConverter())
                .defaultTo(LDTask.DEFAULT_MAF)
                .validateWith(new RangeValidator(AlleleAFController.MIN, AlleleAFController.MAX))
                .setOptionGroup("LD Calculation Options")
                .setDescription("Exclude variants with the minor allele frequency (MAF) per variant < --maf.")
                .setFormat("'--maf <float, 0~1>'");
        parser.register("--subject", "-s")
                .arity(1)
                .convertTo(new StringArrayConverter(","))
                .setOptionGroup("Subset Selection Options")
                .setDescription("Calculate the LD for the specified subjects. Subject name can be stored in a file with ',' delimited form, and pass in via '-s @file'")
                .setFormat("'-s <string>,<string>,...' or '-s @<file>'");
        parser.register("--range", "-r")
                .arity(1)
                .convertTo(params -> {
                    RangeWithIndexConverter converter = new RangeWithIndexConverter();
                    String[] range = converter.convert(params);
                    return new int[]{ChromosomeInfo.getIndex(range[0]), range[1].length() == 0 ? 0 : Integer.parseInt(range[1]), range[2].length() == 0 ? 0 : Integer.parseInt(range[2])};
                })
                .setOptionGroup("Subset Selection Options")
                .setDescription("Calculate the LD by specified position range.")
                .setFormat("'-r <chrom>', '-r <chrom>:<start>-', '-r <chrom>:-<end>' or '-r <chrom>:<start>-<end>'");

        // add commandRules
        parser.registerRule("--o-text", "--o-bgz", AT_MOST_ONE);
        parser.registerRule("--o-text", "--level", AT_MOST_ONE);
        parser.registerRule("--window-bp", "--window-kb", AT_MOST_ONE);
    }
}
