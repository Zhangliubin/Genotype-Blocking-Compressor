package edu.sysu.pmglab.suranyi.gbc.setup.command;

import edu.sysu.pmglab.suranyi.commandParser.CommandMatcher;
import edu.sysu.pmglab.suranyi.commandParser.CommandParser;
import edu.sysu.pmglab.suranyi.easytools.FileUtils;
import edu.sysu.pmglab.suranyi.gbc.constant.ChromosomeInfo;
import edu.sysu.pmglab.suranyi.gbc.core.calculation.ld.ILDModel;
import edu.sysu.pmglab.suranyi.gbc.core.calculation.ld.LDTask;

import java.io.IOException;
import java.util.Scanner;

/**
 * @author suranyi
 * @description 计算 LD-score 模式解析器
 */

enum LDFunction {
    /**
     * 单例模式
     */
    INSTANCE;

    private final CommandParser parser = LDParser.getParser();

    public static int submit(String... args) throws IOException {
        CommandMatcher options = parse(args);

        if (options.isPassedIn("-h")) {
            System.out.println(usage());
        } else {
            // 加载染色体资源文件
            ChromosomeInfo.load((String) options.get("--contig"));

            // 设置 LD 模型
            LDTask task = new LDTask((String) options.get("ld"))
                    .setLdModel((ILDModel) options.get("-m"))
                    .setMaf((double) options.get("--maf"))
                    .setMinR2((double) options.get("--min-r2"))
                    .setWindowSizeBp(options.isPassedIn("-kb") ? (int) options.get("-kb") * 1000 : (int) options.get("-bp"))
                    .setParallel((int) options.get("-t"));

            // 设置输出文件名
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

            // 样本选择
            if (options.isPassedIn("-s")) {
                task.selectSubjects((String[]) options.get("-s"));
            }

            System.out.println("INFO    Start running " + task);
            long jobStart = System.currentTimeMillis();
            if (options.isPassedIn("-r")) {
                int[] range = ((int[]) options.get("-r"));
                task.submit(range[0], range[1], range[2]);
            } else {
                task.submit();
            }
            long jobEnd = System.currentTimeMillis();

            // 结束任务，输出日志信息
            System.out.printf("INFO    Total Processing time: %.3f s; LD file size: %s%n",
                    (float) (jobEnd - jobStart) / 1000,
                    FileUtils.sizeTransformer(FileUtils.sizeOf(task.getOutputFileName()), 3));
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