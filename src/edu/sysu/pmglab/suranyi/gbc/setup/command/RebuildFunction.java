package edu.sysu.pmglab.suranyi.gbc.setup.command;

import edu.sysu.pmglab.suranyi.commandParser.CommandMatcher;
import edu.sysu.pmglab.suranyi.commandParser.CommandParser;
import edu.sysu.pmglab.suranyi.easytools.FileUtils;
import edu.sysu.pmglab.suranyi.gbc.constant.ChromosomeInfo;
import edu.sysu.pmglab.suranyi.gbc.core.build.RebuildTask;
import edu.sysu.pmglab.suranyi.gbc.core.common.allelechecker.AlleleFreqTestChecker;
import edu.sysu.pmglab.suranyi.gbc.core.common.allelechecker.Chi2TestChecker;

import java.io.IOException;
import java.util.HashMap;
import java.util.Scanner;

/**
 * @author suranyi
 * @description 重压缩模式解析器
 */

enum RebuildFunction {
    /**
     * 单例模式
     */
    INSTANCE;

    private final CommandParser parser = RebuildParser.getParser();

    public static int submit(String... args) throws IOException, InterruptedException {
        if (args.length == INSTANCE.parser.getOffset() + 1) {
            // 参数长度和偏移量相等，此时打印 help 文档
            System.out.println(INSTANCE.parser);
            return 0;
        }

        CommandMatcher options = parse(args);

        if (options.isPassedIn("-h")) {
            System.out.println(usage());
        } else {
            // 加载染色体资源文件
            ChromosomeInfo.load((String) options.get("--contig"));

            // 预处理工作
            RebuildTask task = (RebuildTask) new RebuildTask((String) options.get("rebuild"))
                    .setParallel((int) options.get("-t"))
                    .setPhased(options.isPassedIn("-p"))
                    .setReordering(!options.isPassedIn("-nr"))
                    .setWindowSize((int) options.get("-ws"))
                    .setBlockSizeType((int) options.get("-bs"))
                    .setCompressor((String) options.get("-c"), (int) options.get("-l"))
                    .readyParas((String) options.get("-rp"));

            // 设置输入文件名
            if (options.isPassedIn("-o")) {
                task.setOutputFileName((String) options.get("-o"));
            } else {
                task.setOutputFileName(task.autoGenerateOutputFileName());
            }

            // 判断输出文件是否存在
            if (!options.isPassedIn("-y")) {
                if (FileUtils.exists(task.getOutputFileName())) {
                    Scanner scanner = new Scanner(System.in);
                    System.out.print("WARN    " + task.getOutputFileName() + " already exists, do you wish to overwrite? (y or n) ");

                    // 不覆盖文件，则删除该文件
                    if (!scanner.next().trim().equalsIgnoreCase("y")) {
                        throw new IOException("GBC can't create " + task.getOutputFileName() + ": file exists");
                    }
                }
            }

            // 开启过滤
            if (options.isPassedIn("--seq-ac")) {
                int[] ac = ((int[]) options.get("--seq-ac"));
                task.filterByAC(ac[0], ac[1]);
            }

            if (options.isPassedIn("--seq-af")) {
                double[] af = ((double[]) options.get("--seq-af"));
                task.filterByAF(af[0], af[1]);
            }

            if (options.isPassedIn("--seq-an")) {
                int[] an = ((int[]) options.get("--seq-an"));
                task.filterByAN(an[0], an[1]);
            }

            if (options.isPassedIn("--max-allele")) {
                task.setVariantQualityControlAllelesNum((int) options.get("--max-allele"));
            }

            // 子集选择
            if (options.isPassedIn("-s")) {
                task.selectSubjects((String[]) options.get("-s"));
            }

            if (options.isPassedIn("--retain")) {
                task.retain((HashMap<Integer, int[]>) options.get("--retain"));
            }

            if (options.isPassedIn("--delete")) {
                task.retain((HashMap<Integer, int[]>) options.get("--delete"));
            }

            if (options.isPassedIn("--align")) {
                task.alignWith((String) options.get("--align"));

                if (options.isPassedIn("--check-allele")) {
                    if (options.isPassedIn("--freq-gap")) {
                        task.setAlleleChecker(new AlleleFreqTestChecker((double) options.get("--freq-gap")));
                    } else if (options.isPassedIn("--p-value")) {
                        task.setAlleleChecker(new Chi2TestChecker((double) options.get("--p-value")));
                    } else {
                        task.setAlleleChecker(true);
                    }
                }
            }
            task.setSiteMergeType((String) (options.get("--method")));
            
            // 输出日志
            System.out.println("INFO    Start running " + task);
            long jobStart = System.currentTimeMillis();
            task.submit();
            long jobEnd = System.currentTimeMillis();

            // 结束任务，输出日志信息
            System.out.printf("INFO    Total Processing time: %.3f s; GTB size: %s%n", (float) (jobEnd - jobStart) / 1000,
                    FileUtils.sizeTransformer(FileUtils.sizeOf(task.getOutputFileName()), 3));
            System.out.printf("INFO    You can use command `show %s` to view all the information.%n", task.getOutputFileName());
        }

        return 0;
    }

    public static CommandMatcher parse(String... args) {
        return INSTANCE.parser.parse(args);
    }

    public static String usage() {
        return INSTANCE.parser.toString();
    }
}