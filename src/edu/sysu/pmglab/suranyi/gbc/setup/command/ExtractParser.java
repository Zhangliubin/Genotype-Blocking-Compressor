package edu.sysu.pmglab.suranyi.gbc.setup.command;

import edu.sysu.pmglab.suranyi.check.Assert;
import edu.sysu.pmglab.suranyi.commandParser.CommandMatcher;
import edu.sysu.pmglab.suranyi.commandParser.CommandParser;
import edu.sysu.pmglab.suranyi.commandParser.converter.array.StringArrayConverter;
import edu.sysu.pmglab.suranyi.commandParser.converter.map.KVConverter;
import edu.sysu.pmglab.suranyi.commandParser.converter.map.NaturalDoubleRangeConverter;
import edu.sysu.pmglab.suranyi.commandParser.converter.map.NaturalIntRangeConverter;
import edu.sysu.pmglab.suranyi.commandParser.converter.map.RangeWithIndexConverter;
import edu.sysu.pmglab.suranyi.commandParser.converter.value.BooleanConverter;
import edu.sysu.pmglab.suranyi.commandParser.converter.value.IntConverter;
import edu.sysu.pmglab.suranyi.commandParser.converter.value.PassedInConverter;
import edu.sysu.pmglab.suranyi.commandParser.converter.value.StringConverter;
import edu.sysu.pmglab.suranyi.commandParser.exception.ParameterException;
import edu.sysu.pmglab.suranyi.commandParser.validator.EnsureFileExistsValidator;
import edu.sysu.pmglab.suranyi.commandParser.validator.EnsureFileIsNotDirectoryValidator;
import edu.sysu.pmglab.suranyi.commandParser.validator.RangeValidator;
import edu.sysu.pmglab.suranyi.container.SmartList;
import edu.sysu.pmglab.suranyi.gbc.constant.ChromosomeInfo;
import edu.sysu.pmglab.suranyi.gbc.core.ITask;
import edu.sysu.pmglab.suranyi.gbc.core.common.qualitycontrol.allele.AlleleACController;
import edu.sysu.pmglab.suranyi.gbc.core.common.qualitycontrol.allele.AlleleAFController;
import edu.sysu.pmglab.suranyi.gbc.core.common.qualitycontrol.allele.AlleleANController;
import edu.sysu.pmglab.suranyi.unifyIO.FileStream;
import edu.sysu.pmglab.suranyi.unifyIO.partwriter.BGZOutputParam;

import java.io.IOException;
import java.util.HashMap;

import static edu.sysu.pmglab.suranyi.commandParser.CommandOptions.*;
import static edu.sysu.pmglab.suranyi.commandParser.CommandRuleType.AT_MOST_ONE;

enum ExtractParser {
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

