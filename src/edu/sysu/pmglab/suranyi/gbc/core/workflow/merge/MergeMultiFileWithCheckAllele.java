package edu.sysu.pmglab.suranyi.gbc.core.workflow.merge;

import edu.sysu.pmglab.suranyi.easytools.ArrayUtils;
import edu.sysu.pmglab.suranyi.easytools.FileUtils;
import edu.sysu.pmglab.suranyi.gbc.constant.ChromosomeInfo;
import edu.sysu.pmglab.suranyi.gbc.core.build.BuildTask;
import edu.sysu.pmglab.suranyi.gbc.core.extract.ExtractTask;
import edu.sysu.pmglab.suranyi.gbc.core.gtbcomponent.GTBRootCache;
import edu.sysu.pmglab.suranyi.gbc.core.gtbcomponent.gtbreader.GTBReader;
import edu.sysu.pmglab.suranyi.gbc.core.gtbcomponent.gtbreader.Variant;
import edu.sysu.pmglab.suranyi.gbc.core.gtbcomponent.gtbwriter.GTBWriter;
import edu.sysu.pmglab.suranyi.gbc.core.gtbcomponent.gtbwriter.GTBWriterBuilder;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

/**
 * @author suranyi
 * @description
 */

class MergeMultiFileWithCheckAllele {
    public static void main(String[] args) {

        try {
            // 解析输入的参数
            Options.parse(args);

            // 产生具有相交坐标的新文件
            convertInputToGTB();

            // 产生具有相交坐标的新文件
            createMergeCoordinate();

            // 校正等位基因
            checkAllele();

            // 输出文件
            outputAsVCF();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static void convertInputToGTB() throws IOException {
        // 压缩输入的文件
        for (int i = 0; i < Options.fileNums; i++) {
            if (Options.inputFileNames[i].endsWith(".vcf") || Options.inputFileNames[i].endsWith(".gz")) {
                String temp = FileUtils.fixExtension(Options.inputFileNames[i], ".gtb", ".vcf.gz", ".vcf", ".gz");

                // 压缩 GTB 文件
                BuildTask compressor = new BuildTask(Options.inputFileNames[i], temp);
                compressor.setPhased(Options.phased);
                compressor.submit();

                // 替换输入文件名
                Options.inputFileNames[i] = temp;
            }
        }
    }

    /**
     * 剔除不重叠的坐标
     */
    static void createMergeCoordinate() throws IOException {
        // 记录坐标，如果坐标太多，可以考虑分染色体读取 (使用 reader.limit 语句)
        HashMap<String, HashSet<Integer>>[] positions = new HashMap[Options.fileNums];
        for (int i = 0; i < Options.fileNums; i++) {
            GTBReader reader = new GTBReader(Options.inputFileNames[i]);

            // 只检查坐标，因此不需要解压基因型
            reader.selectSubjects(new int[]{});

            positions[i] = new HashMap<>();
            for (Variant variant : reader) {
                if (!positions[i].containsKey(variant.chromosome)) {
                    positions[i].put(variant.chromosome, new HashSet<>());
                }

                positions[i].get(variant.chromosome).add(variant.position);
            }
            reader.close();
        }

        // 合并坐标
        for (int chromosomeIndex = 0; chromosomeIndex < ChromosomeInfo.supportedChromosomeList().length; chromosomeIndex++) {
            String chromosome = ChromosomeInfo.getString(chromosomeIndex);
            for (int i = 1; i < Options.fileNums; i++) {
                if (positions[i].get(chromosome) != null) {
                    positions[0].get(chromosome).retainAll(positions[i].get(chromosome));
                }
            }
        }
        HashMap<String, HashSet<Integer>> finalPositions = positions[0];

        // 根据合并坐标提取新文件
        for (int i = 0; i < Options.fileNums; i++) {
            GTBWriterBuilder builder = new GTBWriterBuilder(FileUtils.fixExtension(Options.inputFileNames[i], ".temp.gtb", ".gtb"));
            builder.setSubject(GTBRootCache.get(Options.inputFileNames[i]).getSubjects());
            builder.setPhased(Options.phased);
            GTBWriter writer = builder.build();
            GTBReader reader = new GTBReader(Options.inputFileNames[i]);

            for (Variant variant : reader) {
                if (finalPositions.containsKey(variant.chromosome) && finalPositions.get(variant.chromosome).contains(variant.position)) {
                    writer.write(variant);
                }
            }
            writer.close();
            reader.close();
            Options.inputFileNames[i] = builder.getOutputFileName();
        }
    }

    static void checkAllele() throws Exception {
        // 校正等位基因
        int index = 0;
        while (index + 1 < Options.fileNums) {
            GTBReader reader1;
            if (index == 0) {
                reader1 = new GTBReader(Options.inputFileNames[index]);
            } else {
                reader1 = new GTBReader(Options.outputDir + "/merge.gtb");
            }

            GTBReader reader2 = new GTBReader(Options.inputFileNames[index + 1]);
            GTBWriterBuilder writerBuilder = new GTBWriterBuilder(Options.outputDir + "/merge.gtb.~$temp");
            writerBuilder.setSubject(ArrayUtils.merge(reader1.getAllSubjects(), reader2.getAllSubjects()));
            writerBuilder.setReference(reader1.getManager().getReference());
            GTBWriter writer = writerBuilder.build();

            // 读取 2 号文件的第一个位点
            Variant variant2 = reader2.readVariant();

            // 校正等位基因
            out:
            for (Variant variant1 : reader1) {
                int chromosome1 = ChromosomeInfo.getIndex(variant1.chromosome);
                int chromosome2 = ChromosomeInfo.getIndex(variant2.chromosome);

                if (chromosome1 < chromosome2) {
                    continue out;
                } else {
                    while (chromosome1 > chromosome2) {
                        if (reader2.readVariant(variant2)) {
                            chromosome2 = ChromosomeInfo.getIndex(variant2.chromosome);
                        } else {
                            // 文件 2 已经结束了
                            break out;
                        }
                    }

                    // 判断 position 的值
                    if (variant1.position < variant2.position) {
                        continue out;
                    } else {
                        while (variant2.position < variant1.position) {
                            if (reader2.readVariant(variant2)) {
                                chromosome2 = ChromosomeInfo.getIndex(variant2.chromosome);

                                if (chromosome1 != chromosome2) {
                                    continue out;
                                }
                            } else {
                                // 文件 2 已经结束了
                                break out;
                            }
                        }

                        // position 一致，此时比对 allele 信息
                        writer.write(mergeVariant(variant1, variant2));
                    }
                }
            }

            reader1.close();
            reader2.close();
            writer.close();
            FileUtils.rename(Options.outputDir + "/merge.gtb.~$temp", Options.outputDir + "/merge.gtb");
            GTBRootCache.clear();
            index++;
        }

        // 最后删除所有的临时 GTB 文件
        for (int i = 0; i < Options.fileNums; i++) {
            FileUtils.delete(Options.inputFileNames[i]);
        }
    }

    static Variant mergeVariant(Variant variant1, Variant variant2) {
        boolean variant1AltIsMiss = false;
        boolean variant2AltIsMiss = false;

        if (Arrays.equals(variant1.ALT, Options.missAllele)) {
            variant1AltIsMiss = true;
        }

        if (Arrays.equals(variant2.ALT, Options.missAllele)) {
            variant2AltIsMiss = true;
        }

        if (Arrays.equals(variant1.REF, variant2.REF) && Arrays.equals(variant1.ALT, variant2.ALT)) {
            // 碱基序列一致
            if (!variant1AltIsMiss) {
                // 查看是否有反转可能  A T   A T, 但其实是相反的
                if (!alleleFrequencyEqual(1 - variant1.getAF(), 1 - variant2.getAF()) && alleleFrequencyEqual(variant1.getAF(), 1 - variant2.getAF()) &&
                        Arrays.equals(variant1.ALT, getInverseAllele(variant2.REF))) {
                    // 翻转
                    variant2.resetAlleles(variant1.ALT, variant1.REF);
                    variant2.ALT = variant1.ALT;
                    variant2.REF = variant1.REF;
                }
            }
        } else {
            double af10 = 1 - variant1.getAF();
            double af11 = 1 - af10;
            double af20 = 1 - variant2.getAF();
            double af21 = 1 - af20;

            if (alleleFrequencyEqual(af10, af20) && alleleEqual(variant1.REF, variant2.REF)) {
                variant2.REF = variant1.REF;
            }

            if (!variant1AltIsMiss && !variant2AltIsMiss && alleleFrequencyEqual(af11, af21) && alleleEqual(variant1.ALT, variant2.ALT)) {
                variant2.ALT = variant1.ALT;
            }

            if (variant1AltIsMiss && alleleFrequencyEqual(af10, af21) && alleleEqual(variant1.REF, variant2.ALT)) {
                variant2.ALT = variant1.REF;
            }

            if (variant2AltIsMiss && alleleFrequencyEqual(af11, af20) && alleleEqual(variant1.ALT, variant2.REF)) {
                variant2.REF = variant1.ALT;
            }

        }
        return variant1.merge(variant2);
    }

    static void outputAsVCF() throws IOException {
        ExtractTask task = new ExtractTask(Options.outputDir + "/merge.gtb", Options.outputDir + "/merge.vcf.gz");
        task.decompressAll();
    }

    static boolean alleleFrequencyEqual(double af1, double af2) {
        return (Math.abs(af1 - af2) <= 0.1);
    }

    static boolean alleleEqual(byte[] allele1, byte[] allele2) {
        return Arrays.equals(allele1, allele2) || Arrays.equals(allele1, getInverseAllele(allele2));
    }

    static byte[] getInverseAllele(byte[] allele) {
        byte[] alleleInverse = new byte[allele.length];
        for (int i = 0; i < alleleInverse.length; i++) {
            if (Options.complementaryBase.containsKey((int) allele[i])) {
                int code = Options.complementaryBase.get((int) allele[i]);
                alleleInverse[i] = (byte) code;
            } else {
                alleleInverse[i] = (byte) allele[i];
            }
        }

        return alleleInverse;
    }
}
