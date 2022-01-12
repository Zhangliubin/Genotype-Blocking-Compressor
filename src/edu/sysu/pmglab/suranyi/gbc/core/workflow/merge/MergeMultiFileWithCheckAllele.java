package edu.sysu.pmglab.suranyi.gbc.core.workflow.merge;

import edu.sysu.pmglab.suranyi.container.SmartList;
import edu.sysu.pmglab.suranyi.easytools.ArrayUtils;
import edu.sysu.pmglab.suranyi.easytools.ByteCode;
import edu.sysu.pmglab.suranyi.gbc.core.build.MergeTask;
import edu.sysu.pmglab.suranyi.gbc.core.gtbcomponent.GTBManager;
import edu.sysu.pmglab.suranyi.gbc.core.gtbcomponent.gtbreader.GTBReader;
import edu.sysu.pmglab.suranyi.gbc.core.gtbcomponent.gtbreader.Variant;
import edu.sysu.pmglab.suranyi.gbc.core.gtbcomponent.gtbwriter.GTBWriter;
import edu.sysu.pmglab.suranyi.gbc.core.gtbcomponent.gtbwriter.GTBWriterBuilder;

import java.io.IOException;
import java.util.*;

/**
 * @author suranyi
 * @description
 */

public class MergeMultiFileWithCheckAllele {
    MergeTask task;
    static byte[] missAllele = new byte[]{ByteCode.PERIOD};
    static HashMap<Byte, Byte> complementaryBase = new HashMap<>();

    static {
        byte A = 65;
        byte T = 84;
        byte C = 67;
        byte G = 71;
        complementaryBase.put(A, T);
        complementaryBase.put(T, A);
        complementaryBase.put(C, G);
        complementaryBase.put(G, C);
    }

    MergeMultiFileWithCheckAllele(MergeTask task) throws IOException {
        this.task = task;

        // sort managers (large -> small)
        this.task.getManagers().sort((o1, o2) -> -Integer.compare(o1.getSubjectNum(), o2.getSubjectNum()));

        // start
        start();
    }

    public static void submit(MergeTask task) throws IOException {
        new MergeMultiFileWithCheckAllele(task);
    }

