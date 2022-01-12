package edu.sysu.pmglab.suranyi.gbc.setup.command;

import edu.sysu.pmglab.suranyi.commandParser.CommandMatcher;
import edu.sysu.pmglab.suranyi.commandParser.CommandParser;
import edu.sysu.pmglab.suranyi.easytools.FileUtils;
import edu.sysu.pmglab.suranyi.gbc.constant.ChromosomeInfo;
import edu.sysu.pmglab.suranyi.gbc.core.extract.ExtractTask;

import java.io.IOException;
import java.util.HashMap;
import java.util.Scanner;

/**
 * @author suranyi
 * @description 解压模式解析器
 */

enum ExtractFunction {
    /**
     * 单例模式
     */
    INSTANCE;

    private final CommandParser parser = ExtractParser.getParser();

    public static int submit(String... args) throws IOException {
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
            ExtractTask task = new ExtractTask((String) options.get("extract"))
                    .setParallel((int) options.get("-t"))
                    .hideGenotype(options.isPassedIn("-hg"));

            // 设置向型
            if (options.isPassedIn("-p")) {
                task.setPhased((boolean) options.get("-p"));
            }

            // 样本选择
            if (options.isPassedIn("-s")) {
                task.selectSubjects((String[]) options.get("-s"));
            }

            // 添加过滤器
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

            // 设置输出文件名与输出格式
            if (options.isPassedIn("-o")) {
                task.setOutputFileName((String) options.get("-o"));

                // 指定输出格式，否则根据文件扩展名进行判断
                if (options.isPassedIn("--o-text")) {
                    task.setCompressToBGZF(false);
                } else if (options.isPassedIn("--o-bgz") || options.isPassedIn("-l") || task.isCompressToBGZF()) {
                    task.setCompressToBGZF(true, (int) options.get("-l"));
                }
            } else {
                // 设置输出格式
                if (options.isPassedIn("--o-text")) {
                    task.setCompressToBGZF(false);
                } else if (options.isPassedIn("--o-bgz") || options.isPassedIn("-l")) {
                    task.setCompressToBGZF(true, (int) options.get("-l"));
                }

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

            // 提交任务
            System.out.println("INFO    Start running " + task);
            long jobStart = System.currentTimeMillis();

            // 根据子模型进行相应的工作
            if (options.isPassedIn("--range")) {
                // 按照位置访问
                int[] range = (int[]) options.get("--range");
                task.decompressByRange(range[0], range[1], range[2]);
            } else if (options.isPassedIn("--random")) {
                // 随机提取数据
                task.decompressByPosition((HashMap<Integer, int[]>) options.get("--random"));
            } else if (options.isPassedIn("--node")) {
                task.decompressByNodeIndex((HashMap<Integer, int[]>) options.get("--node"));
            } else {
                // 解压全部
                task.decompressAll();
            }

            long jobEnd = System.currentTimeMillis();
            // 结束任务，输出日志信息
            System.out.printf("INFO    Total Processing time: %.3f s; Output size: %s%n", (float) (jobEnd - jobStart) / 1000,
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