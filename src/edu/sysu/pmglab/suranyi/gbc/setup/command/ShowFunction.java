package edu.sysu.pmglab.suranyi.gbc.setup.command;

import edu.sysu.pmglab.suranyi.commandParser.CommandMatcher;
import edu.sysu.pmglab.suranyi.commandParser.CommandParser;
import edu.sysu.pmglab.suranyi.gbc.constant.ChromosomeInfo;
import edu.sysu.pmglab.suranyi.gbc.core.gtbcomponent.GTBRootCache;
import edu.sysu.pmglab.suranyi.gbc.core.gtbcomponent.ManagerStringBuilder;

import java.io.IOException;

/**
 * @author suranyi
 * @description 打印 GTB 信息模式解析器
 */

enum ShowFunction {
    /**
     * 单例模式
     */
    INSTANCE;

    private final CommandParser parser = ShowParser.getParser();

    public static int submit(String... args) throws IOException {
        CommandMatcher options = parse(args);

        if (options.isPassedIn("-h")) {
            System.out.println(usage());
        } else {
            // 加载染色体资源文件
            ChromosomeInfo.load((String) options.get("--contig"));

            // 仅打印样本信息
            if (options.isPassedIn("--list-subject-only")) {
                System.out.println(new String(GTBRootCache.get((String) options.get("show")).getSubjects()).replace("\t", ","));
                return 0;
            }

            ManagerStringBuilder builder = GTBRootCache.get((String) options.get("show")).getManagerStringBuilder();
            if (options.isPassedIn("--full")) {
                builder.calculateMd5(options.isPassedIn("--list-md5")).listFileBaseInfo(true).listSummaryInfo(true).listSubjects(true).listGTBTree(true);
            } else {
                builder.calculateMd5(options.isPassedIn("--list-md5")).listFileBaseInfo(options.isPassedIn("--list-baseInfo")).listSummaryInfo(true).listSubjects(options.isPassedIn("--list-subject"));

                if (options.isPassedIn("--list-tree") || options.isPassedIn("--list-node") || options.isPassedIn("--assign-chromosome")) {
                    if (options.isPassedIn("--list-node")) {
                        if (options.isPassedIn("--assign-chromosome")) {
                            builder.listGTBTree(true, (String[]) options.get("--assign-chromosome"));
                        } else {
                            builder.listGTBTree(true);
                        }
                    } else {
                        if (options.isPassedIn("--assign-chromosome")) {
                            builder.listChromosomeInfo(true, (String[]) options.get("--assign-chromosome"));
                        } else {
                            builder.listChromosomeInfo(true);
                        }
                    }
                }
            }

            System.out.println(builder.build());
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