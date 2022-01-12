package edu.sysu.pmglab.suranyi.gbc.setup.command;

import edu.sysu.pmglab.suranyi.commandParser.CommandMatcher;
import edu.sysu.pmglab.suranyi.commandParser.CommandParser;
import edu.sysu.pmglab.suranyi.gbc.constant.ChromosomeInfo;
import edu.sysu.pmglab.suranyi.gbc.core.gtbcomponent.GTBRootCache;
import edu.sysu.pmglab.suranyi.gbc.core.gtbcomponent.ManagerStringBuilder;
import edu.sysu.pmglab.suranyi.gbc.core.gtbcomponent.gtbreader.GTBReader;
import edu.sysu.pmglab.suranyi.gbc.core.gtbcomponent.gtbreader.Variant;

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

            // 仅打印样本信息
            if (options.isPassedIn("--list-subject-only")) {
                System.out.println(new String(GTBRootCache.get((String) options.get("show")).getSubjects()).replace("\t", ","));
                return 0;
            }

            // 仅打印位置信息
            if (options.isPassedIn("--list-position-only")) {
                GTBReader reader = new GTBReader((String) options.get("show"));
                reader.selectSubjects(new int[]{});

                if (options.isPassedIn("--assign-chromosome")) {
                    reader.limit((String[]) options.get("--assign-chromosome"));
                }

                for (Variant variant : reader) {
                    System.out.println(variant.chromosome + "\t" + variant.position);
                }

                return 0;
            }

            // 仅打印位置信息
            if (options.isPassedIn("--list-site")) {
                GTBReader reader = new GTBReader((String) options.get("show"));

                if (options.isPassedIn("--assign-chromosome")) {
                    reader.limit((String[]) options.get("--assign-chromosome"));
                }

                for (Variant variant : reader) {
                    System.out.println(variant.chromosome + "\t" + variant.position + "\t" + new String(variant.REF) + "\t" + new String(variant.ALT) + "\tAC=" + variant.getAC() + ";AF=" + variant.getAF() + ";AN=" + variant.getAN());
                }

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