    void start() throws IOException {
        // 记录坐标，如果坐标太多，可以考虑分染色体读取 (使用 reader.limit 语句)
        HashMap<String, HashSet<Integer>>[] loadInPositions = new HashMap[this.task.getManagers().size()];

        for (int i = 0; i < this.task.getManagers().size(); i++) {
            GTBReader reader = new GTBReader(this.task.getManager(i));

            // 只检查坐标，因此不需要解压基因型
            reader.selectSubjects(new int[]{});

            loadInPositions[i] = new HashMap<>();
            for (Variant variant : reader) {
                if (!loadInPositions[i].containsKey(variant.chromosome)) {
                    loadInPositions[i].put(variant.chromosome, new HashSet<>());
                }

                loadInPositions[i].get(variant.chromosome).add(variant.position);
            }
            reader.close();
        }

        // 合并坐标
        if (this.task.isKeepAll()) {
            for (int i = 1; i < this.task.getManagers().size(); i++) {
                for (String chromosome : loadInPositions[i - 1].keySet()) {
                    if (!loadInPositions[i].containsKey(chromosome)) {
                        loadInPositions[i].put(chromosome, new HashSet<>());
                    }

                    loadInPositions[i].get(chromosome).addAll(loadInPositions[i - 1].get(chromosome));
                }
            }
        } else {
            int lastIndex = this.task.getManagers().size() - 1;
            for (int i = 0; i < lastIndex; i++) {
                for (String chromosome : loadInPositions[i].keySet()) {
                    if (!loadInPositions[lastIndex].containsKey(chromosome)) {
                        loadInPositions[lastIndex].put(chromosome, new HashSet<>());
                    }

                    loadInPositions[lastIndex].get(chromosome).addAll(loadInPositions[i].get(chromosome));
                }
                loadInPositions[i] = loadInPositions[lastIndex];
            }
        }

        // 校正等位基因
        int index = 0;
        while (index + 1 < this.task.getManagers().size()) {
            GTBReader reader1;
            if (index == 0) {
                reader1 = new GTBReader(this.task.getManager(0));
            } else {
                reader1 = new GTBReader(this.task.getOutputFileName() + "." + (index - 1) + " .~$temp");
            }

            GTBReader reader2 = new GTBReader(this.task.getManager(index + 1));
            GTBWriterBuilder writerBuilder = new GTBWriterBuilder(this.task.getOutputFileName() + "." + (index) + " .~$temp");
            writerBuilder.setSubject(ArrayUtils.merge(reader1.getAllSubjects(), reader2.getAllSubjects()));
            writerBuilder.setReference(reader1.getManager().getReference());
            GTBWriter writer = writerBuilder.build();

            // 元信息
            GTBManager manager1 = reader1.getManager();
            GTBManager manager2 = reader2.getManager();
            SmartList<Variant> variants1;
            SmartList<Variant> variants1Cache = new SmartList<>();
            SmartList<Variant> variants2;
            SmartList<Variant> variants2Cache = new SmartList<>();
            Variant mergeVariant = new Variant();
            mergeVariant.BEGs = new byte[manager1.getSubjectNum() + manager2.getSubjectNum()];

            for (int i = 0; i < 8; i++) {
                variants1Cache.add(new Variant());
                variants2Cache.add(new Variant());
            }

            for (String chromosome : loadInPositions[index].keySet()) {
                boolean condition1 = false;
                boolean condition2 = false;
                HashSet<Integer> position = loadInPositions[index].get(chromosome);

                if (manager1.contain(chromosome)) {
                    reader1.limit(chromosome);
                    reader1.seek(chromosome, 0);
                    variants1 = reader1.readVariants(variants1Cache, position);
                }

                if (manager2.contain(chromosome)) {
                    reader2.limit(chromosome);
                    reader2.seek(chromosome, 0);

                    // 读取位点
                    //condition2 = loadFromReader(reader2, variants2, variants2Cache, position);
                }
//
//                while (condition1 || condition2) {
//                    if (condition1 && !condition2) {
//                        // v1 有效位点, v2 无效位点
//                        Arrays.fill(mergeVariant.BEGs, variant1.BEGs.length, mergeVariant.BEGs.length, (byte) 0);
//                        do {
//                            mergeVariant.chromosome = variant1.chromosome;
//                            mergeVariant.position = variant1.position;
//                            mergeVariant.ploidy = variant1.ploidy;
//                            mergeVariant.phased = variant1.phased;
//                            mergeVariant.ALT = variant1.ALT;
//                            mergeVariant.REF = variant1.REF;
//                            System.arraycopy(variant1.BEGs, 0, mergeVariant.BEGs, 0, variant1.BEGs.length);
//                            writer.write(mergeVariant);
//                        } while (reader1.readVariant(variant1, position));
//                        condition1 = false;
//                    } else if (!condition1 && condition2) {
//                        // v2 有效位点, v1 无效位点
//                        Arrays.fill(mergeVariant.BEGs, 0, variant1.BEGs.length, (byte) 0);
//                        do {
//                            for (Variant variant2 : variants2) {
//                                mergeVariant.chromosome = variant2.chromosome;
//                                mergeVariant.position = variant2.position;
//                                mergeVariant.ploidy = variant2.ploidy;
//                                mergeVariant.phased = variant2.phased;
//                                mergeVariant.ALT = variant2.ALT;
//                                mergeVariant.REF = variant2.REF;
//                                System.arraycopy(variant2.BEGs, 0, mergeVariant.BEGs, variant1.BEGs.length, variant2.BEGs.length);
//                                writer.write(mergeVariant);
//                            }
//                            variants2Cache.add(variants2);
//                            variants2.clear();
//                        } while (loadFromReader(reader2, variants2, variants2Cache, position));
//                        condition2 = false;
//                    } else if (condition1 && condition2) {
//                        for (Variant variant2: variants2) {
//                            if (variant1.position == variant2.position) {
//                                // 合并位点
//                                boolean v1IsNormalAllele = isNormalAllele(variant1.ALT) && isNormalAllele(variant1.REF);
//                                boolean v2IsNormalAllele = isNormalAllele(variant2.ALT) && isNormalAllele(variant2.REF);
//
//                                if (v1IsNormalAllele && v2IsNormalAllele) {
//                                    // 标准碱基类型，执行位点合并
//                                    writer.write(mergeVariant(variant1, variant2, mergeVariant));
//                                } else {
//                                    // 包含奇奇怪怪的碱基类型，此时不合并位点
//                                    if (!v1IsNormalAllele) {
//                                        Arrays.fill(mergeVariant.BEGs, manager1.getSubjectNum(), mergeVariant.BEGs.length, (byte) 0);
//                                        mergeVariant.chromosome = variant1.chromosome;
//                                        mergeVariant.position = variant1.position;
//                                        mergeVariant.ploidy = variant1.ploidy;
//                                        mergeVariant.phased = variant1.phased;
//                                        mergeVariant.ALT = variant1.ALT;
//                                        mergeVariant.REF = variant1.REF;
//                                        writer.write(mergeVariant);
//                                        condition1 = reader1.readVariant(variant1, position);
//                                    }
//
//                                    if (!v2IsNormalAllele) {
//                                        Arrays.fill(mergeVariant.BEGs, 0, manager1.getSubjectNum(), (byte) 0);
//                                        mergeVariant.chromosome = variant2.chromosome;
//                                        mergeVariant.position = variant2.position;
//                                        mergeVariant.ploidy = variant2.ploidy;
//                                        mergeVariant.phased = variant2.phased;
//                                        mergeVariant.ALT = variant2.ALT;
//                                        mergeVariant.REF = variant2.REF;
//                                        writer.write(mergeVariant);
//                                        condition2 = reader2.readVariant(variant2, position);
//                                    }
//
//                                    continue;
//                                }
//
//                                condition2 = reader2.readVariant(variant2, position);
//                                if (condition2 && variant2.position == variant1.position) {
//                                    while (condition2 && variant2.position == variant1.position) {
//                                        // 仍然相等，需要记录指针 (说明 reader2 中有多个相同坐标的位点)
//                                        writer.write(mergeVariant(variant1, variant2, mergeVariant));
//                                        condition2 = reader2.readVariant(variant2, position);
//                                    }
//
//                                    reader2.seek(pointer2);
//                                }
//                                condition1 = reader1.readVariant(variant1, position);
//                            } else if (variant1.position < variant2.position) {
//                                // 写入位点 1
//                                Arrays.fill(mergeVariant.BEGs, manager1.getSubjectNum(), mergeVariant.BEGs.length, (byte) 0);
//                                mergeVariant.chromosome = variant1.chromosome;
//                                mergeVariant.position = variant1.position;
//                                mergeVariant.ploidy = variant1.ploidy;
//                                mergeVariant.phased = variant1.phased;
//                                mergeVariant.ALT = variant1.ALT;
//                                mergeVariant.REF = variant1.REF;
//                                writer.write(mergeVariant);
//                                condition1 = reader1.readVariant(variant1, position);
//                            } else {
//                                // variant1.position > variant2.position
//                                Arrays.fill(mergeVariant.BEGs, 0, manager1.getSubjectNum(), (byte) 0);
//                                mergeVariant.chromosome = variant2.chromosome;
//                                mergeVariant.position = variant2.position;
//                                mergeVariant.ploidy = variant2.ploidy;
//                                mergeVariant.phased = variant2.phased;
//                                mergeVariant.ALT = variant2.ALT;
//                                mergeVariant.REF = variant2.REF;
//                                writer.write(mergeVariant);
//                                condition2 = reader2.readVariant(variant2, position);
//                            }
//                        }
//                    }
//                }
//
//                index++;
            }
//            out:
//            for (Variant variant1 : reader1) {
//                int chromosome1 = ChromosomeInfo.getIndex(variant1.chromosome);
//                int chromosome2 = ChromosomeInfo.getIndex(variant2.chromosome);
//
//                if (chromosome1 < chromosome2) {
//                    // 染色体编号不一致
//                    if (loadInPositions[index].containsKey(variant1.chromosome)) {
//                        if (loadInPositions[index].get(variant1.chromosome).contains(variant1.position)) {
//                            writer.write(variant1.merge(variant2.BEGs.length));
//                        }
//                    }
//
//                    continue;
//                } else {
//                    while (chromosome1 > chromosome2) {
//                        pointer2 = reader2.tell();
//
//                        if (reader2.readVariant(variant2)) {
//                            chromosome2 = ChromosomeInfo.getIndex(variant2.chromosome);
//                        } else {
//                            // 文件 2 已经结束了
//                            break out;
//                        }
//                    }
//
//                    // 判断 position 的值
//                    if (variant1.position < variant2.position) {
//                        continue out;
//                    } else {
//                        while (variant2.position < variant1.position) {
//                            if (reader2.readVariant(variant2)) {
//                                chromosome2 = ChromosomeInfo.getIndex(variant2.chromosome);
//
//                                if (chromosome1 != chromosome2) {
//                                    continue out;
//                                }
//                            } else {
//                                // 文件 2 已经结束了
//                                break out;
//                            }
//                        }
//
//                        // position 一致，此时比对 allele 信息
//                        writer.write(mergeVariant(variant1, variant2));
//                    }
//                }
//            }
//
//            reader1.close();
//            reader2.close();
//            writer.close();
//            GTBRootCache.clear();
//            index++;
        }
    }