    ExtractParser() {
        // global options
        parser = new CommandParser(false);
        parser.setProgramName("extract <input>");
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
        parser.register("extract")
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
                .setOptionGroup("Output Options")
                .setDescription("Specify the corresponding contig file.")
                .setFormat("'--contig <file>'");
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
                .setDescription("Output VCF file in text format. (this command will be executed automatically if '--o-bgz' is not passed in and the output file specified by '-o' is not end with '.gz')");
        parser.register("--o-bgz")
                .arity(0)
                .convertTo(new PassedInConverter())
                .setOptionGroup("Output Options")
                .setDescription("Output VCF file in bgz format. (this command will be executed automatically if '--o-text' is not passed in and the output file specified by '-o' is end with '.gz')");
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
        parser.register("--phased", "-p")
                .arity(1)
                .convertTo(new BooleanConverter())
                .setOptionGroup("Output Options")
                .setDescription("Force-set the status of the genotype. (same as the GTB basic information by default)")
                .setFormat("'-p [true/false]'");
        parser.register("--hideGT", "-hg")
                .arity(0)
                .convertTo(new PassedInConverter())
                .setOptionGroup("Output Options")
                .setDescription("Do not output the sample genotypes (only CHROM, POS, REF, ALT, AC, AN, AF).");
        parser.register("--yes", "-y")
                .arity(0)
                .convertTo(new PassedInConverter())
                .setOptionGroup("Output Options")
                .setDescription("Overwrite output file without asking.");
        parser.register("--subject", "-s")
                .arity(1)
                .convertTo(new StringArrayConverter(","))
                .setOptionGroup("Subset Selection Options")
                .setDescription("Extract the information of the specified subjects. Subject name can be stored in a file with ',' delimited form, and pass in via '-s @file'.")
                .setFormat("'-s <string>,<string>,...' or '-s @<file>'");
        parser.register("--range", "-r")
                .arity(1)
                .convertTo(params -> {
                    RangeWithIndexConverter converter = new RangeWithIndexConverter();
                    String[] range = converter.convert(params);
                    return new int[]{ChromosomeInfo.getIndex(range[0]), range[1].length() == 0 ? 0 : Integer.parseInt(range[1]), range[2].length() == 0 ? Integer.MAX_VALUE : Integer.parseInt(range[2])};
                })
                .setOptionGroup("Subset Selection Options")
                .setDescription("Extract the information by position range.")
                .setFormat("'-r <chrom>', '-r <chrom>:<start>-', '-r <chrom>:-<end>' or '-r <chrom>:<start>-<end>'");
        parser.register("--random")
                .arity(1)
                .convertTo(params -> {
                    Assert.that(params.length == 1);

                    // 使用 HashSet 保存位点，实现去重效果
                    HashMap<Integer, SmartList<Integer>> elementSet = new HashMap<>();
                    HashMap<Integer, int[]> target = new HashMap<>();

                    // 打开并读取文件
                    try (FileStream fs = new FileStream(params[0])) {
                        String line;
                        int chromosomeIndex;
                        int position;

                        while ((line = fs.readLineToString()) != null) {
                            String[] split = null;
                            if (line.contains(",")) {
                                split = line.split(",");
                            } else if (line.contains(" ")) {
                                split = line.split(" ");
                            } else if (line.contains("\t")) {
                                split = line.split("\t");
                            }

                            if ((split == null) || (split.length != 2)) {
                                throw new ParameterException("couldn't convert " + line + " to 'chrom,pos' or 'chrom<\\t>position'");
                            }

                            chromosomeIndex = ChromosomeInfo.getIndex(split[0]);

                            // 匹配 position
                            position = Integer.parseInt(split[1]);
                            if (!elementSet.containsKey(chromosomeIndex)) {
                                elementSet.put(chromosomeIndex, new SmartList<>(1024, true));
                            }
                            elementSet.get(chromosomeIndex).add(position);
                        }

                        // 为位点排序
                        for (int chromosomeInd : elementSet.keySet()) {
                            SmartList<Integer> positions = elementSet.get(chromosomeInd);
                            if ((positions != null) && (positions.size() > 0)) {
                                // 元素去重
                                positions.dropDuplicated();

                                // 元素排序
                                positions.sort(Integer::compare);

                                // 提取最终结果
                                target.put(chromosomeInd, positions.toIntegerArray());
                            }
                        }

                        return target;
                    } catch (IOException e) {
                        throw new ParameterException(e.getMessage());
                    }
                })
                .setOptionGroup("Subset Selection Options")
                .setDescription("Extract the information by position. (An inputFile is needed here, with each line contains 'chrom,position' or 'chrom<\\t> position'.")
                .setFormat("'--random <file>'");
        parser.register("--node")
                .arity(1)
                .convertTo(new KVConverter<Integer, int[]>("chrom", "node") {
                    @Override
                    public HashMap<Integer, int[]> convert(String... params) {
                        HashMap<String, String> KV = super.parseKV(params);

                        if (!KV.containsKey("chrom") || (KV.get("chrom") == null) || (KV.get("chrom").length() == 0)) {
                            throw new ParameterException("no chromosomes specified");
                        }

                        HashMap<Integer, int[]> result = new HashMap<>();

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
                        return result;
                    }
                })
                .setOptionGroup("Subset Selection Options")
                .setDescription("Extract the information by nodeIndex.")
                .setFormat("'--node chrom=<string>,<string>,...' or '--node chrom=<string>,<string>,...;node=<int>,<int>,...'");
        parser.register("--seq-ac")
                .arity(1)
                .convertTo(new NaturalIntRangeConverter())
                .validateWith(new RangeValidator(AlleleACController.MIN, AlleleACController.MAX))
                .setOptionGroup("Filter Options")
                .setDescription("Set the minimum alternate allele count (AC) or the AC range and extract the corresponding information.")
                .setFormat("'--seq-ac <minAc>-', '--seq-ac -<maxAc>' or '--seq-ac <minAc>-<maxAc>' (minAc and maxAc are non-negative integers)");
        parser.register("--seq-af")
                .arity(1)
                .convertTo(new NaturalDoubleRangeConverter())
                .validateWith(new RangeValidator(AlleleAFController.MIN, AlleleAFController.MAX))
                .setOptionGroup("Filter Options")
                .setDescription("Set the minimum alternate allele frequency (AF) or the AF range and extract the corresponding information.")
                .setFormat("'--seq-af <minAf>-', '--seq-af -<maxAf>' or '--seq-af <minAf>-<maxAf>' (minAf and maxAf are floating values between 0 and 1)");
        parser.register("--seq-an")
                .arity(1)
                .convertTo(new NaturalIntRangeConverter())
                .validateWith(new RangeValidator(AlleleANController.MIN, AlleleANController.MAX))
                .setOptionGroup("Filter Options")
                .setDescription("Set the minimum non-missing allele number (AN) and extract the corresponding information.")
                .setFormat("'--seq-an <minAn>-', '--seq-an -<maxAn>' or '--seq-an <minAn>-<maxAn>' (minAn and maxAn are non-negative integers)");

        // add commandRules
        parser.registerRule("--range", "--random", AT_MOST_ONE);
        parser.registerRule("--range", "--node", AT_MOST_ONE);
        parser.registerRule("--random", "--node", AT_MOST_ONE);
        parser.registerRule("--o-text", "--o-bgz", AT_MOST_ONE);
        parser.registerRule("--o-text", "--level", AT_MOST_ONE);
    }
}
