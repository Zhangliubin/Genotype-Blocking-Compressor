package edu.sysu.pmglab.suranyi.gbc.setup.command;

import edu.sysu.pmglab.suranyi.commandParser.CommandMatcher;
import edu.sysu.pmglab.suranyi.commandParser.CommandParser;
import edu.sysu.pmglab.suranyi.easytools.FileUtils;
import edu.sysu.pmglab.suranyi.gbc.constant.ChromosomeInfo;
import edu.sysu.pmglab.suranyi.gbc.core.edit.EditTask;

import java.io.IOException;
import java.util.HashMap;
import java.util.Scanner;

/**
 * @author suranyi
 * @description 编辑模式解析器
 */

enum EditFunction {
    /**
     * 单例模式
     */
    INSTANCE;

    private final CommandParser parser = EditParser.getParser();

    public static int submit(String... args) throws IOException {
        CommandMatcher options = parse(args);

        if (options.isPassedIn("-h")) {
            System.out.println(usage());
        } else {
            // 加载染色体资源文件
            ChromosomeInfo.load((String) options.get("--contig"));

            EditTask task = new EditTask((String) options.get("edit"));

            if (options.isPassedIn("--unique")) {
                task.unique();
            }

            if (options.isPassedIn("--split")) {
                task.split();
            }

            if (options.isPassedIn("--reset-subject")) {
                task.resetSubjects((String[]) options.get("--reset-subject"));
            }

            if (options.isPassedIn("--delete")) {
                // 按照位置删除
                task.delete((HashMap<Integer, int[]>) options.get("--delete"));
            }

            if (options.isPassedIn("--retain")) {
                // 按照位置删除
                task.retain((HashMap<Integer, int[]>) options.get("--retain"));
            }

            if (options.isPassedIn("--concat")) {
                // 按照位置删除
                task.concat((String[]) options.get("--concat"));
            }

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

            // 根据所选模式选择合适的方法
            System.out.println("INFO    Start running " + task);
            long jobStart = System.currentTimeMillis();
            // 提交更改
            task.submit();
            long jobEnd = System.currentTimeMillis();

            // 结束任务，输出日志信息
            System.out.printf("INFO    Total Processing time: %.3f s; GTB size: %s%n", (float) (jobEnd - jobStart) / 1000,
                    FileUtils.sizeTransformer(FileUtils.sizeOf(task.getOutputFileName()), 3));

            // 按照染色体编号切割文件
            if (options.isPassedIn("--split")) {
                System.out.printf("INFO    You can use command `show %s/chr[x].gtb` to view all the information on chromosome [x].%n", task.getOutputFileName());
            } else {
                System.out.printf("INFO    You can use command `show %s` to view all the information.%n", task.getOutputFileName());
            }
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