    Variant mergeVariant(Variant variant1, Variant variant2, Variant target) {
        boolean variant1AltIsMiss = false;
        boolean variant2AltIsMiss = false;

        if (Arrays.equals(variant1.ALT, missAllele)) {
            variant1AltIsMiss = true;
        }

        if (Arrays.equals(variant2.ALT, missAllele)) {
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

    boolean alleleFrequencyEqual(double af1, double af2) {
        return (Math.abs(af1 - af2) <= this.task.getIdentityAF());
    }

    boolean alleleEqual(byte[] allele1, byte[] allele2) {
        return Arrays.equals(allele1, allele2) || Arrays.equals(allele1, getInverseAllele(allele2));
    }

    byte[] getInverseAllele(byte[] allele) {
        byte[] alleleInverse = new byte[allele.length];
        for (int i = 0; i < alleleInverse.length; i++) {
            if (complementaryBase.containsKey((int) allele[i])) {
                int code = complementaryBase.get((int) allele[i]);
                alleleInverse[i] = (byte) code;
            } else {
                alleleInverse[i] = (byte) allele[i];
            }
        }

        return alleleInverse;
    }

    boolean isNormalAllele(byte[] allele) {
        byte[] alleleInverse = new byte[allele.length];
        for (int i = 0; i < alleleInverse.length; i++) {
            if (!complementaryBase.containsKey(allele[i]) && allele[i] != ByteCode.COMMA && allele[i] != ByteCode.PERIOD) {
                // 不为 . 和 ,
                return false;
            }
        }

        return true;
    }

    public static void main(String[] args) throws IOException {
        GTBReader reader = new GTBReader("/Users/suranyi/Documents/project/GBC/GBC-1.1/example/1.gtb");
        SmartList<Variant> variants = new SmartList<>();
        SmartList<Variant> variantsCache = new SmartList<>();
        Map<Integer, Set<Integer>>  sets = new HashMap<>();
        sets.put(4, new HashSet<>());
        sets.get(4).add(289676);
        System.out.println(Arrays.toString(reader.readVariants()));
        System.out.println(Arrays.toString(reader.readVariants()));
        System.out.println(reader.readVariant());
        // System.out.println(Arrays.toString(reader.readVariants(sets)));
    }
}
