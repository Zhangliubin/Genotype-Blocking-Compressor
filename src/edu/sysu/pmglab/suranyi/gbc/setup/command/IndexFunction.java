package edu.sysu.pmglab.suranyi.gbc.setup.command;

import edu.sysu.pmglab.suranyi.commandParser.CommandMatcher;
import edu.sysu.pmglab.suranyi.commandParser.CommandParser;
import edu.sysu.pmglab.suranyi.easytools.FileUtils;
import edu.sysu.pmglab.suranyi.gbc.constant.ChromosomeInfo;
import edu.sysu.pmglab.suranyi.gbc.core.gtbcomponent.GTBManager;
import edu.sysu.pmglab.suranyi.gbc.core.gtbcomponent.GTBRootCache;

import java.io.IOException;
import java.util.Scanner;

/**
 * @author suranyi
 * @description 索引 contig 模式解析器
 */

enum IndexFunction {
    /**
     * 单例模式
     */
    INSTANCE;

    private final CommandParser parser = IndexParser.getParser();

    public static int submit(String... args) throws IOException {
        CommandMatcher options = parse(args);

        if (options.isPassedIn("-h")) {
            System.out.println(usage());
        } else {
            ChromosomeInfo.load((String) options.get("-from"));

            if (options.isPassedIn("-to")) {
                String realOutputFileName = options.isPassedIn("-o") ? (String) options.get("-o") : (String) options.get("index");

                if (!(boolean) options.isPassedIn("-y")) {
                    if (FileUtils.exists(realOutputFileName)) {
                        Scanner scanner = new Scanner(System.in);
                        System.out.print("WARN    " + realOutputFileName + " already exists, do you wish to overwrite? (y or n) ");

                        // 不覆盖文件，则删除该文件
                        if (!scanner.next().trim().equalsIgnoreCase("y")) {
                            throw new IOException("GBC can't create " + realOutputFileName + ": file exists");
                        }
                    }
                }

                // 打开 GTB 管理器
                GTBManager manager = GTBRootCache.get((String) options.get("index"));

                // 重设重叠群信息
                manager.getGtbTree().resetContig((String) options.get("-to"));

                // 输出文件
                manager.toFile(realOutputFileName + ".~$temp");

                // 修改文件名
                FileUtils.rename(realOutputFileName + ".~$temp", realOutputFileName);
            } else {
                String realOutputFileName = options.isPassedIn("-o") ? (String) options.get("-o") : FileUtils.fixExtension((String) options.get("index"), ".contig", ".vcf.gz", ".vcf", ".gz");

                // 判断输出文件是否存在
                if (!options.isPassedIn("-y")) {
                    if (FileUtils.exists(realOutputFileName)) {
                        Scanner scanner = new Scanner(System.in);
                        System.out.print("WARN    " + realOutputFileName + " already exists, do you wish to overwrite? (y or n) ");

                        // 不覆盖文件，则删除该文件
                        if (!scanner.next().trim().equalsIgnoreCase("y")) {
                            throw new IOException("GBC can't create " + realOutputFileName + ": file exists");
                        }
                    }
                }

                ChromosomeInfo.build((String) options.get("index"), realOutputFileName + ".~$temp");
                FileUtils.rename(realOutputFileName + ".~$temp", realOutputFileName);
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