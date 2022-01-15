package dev.fromcommandfile;

import edu.sysu.pmglab.suranyi.commandParser.CommandMatcher;
import edu.sysu.pmglab.suranyi.commandParser.CommandParser;
import edu.sysu.pmglab.suranyi.easytools.FileUtils;
import edu.sysu.pmglab.suranyi.unifyIO.bgztools.BGZTools;

import java.io.IOException;
import java.util.Scanner;

/**
 * @author suranyi
 * @description BGZIP 工具模式解析器
 */

public class BGZIPCommandEntry {
    public static int submit(String... args) throws IOException {
        CommandMatcher modeSelection = BGZIPParser.parse(args);
        CommandParser subParser = getSubParser(modeSelection);

        // 打印帮助文档
        if (modeSelection.isPassedIn("-h")) {
            // 没有选择子模式
            if (subParser == null) {
                System.out.println(BGZIPParser.getParser());
            } else {
                System.out.println(subParser);
            }

            return 0;
        }

        // 解析参数列表 (subParser 一定不为 null)
        assert subParser != null;

        CommandMatcher options = subParser.parse(args);

        if (subParser.containCommandItem("compress")) {
            // 没有设置 outputFileName 时，则使用输入文件 + .gz
            String realOutputFileName = options.isPassedIn("-o") ? (String) options.get("-o") : options.get("compress") + ".gz";

            // 检查文件是否存在，若存在则抛出警告
            checkOutputFile(!(boolean) options.isPassedIn("-y"), realOutputFileName);

            // 运行任务并计时
            long start = System.currentTimeMillis();
            BGZTools.compress((String) options.get("compress"), realOutputFileName + ".~$temp", (int) options.get("-l"), (int) options.get("-t"));
            long end = System.currentTimeMillis();

            // 修改文件名
            FileUtils.rename(realOutputFileName + ".~$temp", realOutputFileName);

            // 结束任务，输出日志信息
            System.out.printf("INFO    Total Processing time: %.3f s; BGZ format size: %s%n",
                    (float) (end - start) / 1000, FileUtils.sizeTransformer(FileUtils.sizeOf(realOutputFileName), 3));

            return 0;
        }

        if (subParser.containCommandItem("decompress")) {
            String realOutputFileName;

            // 没有设置 outputFileName 时，则使用输入文件去掉 .gz
            if (!options.isPassedIn("-o")) {
                String inputFileName = ((String) options.get("decompress"));
                if (inputFileName.endsWith(".gz")) {
                    realOutputFileName = inputFileName.substring(0, inputFileName.length() - 3);
                } else {
                    throw new IOException("ERROR   can't remove an extension from " + inputFileName + " -- please rename");
                }
            } else {
                realOutputFileName = (String) options.get("-o");
            }

            // 检查文件是否存在，若存在则抛出警告
            checkOutputFile(!(boolean) options.isPassedIn("-y"), realOutputFileName);

            // 运行任务并计时
            long start = System.currentTimeMillis();
            BGZTools.decompress((String) options.get("decompress"), realOutputFileName + ".~$temp");
            long end = System.currentTimeMillis();

            // 修改文件名
            FileUtils.rename(realOutputFileName + ".~$temp", realOutputFileName);

            // 结束任务，输出日志信息
            System.out.printf("INFO    Total Processing time: %.3f s; Output size: %s%n",
                    (float) (end - start) / 1000,
                    FileUtils.sizeTransformer(FileUtils.sizeOf(realOutputFileName), 3));

            return 0;
        }

        if (subParser.containCommandItem("md5")) {
            // 计算解压信息的 md5 码
            System.out.printf("MD5 (%s, decompressed file) = %s%n", options.get("md5"), BGZTools.md5((String) options.get("md5")));
            return 0;
        }

        if (subParser.containCommandItem("convert")) {
            // 没有设置 outputFileName 时，则使用输入文件
            String realOutputFileName = options.isPassedIn("-o") ? (String) options.get("-o") : (String) options.get("convert");

            // 检查文件是否存在，若存在则抛出警告
            checkOutputFile(!(boolean) options.isPassedIn("-y"), realOutputFileName);

            // 运行任务并计时
            long start = System.currentTimeMillis();
            BGZTools.convert((String) options.get("convert"), realOutputFileName + ".~$temp", (int) options.get("-l"), (int) options.get("-t"));
            long end = System.currentTimeMillis();

            // 修改文件名
            FileUtils.rename(realOutputFileName + ".~$temp", realOutputFileName);

            // 结束任务，输出日志信息
            System.out.printf("INFO    Total Processing time: %.3f s; BGZ format size: %s%n",
                    (float) (end - start) / 1000,
                    FileUtils.sizeTransformer(FileUtils.sizeOf(realOutputFileName), 3));

            return 0;
        }

        if (subParser.containCommandItem("concat")) {
            // 没有设置 outputFileName 时，则使用第一个文件名
            String realOutputFileName = options.isPassedIn("-o") ? (String) options.get("-o") : ((String[]) options.get("concat"))[0];

            // 检查文件是否存在，若存在则抛出警告
            checkOutputFile(!(boolean) options.isPassedIn("-y"), realOutputFileName);

            // 运行任务并计时
            long start = System.currentTimeMillis();
            BGZTools.concat(realOutputFileName + ".~$temp", (String[]) options.get("concat"));
            long end = System.currentTimeMillis();

            // 修改文件名
            FileUtils.rename(realOutputFileName + ".~$temp", realOutputFileName);

            // 结束任务，输出日志信息
            System.out.printf("INFO    Total Processing time: %.3f s; BGZ format size: %s%n",
                    (float) (end - start) / 1000,
                    FileUtils.sizeTransformer(FileUtils.sizeOf(realOutputFileName), 3));

            return 0;
        }

        if (subParser.containCommandItem("extract")) {
            // 没有设置 outputFileName 时，则使用源文件名
            String realOutputFileName = options.isPassedIn("-o") ? (String) options.get("-o") : (String) options.get("extract");

            // 检查文件是否存在，若存在则抛出警告
            checkOutputFile(!(boolean) options.isPassedIn("-y"), realOutputFileName);

            long[] pointer = (long[]) options.get("-r");

            // 运行任务并计时
            long start = System.currentTimeMillis();
            BGZTools.extract((String) options.get("extract"), realOutputFileName + ".~$temp", pointer[0], pointer[1], (int) options.get("-l"), (int) options.get("-t"));
            long end = System.currentTimeMillis();

            // 修改文件名
            FileUtils.rename(realOutputFileName + ".~$temp", realOutputFileName);

            // 结束任务，输出日志信息
            System.out.printf("INFO    Total Processing time: %.3f s; BGZ format size: %s%n",
                    (float) (end - start) / 1000,
                    FileUtils.sizeTransformer(FileUtils.sizeOf(realOutputFileName), 3));

            return 0;
        }

        return 1;
    }

    static void checkOutputFile(boolean check, String realOutputFileName) throws IOException {
        if (check) {
            if (FileUtils.exists(realOutputFileName)) {
                Scanner scanner = new Scanner(System.in);
                System.out.print("WARN    " + realOutputFileName + " already exists, do you wish to overwrite? (y or n) ");

                // 不覆盖文件，则删除该文件
                if (!scanner.next().trim().equalsIgnoreCase("y")) {
                    throw new IOException("GBC can't create " + realOutputFileName + ": file exists");
                }
            }
        }
    }

    static CommandParser getSubParser(CommandMatcher options) {
        if (options.isPassedIn("compress")) {
            return CompressParser.getParser();
        } else if (options.isPassedIn("decompress")) {
            return DecompressParser.getParser();
        } else if (options.isPassedIn("md5")) {
            return MD5Parser.getParser();
        } else if (options.isPassedIn("convert")) {
            return ConvertParser.getParser();
        } else if (options.isPassedIn("concat")) {
            return ConcatParser.getParser();
        } else if (options.isPassedIn("extract")) {
            return ExtractParser.getParser();
        }

        return null;
    }
}