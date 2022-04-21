package edu.sysu.pmglab.gbc.setup.command;

import dev.fromjavascript.BGZIPCommandEntry;
import edu.sysu.pmglab.commandParser.CommandMatcher;
import edu.sysu.pmglab.commandParser.CommandParser;

import java.io.IOException;

/**
 * @Data :2020/10/11
 * @Author :suranyi
 * @Contact :suranyi.sysu@gamil.com
 * @Description :程序入口点
 */

public enum GBCFunction {
    /**
     * 单例
     */
    INSTANCE;

    public final CommandParser parser = GBCParser.getParser();

    public static int submit(String[] args) throws IOException {
        // 解析参数
        try {
            if (args.length == INSTANCE.parser.getOffset()) {
                // 参数长度和偏移量相等，此时打印 help 文档
                System.out.println(INSTANCE.parser);
            } else {
                CommandMatcher options = INSTANCE.parser.parse(args);

                if (options.isPassedIn("build")) {
                    BuildFunction.submit(args);
                } else if (options.isPassedIn("rebuild")) {
                    RebuildFunction.submit(args);
                } else if (options.isPassedIn("extract")) {
                    ExtractFunction.submit(args);
                } else if (options.isPassedIn("edit")) {
                    EditFunction.submit(args);
                } else if (options.isPassedIn("merge")) {
                    MergeFunction.submit(args);
                } else if (options.isPassedIn("show")) {
                    ShowFunction.submit(args);
                } else if (options.isPassedIn("index")) {
                    IndexFunction.submit(args);
                } else if (options.isPassedIn("ld")) {
                    LDFunction.submit(args);
                } else if (options.isPassedIn("bgzip")) {
                    BGZIPCommandEntry.submit(args);
                } else if (options.isPassedIn("md5")) {
                    MD5Function.submit(args);
                } else if (options.isPassedIn("version")) {
                    System.out.println("Version: GBC-1.1 (last edited on 2022.01.15)");
                } else if (options.isPassedIn("-h")) {
                    System.out.println(INSTANCE.parser);
                }
            }
        } catch (Exception e) {
            System.out.println("ERROR   " + e.getMessage());
        }

        return 0;
    }
}
