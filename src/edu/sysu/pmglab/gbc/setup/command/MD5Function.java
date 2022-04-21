package edu.sysu.pmglab.gbc.setup.command;

import edu.sysu.pmglab.commandParser.CommandMatcher;
import edu.sysu.pmglab.commandParser.CommandParser;
import edu.sysu.pmglab.easytools.FileUtils;
import edu.sysu.pmglab.easytools.MD5;
import edu.sysu.pmglab.unifyIO.FileStream;
import edu.sysu.pmglab.unifyIO.options.FileOptions;

import java.io.IOException;
import java.util.Scanner;

/**
 * @author suranyi
 * @description MD5 码计算模式解析器
 */

enum MD5Function {
    /**
     * 单例模式
     */
    INSTANCE;

    private final CommandParser parser = MD5Parser.getParser();

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
            // 解析命令
            for (String fileName : (String[]) options.get("md5")) {
                String md5Code = MD5.check(fileName);
                if ((boolean) options.get("--o-md5")) {
                    if (options.isPassedIn("-y")) {
                        try (FileStream fs = new FileStream(fileName + ".md5", FileOptions.DEFAULT_WRITER)) {
                            fs.write(md5Code);
                        }
                    } else {
                        if (FileUtils.exists(fileName + ".md5")) {
                            Scanner scanner = new Scanner(System.in);
                            System.out.print("WARN    " + fileName + ".md5" + " already exists, do you wish to overwrite? (y or n) ");

                            // 不覆盖文件，则删除该文件
                            if (!"y".equalsIgnoreCase(scanner.next().trim())) {
                                throw new IOException("GBC can't create " + fileName + ": file exists");
                            }
                        }

                        try (FileStream fs = new FileStream(fileName + ".md5", FileOptions.DEFAULT_WRITER)) {
                            fs.write(md5Code);
                        }
                    }
                }
                System.out.printf("MD5 (%s) = %s%n", fileName, md5Code);
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