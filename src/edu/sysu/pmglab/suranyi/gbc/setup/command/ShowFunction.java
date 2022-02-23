package edu.sysu.pmglab.suranyi.gbc.setup.command;

import edu.sysu.pmglab.suranyi.commandParser.CommandMatcher;
import edu.sysu.pmglab.suranyi.commandParser.CommandParser;
import edu.sysu.pmglab.suranyi.gbc.coder.decoder.BEGDecoder;
import edu.sysu.pmglab.suranyi.gbc.coder.encoder.BEGEncoder;
import edu.sysu.pmglab.suranyi.gbc.constant.ChromosomeInfo;
import edu.sysu.pmglab.suranyi.gbc.core.gtbcomponent.GTBRootCache;
import edu.sysu.pmglab.suranyi.gbc.core.gtbcomponent.ManagerStringBuilder;
import edu.sysu.pmglab.suranyi.gbc.core.gtbcomponent.gtbreader.GTBReader;
import edu.sysu.pmglab.suranyi.gbc.core.gtbcomponent.gtbreader.Variant;
import edu.sysu.pmglab.suranyi.gbc.core.gtbcomponent.gtbreader.formatter.VariantFormatter;

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
                GTBReader reader = new GTBReader((String) options.get("show"), false, false);

                if (options.isPassedIn("--assign-chromosome")) {
                    reader.limit((String[]) options.get("--assign-chromosome"));
                }

                for (Variant variant : reader) {
                    System.out.println(variant.chromosome + "\t" + variant.position);
                }

                return 0;
            }

            // 仅打印位点的位置、碱基信息
            if (options.isPassedIn("--list-site")) {
                GTBReader reader = new GTBReader((String) options.get("show"));

                if (options.isPassedIn("--assign-chromosome")) {
                    reader.limit((String[]) options.get("--assign-chromosome"));
                }

                // 打印基因型频率
                if (options.isPassedIn("--list-gt")) {
                    for (Variant variant : reader) {
                        String gtInfo = variant.apply(new VariantFormatter<Void, String>() {
                            @Override
                            public String apply(Variant variant) {
                                int alleleNums = variant.getAlternativeAlleleNum();
                                int[] counts = variant.getGenotypeCounts();

                                StringBuilder builder = new StringBuilder();
                                BEGDecoder decoder = BEGDecoder.getDecoder(variant.phased);
                                BEGEncoder encoder = BEGEncoder.getEncoder(variant.phased);
                                if (variant.phased) {
                                    if (variant.ploidy == 1) {
                                        // 单倍型时
                                        builder.append(";").append(".").append("=" + counts[0]);

                                        for (int i = 0; i < alleleNums; i++) {
                                            int code = encoder.encode(i, i);
                                            builder.append(";").append(new String(decoder.decode(variant.ploidy, code))).append("=" + counts[code]);
                                        }
                                    } else {
                                        for (int i = 0; i < counts.length; i++) {
                                            builder.append(";").append(new String(decoder.decode(variant.ploidy, i))).append("=" + counts[i]);
                                        }
                                    }
                                } else {
                                    // unphased 时一部分基因型是没有的
                                    if (variant.ploidy == 1) {
                                        // 单倍型时
                                        builder.append(";").append(".").append("=" + counts[0]);

                                        for (int i = 0; i < alleleNums; i++) {
                                            int code = encoder.encode(i, i);
                                            builder.append(";").append(new String(decoder.decode(variant.ploidy, code))).append("=" + counts[code]);
                                        }
                                    } else {
                                        builder.append(";").append("./.").append("=" + counts[0]);
                                        for (int i = 0; i < alleleNums; i++) {
                                            for (int j = i; j < alleleNums; j++) {
                                                int code = encoder.encode(i, j);
                                                builder.append(";").append(new String(decoder.decode(variant.ploidy, code))).append("=" + counts[code]);
                                            }
                                        }
                                    }
                                }

                                return builder.toString();
                            }
                        });
                        System.out.println(variant.chromosome + "\t" + variant.position + "\t" + new String(variant.REF) + "\t" + new String(variant.ALT) + "\tAC=" + variant.getAC() + ";AF=" + String.format("%.8f", variant.getAF()) + ";AN=" + variant.getAN() + gtInfo);
                    }
                } else {
                    for (Variant variant : reader) {
                        System.out.println(variant.chromosome + "\t" + variant.position + "\t" + new String(variant.REF) + "\t" + new String(variant.ALT) + "\tAC=" + variant.getAC() + ";AF=" + String.format("%.8f", variant.getAF()) + ";AN=" + variant.getAN());
                    }
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