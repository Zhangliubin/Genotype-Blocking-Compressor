package edu.sysu.pmglab.gbc.setup.command;

import edu.sysu.pmglab.gbc.constant.ChromosomeTags;
import edu.sysu.pmglab.gbc.core.gtbcomponent.GTBManager;
import edu.sysu.pmglab.gbc.core.gtbcomponent.GTBRootCache;
import edu.sysu.pmglab.commandParser.CommandMatcher;
import edu.sysu.pmglab.commandParser.CommandParser;
import edu.sysu.pmglab.easytools.FileUtils;

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
        if (args.length == INSTANCE.parser.getOffset() + 1) {
            // 参数长度和偏移量相等，此时打印 help 文档
            System.out.println(INSTANCE.parser);
            return 0;
        }

        CommandMatcher options = parse(args);

        if (options.isPassedIn("-h")) {
            System.out.println(usage());
        } else {
            ChromosomeTags.load((String) options.get("-from"));
            boolean isGTB = false;

            // 判断是否为 GTB 文件，如果不是 GTB 文件，则使用转换方法
            try {
                GTBManager manager = GTBRootCache.get((String) options.get("index"));
                isGTB = true;
            } catch (Exception ignored) {
                isGTB = false;
            }

            if (isGTB) {
                // 重叠群转换
                String realOutputFileName = options.isPassedIn("-o") ? (String) options.get("-o") : (String) options.get("index");

                if (!(boolean) options.isPassedIn("-y")) {
                    if (FileUtils.exists(realOutputFileName)) {
                        Scanner scanner = new Scanner(System.in);
                        System.out.print("WARN    " + realOutputFileName + " already exists, do you wish to overwrite? (y or n) ");

                        // 不覆盖文件，则删除该文件
                        if (!"y".equalsIgnoreCase(scanner.next().trim())) {
                            throw new IOException("GBC can't create " + realOutputFileName + ": file exists");
                        }
                    }
                }

                // 打开 GTB 管理器
                GTBManager manager = GTBRootCache.get((String) options.get("index"));

                // 重设重叠群信息
                manager.resetContig((String) options.get("-to"));

                // 输出文件
                manager.toFile(realOutputFileName + ".~$temp");

                // 修改文件名
                FileUtils.rename(realOutputFileName + ".~$temp", realOutputFileName);
            } else {
                // build contig for VCF
                String realOutputFileName = options.isPassedIn("-o") ? (String) options.get("-o") : FileUtils.fixExtension((String) options.get("index"), ".contig", ".vcf.gz", ".vcf", ".gz");

                // 判断输出文件是否存在
                if (!options.isPassedIn("-y")) {
                    if (FileUtils.exists(realOutputFileName)) {
                        Scanner scanner = new Scanner(System.in);
                        System.out.print("WARN    " + realOutputFileName + " already exists, do you wish to overwrite? (y or n) ");

                        // 不覆盖文件，则删除该文件
                        if (!"y".equalsIgnoreCase(scanner.next().trim())) {
                            throw new IOException("GBC can't create " + realOutputFileName + ": file exists");
                        }
                    }
                }

                ChromosomeTags.build((String) options.get("index"), realOutputFileName + ".~$temp", options.isPassedIn("--deep-scan"));
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