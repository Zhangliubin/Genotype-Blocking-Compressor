package edu.sysu.pmglab.suranyi.gbc.core.build;

import edu.sysu.pmglab.suranyi.container.SmartList;
import edu.sysu.pmglab.suranyi.easytools.ByteCode;
import edu.sysu.pmglab.suranyi.easytools.FileUtils;
import edu.sysu.pmglab.suranyi.gbc.constant.ChromosomeInfo;
import edu.sysu.pmglab.suranyi.gbc.core.common.allelechecker.AlleleChecker;
import edu.sysu.pmglab.suranyi.gbc.core.common.qualitycontrol.allele.AlleleQC;
import edu.sysu.pmglab.suranyi.gbc.core.common.qualitycontrol.variant.VariantQC;
import edu.sysu.pmglab.suranyi.gbc.core.gtbcomponent.GTBManager;
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
 * @description 文件标准化 (位点拆分与合并)
 */

class NormKernel {
    final VariantQC variantQC;
    final AlleleQC alleleQC;
    final RebuildTask task;
    final AlleleChecker alleleChecker;
    final HashSet<String>[] loadInChromosomes;

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

    NormKernel(RebuildTask task) throws IOException {
        this.task = task;
        this.alleleChecker = this.task.getAlleleChecker();

        // 记录坐标
        this.loadInChromosomes = recordChromosome();

        // 获取位点 QC
        this.variantQC = this.task.getVariantQC();
        this.alleleQC = this.task.getAlleleQC();

        // 开始工作
        if (this.alleleChecker == null) {
            startWithoutAlleleCheck();
        } else {
            startWithAlleleCheck();
        }
    }

    public static void submit(RebuildTask task) throws IOException {
        new NormKernel(task);
    }

    HashSet<String>[] recordChromosome() {
        // 记录坐标，如果坐标太多，可以考虑分染色体读取 (使用 reader.limit 语句)
        HashSet<String>[] loadInChromosomes = new HashSet[2];

        loadInChromosomes[0] = new HashSet<>();
        for (int chromosome : this.task.getTemplateFile().getChromosomeList()) {
            loadInChromosomes[0].add(ChromosomeInfo.getString(chromosome));
        }

        loadInChromosomes[1] = new HashSet<>();
        for (int chromosome : this.task.getInputFile().getChromosomeList()) {
            loadInChromosomes[1].add(ChromosomeInfo.getString(chromosome));
        }

        if (this.task.isKeepAll()) {
            // 合并所有坐标
            loadInChromosomes[1].addAll(loadInChromosomes[0]);
        } else {
            // 先在染色体水平取交集
            loadInChromosomes[1].retainAll(loadInChromosomes[0]);
            loadInChromosomes[0] = loadInChromosomes[1];
        }

        return loadInChromosomes;
    }

    HashSet<Integer> recordPosition(GTBReader reader1, GTBReader reader2, String chromosome) throws IOException {
        if (this.task.isKeepAll()) {
            return null;
        } else {
            HashSet<Integer> loadInPosition1 = new HashSet<>();

            reader1 = new GTBReader(reader1.getManager(), false, false);
            reader1.limit(chromosome);
            reader1.seek(chromosome, 0);

            // 先加载第一个文件全部的位点
            for (Variant variant : reader1) {
                loadInPosition1.add(variant.position);
            }

            reader1.close();

            // 再移除第二个文件中没有的位点
            reader2 = new GTBReader(reader2.getManager(), false, false);
            reader2.limit(chromosome);
            reader2.seek(chromosome, 0);

            HashSet<Integer> loadInPosition2 = new HashSet<>();
            for (Variant variant : reader2) {
                if (loadInPosition1.contains(variant.position)) {
                    loadInPosition2.add(variant.position);
                }
            }

            reader2.close();
            if (loadInPosition1.size() == loadInPosition2.size()) {
                // 取了交集后元素数量一致，则转为不做约束
                loadInPosition2.clear();
                loadInPosition2 = null;
            }

            loadInPosition1.clear();
            loadInPosition1 = null;

            return loadInPosition2;
        }
    }

    void startWithAlleleCheck() throws IOException {
        // 校正等位基因
        GTBReader reader1 = new GTBReader(this.task.getTemplateFile(), this.task.getTemplateFile().isPhased(), false);
        GTBReader reader2 = new GTBReader(this.task.getInputFile());
        if (this.task.getSubjects() != null) {
            reader2.selectSubjects(this.task.getSubjects());
        }
        GTBWriterBuilder writerBuilder = new GTBWriterBuilder(this.task.isInplace() ? this.task.getOutputFileName() + ".~$temp" : this.task.getOutputFileName());
        writerBuilder.setPhased(this.task.isPhased());
        writerBuilder.setSubject(reader2.getAllSubjects());
        writerBuilder.setReference(reader1.getManager().getReference());
        writerBuilder.setCompressor(this.task.getCompressor(), this.task.getCompressionLevel());
        writerBuilder.setReordering(this.task.isReordering());
        writerBuilder.setWindowSize(this.task.getWindowSize());
        writerBuilder.setBlockSizeType(this.task.getBlockSizeType());
        writerBuilder.setThreads(this.task.getThreads());
        GTBWriter writer = writerBuilder.build();

        // 元信息
        GTBManager manager1 = reader1.getManager();
        GTBManager manager2 = reader2.getManager();
        SmartList<Variant> variants1 = null;
        SmartList<Variant> variants1Cache = new SmartList<>();
        SmartList<Variant> variants2 = null;
        SmartList<Variant> variants2Cache = new SmartList<>();

        for (int i = 0; i < 1; i++) {
            variants1Cache.add(new Variant());
            variants2Cache.add(new Variant());
        }

        for (String chromosome : loadInChromosomes[1]) {
            boolean condition1 = false;
            boolean condition2 = false;
            HashSet<Integer> position = recordPosition(reader1, reader2, chromosome);

            if (manager1.contain(chromosome)) {
                reader1.limit(chromosome);
                reader1.seek(chromosome, 0);
                variants1 = reader1.readVariants(variants1Cache, position);
                condition1 = variants1 != null;
            }

            if (manager2.contain(chromosome)) {
                reader2.limit(chromosome);
                reader2.seek(chromosome, 0);
                variants2 = reader2.readVariants(variants2Cache, position);
                condition2 = variants2 != null;
            }

            while (condition1 || condition2) {
                if (condition1 && !condition2) {
                    break;
                } else if (!condition1) {
                    // v2 有效位点, v1 无效位点
                    do {
                        for (Variant variant2 : variants2) {
                            writeToFile(variant2, writer);
                        }
                        variants2Cache.add(variants2);
                        variants2.clear();
                        variants2 = reader2.readVariants(variants2Cache, position);
                        condition2 = variants2 != null;
                    } while (condition2);
                } else {
                    // 都有有效位点, 此时先进行位置大小的比较
                    int position1 = variants1.get(0).position;
                    int position2 = variants2.get(0).position;
                    int compareStatue = position1 - position2;
                    if (compareStatue < 0) {
                        // 写入所有的 1 位点，并移动 1 的指针
                        do {
                            variants1Cache.add(variants1);
                            variants1.clear();
                            variants1 = reader1.readVariants(variants1Cache, position);
                            condition1 = variants1 != null;
                            position1 = condition1 ? variants1.get(0).position : -1;
                        } while (condition1 && position1 < position2);
                    } else if (compareStatue > 0) {
                        // 写入所有的 2 位点，并移动 2 的指针
                        do {
                            for (Variant variant2 : variants2) {
                                writeToFile(variant2, writer);
                            }
                            variants2Cache.add(variants2);
                            variants2.clear();
                            variants2 = reader2.readVariants(variants2Cache, position);
                            condition2 = variants2 != null;
                            position2 = condition2 ? variants2.get(0).position : -1;
                        } while (condition2 && position1 > position2);
                    } else {
                        // 位点位置值一样
                        int sizeVariants1 = variants1.size();
                        int sizeVariants2 = variants2.size();
                        if (sizeVariants1 == sizeVariants2 && sizeVariants1 == 1) {
                            // 只有一个位点，直接比对
                            Variant variant1 = variants1.get(0);
                            Variant variant2 = variants2.get(0);
                            if (alignVariantWithAlleleCheck(variant1, variant2) != null) {
                                writer.write(variant2);
                            }
                        } else {
                            // 多对多 (先找一致项，不一致的再进行匹配)
                            SmartList<Variant> variants1new = new SmartList<>();

                            out:
                            for (int i = 0; i < variants1.size(); i++) {
                                Variant variant1 = variants1.get(i);
                                for (int j = 0; j < variants2.size(); j++) {
                                    Variant variant2 = variants2.get(j);
                                    if ((Arrays.equals(variant1.REF, variant2.REF) && Arrays.equals(variant1.ALT, variant2.ALT)) ||
                                            (Arrays.equals(variant1.REF, variant2.ALT) && Arrays.equals(variant1.ALT, variant2.REF))) {
                                        // 基本逻辑: 完全一致的碱基序列的位点直接合并，并且只合并一次
                                        if (alignVariantWithAlleleCheck(variant1, variant2) != null) {
                                            writer.write(variant2);
                                        }
                                        variants2.remove(variant2);
                                        variants2Cache.add(variant2);
                                        continue out;
                                    }
                                }

                                // 没有遇到匹配的，此时记录下该位点
                                variants1new.add(variant1);
                            }

                            while (true) {
                                if (variants1new.size() == 0 && variants2.size() == 0) {
                                    // 所有来自文件 1 和 文件 2 的位点都被匹配完成
                                    break;
                                } else if (variants1new.size() == 0) {
                                    // 所有来自文件 1 的位点都被匹配完成，此时文件 2 直接写入
                                    for (Variant variant2 : variants2) {
                                        writeToFile(variant2, writer);
                                    }

                                    break;
                                } else if (variants2.size() == 0) {
                                    // 所有来自文件 2 的位点都被匹配完成，此时文件 1 直接写入
                                    break;
                                } else {
                                    // 文件 1 和文件 2 都有位点，此时按照顺序匹配
                                    SmartList<Variant> variants1NewTemp = new SmartList<>();
                                    for (int i = 0; i < variants1new.size(); i++) {
                                        Variant variant1 = variants1new.get(i);
                                        if (variants2.size() > 0) {
                                            Variant variant2 = variants2.popFirst();
                                            if (alignVariantWithAlleleCheck(variant1, variant2) != null) {
                                                writer.write(variant2);
                                            }
                                            variants2Cache.add(variant2);
                                            continue;
                                        }

                                        // 没有遇到匹配的，此时记录下该位点
                                        variants1NewTemp.add(variant1);
                                    }
                                    variants1new = variants1NewTemp;
                                }
                            }
                        }

                        variants1Cache.add(variants1);
                        variants1.clear();
                        variants1 = reader1.readVariants(variants1Cache, position);
                        condition1 = variants1 != null;
                        variants2Cache.add(variants2);
                        variants2.clear();
                        variants2 = reader2.readVariants(variants2Cache, position);
                        condition2 = variants2 != null;
                    }
                }
            }

            if (position != null) {
                position.clear();
            }
        }

        reader1.close();
        reader2.close();
        writer.close();

        if (this.task.isInplace()) {
            FileUtils.rename(writerBuilder.getOutputFileName(), this.task.getOutputFileName());
        }
    }

    void startWithoutAlleleCheck() throws IOException {
        GTBReader reader1 = new GTBReader(this.task.getTemplateFile(), this.task.getTemplateFile().isPhased(), false);
        GTBReader reader2 = new GTBReader(this.task.getInputFile());
        if (this.task.getSubjects() != null) {
            reader2.selectSubjects(this.task.getSubjects());
        }
        GTBWriterBuilder writerBuilder = new GTBWriterBuilder(this.task.isInplace() ? this.task.getOutputFileName() + ".~$temp" : this.task.getOutputFileName());
        writerBuilder.setPhased(this.task.isPhased());
        writerBuilder.setSubject(reader2.getAllSubjects());
        writerBuilder.setReference(reader1.getManager().getReference());
        writerBuilder.setCompressor(this.task.getCompressor(), this.task.getCompressionLevel());
        writerBuilder.setReordering(this.task.isReordering());
        writerBuilder.setWindowSize(this.task.getWindowSize());
        writerBuilder.setBlockSizeType(this.task.getBlockSizeType());
        writerBuilder.setThreads(this.task.getThreads());
        GTBWriter writer = writerBuilder.build();

        // 元信息
        GTBManager manager1 = reader1.getManager();
        GTBManager manager2 = reader2.getManager();
        SmartList<Variant> variants1 = null;
        SmartList<Variant> variants1Cache = new SmartList<>();
        SmartList<Variant> variants2 = null;
        SmartList<Variant> variants2Cache = new SmartList<>();

        for (int i = 0; i < 1; i++) {
            variants1Cache.add(new Variant());
            variants2Cache.add(new Variant());
        }

        for (String chromosome : loadInChromosomes[1]) {
            boolean condition1 = false;
            boolean condition2 = false;
            HashSet<Integer> position = recordPosition(reader1, reader2, chromosome);

            if (manager1.contain(chromosome)) {
                reader1.limit(chromosome);
                reader1.seek(chromosome, 0);
                variants1 = reader1.readVariants(variants1Cache, position);
                condition1 = variants1 != null;
            }

            if (manager2.contain(chromosome)) {
                reader2.limit(chromosome);
                reader2.seek(chromosome, 0);
                variants2 = reader2.readVariants(variants2Cache, position);
                condition2 = variants2 != null;
            }

            while (condition1 || condition2) {
                if (condition1 && !condition2) {
                    // v1 有效位点, v2 无效位点
                    break;
                } else if (!condition1) {
                    // v2 有效位点, v1 无效位点
                    do {
                        for (Variant variant2 : variants2) {
                            writeToFile(variant2, writer);
                        }
                        variants2Cache.add(variants2);
                        variants2.clear();
                        variants2 = reader2.readVariants(variants2Cache, position);
                        condition2 = variants2 != null;
                    } while (condition2);
                } else {
                    // 都有有效位点, 此时先进行位置大小的比较
                    int position1 = variants1.get(0).position;
                    int position2 = variants2.get(0).position;
                    int compareStatue = position1 - position2;
                    if (compareStatue < 0) {
                        // 写入所有的 1 位点，并移动 1 的指针
                        do {
                            variants1Cache.add(variants1);
                            variants1.clear();
                            variants1 = reader1.readVariants(variants1Cache, position);
                            condition1 = variants1 != null;
                            position1 = variants1 != null ? variants1.get(0).position : -1;
                        } while (condition1 && position1 < position2);

                    } else if (compareStatue > 0) {
                        // 写入所有的 2 位点，并移动 2 的指针
                        do {
                            for (Variant variant2 : variants2) {
                                writeToFile(variant2, writer);
                            }
                            variants2Cache.add(variants2);
                            variants2.clear();
                            variants2 = reader2.readVariants(variants2Cache, position);
                            condition2 = variants2 != null;
                            position2 = variants2 != null ? variants2.get(0).position : -1;
                        } while (condition2 && position1 > position2);
                    } else {
                        int sizeVariants1 = variants1.size();
                        int sizeVariants2 = variants2.size();

                        // 不检查 allele 时, 逐一配对
                        if (sizeVariants1 == sizeVariants2 && sizeVariants1 == 1) {
                            // 只有一个位点，直接比对
                            Variant variant1 = variants1.get(0);
                            Variant variant2 = variants2.get(0);
                            variant2.resetAlleles(variant1.REF, variant1.ALT);
                            writeToFile(variant2, writer);
                        } else {
                            // 多对多 (先找一致项，不一致的再进行匹配)
                            SmartList<Variant> variants1new = new SmartList<>();

                            out:
                            for (int i = 0; i < variants1.size(); i++) {
                                Variant variant1 = variants1.get(i);
                                for (int j = 0; j < variants2.size(); j++) {
                                    Variant variant2 = variants2.get(j);
                                    if ((Arrays.equals(variant1.REF, variant2.REF) && Arrays.equals(variant1.ALT, variant2.ALT)) ||
                                            (Arrays.equals(variant1.REF, variant2.ALT) && Arrays.equals(variant1.ALT, variant2.REF))) {
                                        // 基本逻辑: 完全一致的碱基序列的位点直接合并，并且只合并一次
                                        variant2.resetAlleles(variant1.REF, variant1.ALT);
                                        writeToFile(variant2, writer);
                                        variants2.remove(variant2);
                                        variants2Cache.add(variant2);
                                        continue out;
                                    }
                                }

                                // 没有遇到匹配的，此时记录下该位点
                                variants1new.add(variant1);
                            }

                            while (true) {
                                if (variants1new.size() == 0 && variants2.size() == 0) {
                                    // 所有来自文件 1 和 文件 2 的位点都被匹配完成

                                    break;
                                } else if (variants1new.size() == 0) {
                                    // 所有来自文件 1 的位点都被匹配完成，此时文件 2 直接写入
                                    for (Variant variant2 : variants2) {
                                        writeToFile(variant2, writer);
                                    }

                                    break;
                                } else if (variants2.size() == 0) {
                                    // 所有来自文件 2 的位点都被匹配完成，此时文件 1 直接写入

                                    break;
                                } else {
                                    // 文件 1 和文件 2 都有位点，此时按照顺序匹配
                                    SmartList<Variant> variants1NewTemp = new SmartList<>();
                                    for (int i = 0; i < variants1new.size(); i++) {
                                        Variant variant1 = variants1new.get(i);
                                        if (variants2.size() > 0) {
                                            Variant variant2 = variants2.popFirst();
                                            variant2.resetAlleles(variant1.REF, variant1.ALT);
                                            writeToFile(variant2, writer);
                                            variants2Cache.add(variant2);
                                            continue;
                                        }

                                        // 没有遇到匹配的，此时记录下该位点
                                        variants1NewTemp.add(variant1);
                                    }
                                    variants1new = variants1NewTemp;
                                }
                            }
                        }

                        variants1Cache.add(variants1);
                        variants1.clear();
                        variants1 = reader1.readVariants(variants1Cache, position);
                        condition1 = variants1 != null;
                        variants2Cache.add(variants2);
                        variants2.clear();
                        variants2 = reader2.readVariants(variants2Cache, position);
                        condition2 = variants2 != null;
                    }
                }
            }

            if (position != null) {
                position.clear();
            }
        }

        reader1.close();
        reader2.close();
        writer.close();

        if (this.task.isInplace()) {
            FileUtils.rename(writerBuilder.getOutputFileName(), this.task.getOutputFileName());
        }
    }

    Variant alignVariantWithAlleleCheck(Variant variant1, Variant variant2) {
        int AC12 = variant1.getAC();
        int AN1 = variant1.getAN();
        int AC11 = AN1 - AC12;
        int AC22 = variant2.getAC();
        int AN2 = variant2.getAN();
        int AC21 = AN2 - AC22;
        int alleleNum1 = variant1.getAlternativeAlleleNum();
        int alleleNum2 = variant2.getAlternativeAlleleNum();
        int AC = -1;
        int AN = AN2;

        if (AC22 == 0 && AC12 == 0) {
            // alt 都是 . 并且他们没有频率值
            if (Arrays.equals(variant1.REF, variant2.REF)) {
                variant2.ALT = variant1.ALT;
                AC = 0;
            } else if (Arrays.equals(variant1.REF, getInverseAllele(variant2.REF))) {
                // f1: A .  f2: T .
                variant2.REF = variant1.REF;
                variant2.ALT = variant1.ALT;
                AC = 0;
            } else {
                // f1: A .  f2: C .
                variant1.ALT = variant2.REF;
                variant2.ALT = variant1.REF;
                AC = AN2;
            }
        } else if (AC12 == 0) {
            // 第一个位点的 alt 为 0
            byte[] inverseAlleleVariant2ALT = getInverseAllele(variant2.ALT);
            byte[] inverseAlleleVariant2REF = getInverseAllele(variant2.REF);
            if (Arrays.equals(variant1.REF, variant2.REF)) {
                if (alleleNum2 == 2 && Arrays.equals(variant1.REF, inverseAlleleVariant2ALT) && alleleChecker.isEqual(AC11, AC12, AC22, AC21)) {
                    variant2.REF = variant2.ALT;
                    variant2.ALT = variant1.REF;
                    variant1.ALT = variant2.REF;
                    AC = AC21;
                } else {
                    variant1.ALT = variant2.ALT;
                    AC = AC22;
                }
            } else if (Arrays.equals(variant1.REF, variant2.ALT)) {
                if (Arrays.equals(variant1.REF, inverseAlleleVariant2REF) && alleleChecker.isEqual(AC11, AC12, AC21, AC22)) {
                    variant2.ALT = variant2.REF;
                    variant2.REF = variant1.REF;
                    variant1.ALT = variant2.ALT;
                    AC = AC22;
                } else {
                    variant1.ALT = variant2.REF;
                    AC = AC21;
                }
            } else if (Arrays.equals(variant1.REF, inverseAlleleVariant2REF) && alleleChecker.isEqual(AC11, AC12, AC21, AC22)) {
                variant2.REF = variant1.REF;
                variant2.ALT = inverseAlleleVariant2ALT;
                variant1.ALT = variant2.ALT;
                AC = AC22;
            } else if (alleleNum2 == 2 && Arrays.equals(variant1.REF, inverseAlleleVariant2ALT) && alleleChecker.isEqual(AC11, AC12, AC22, AC21)) {
                variant2.ALT = variant1.REF;
                variant2.REF = inverseAlleleVariant2REF;
                variant1.ALT = variant2.REF;
                AC = AC21;
            } else {
                variant1.ALT = variant2.REF;

                if (alleleNum2 == 2) {
                    AC = AN2;
                }
            }
        } else if (AC22 == 0) {
            // 第二个位点的 alt 为 0
            byte[] inverseAlleleVariant1ALT = getInverseAllele(variant1.ALT);
            byte[] inverseAlleleVariant1REF = getInverseAllele(variant1.REF);
            if (Arrays.equals(variant2.REF, variant1.REF)) {
                if (alleleNum1 == 2 && Arrays.equals(variant2.REF, inverseAlleleVariant1ALT) && alleleChecker.isEqual(AC11, AC12, AC22, AC21)) {
                    variant2.REF = variant1.ALT;
                    variant2.ALT = variant1.REF;
                    AC = AC21;
                } else {
                    variant2.ALT = variant1.ALT;
                    AC = AC22;
                }
            } else if (Arrays.equals(variant2.REF, variant1.ALT)) {
                if (Arrays.equals(variant2.REF, inverseAlleleVariant1REF) && alleleChecker.isEqual(AC11, AC12, AC21, AC22)) {
                    variant2.REF = variant1.REF;
                    variant2.ALT = variant1.ALT;
                    AC = AC22;
                } else {
                    variant2.ALT = variant1.REF;
                    AC = AC21;
                }
            } else if (Arrays.equals(variant2.REF, inverseAlleleVariant1REF) && alleleChecker.isEqual(AC11, AC12, AC21, AC22)) {
                variant2.REF = variant1.REF;
                variant2.ALT = variant1.ALT;
                AC = AC22;
            } else if (alleleNum1 == 2 && Arrays.equals(variant2.REF, inverseAlleleVariant1ALT) && alleleChecker.isEqual(AC11, AC12, AC22, AC21)) {
                variant2.REF = variant1.ALT;
                variant2.ALT = variant1.REF;
                AC = AC21;
            } else {
                variant2.ALT = variant1.REF;
                AC = AC22;
            }
        } else {
            // 都不为 .
            if (alleleNum1 == 2 && alleleNum2 == 2) {
                // 都是 2 等位基因位点
                byte[] inverseAlleleVariant2ALT = getInverseAllele(variant2.ALT);
                byte[] inverseAlleleVariant2REF = getInverseAllele(variant2.REF);
                if (Arrays.equals(variant1.REF, inverseAlleleVariant2REF) && Arrays.equals(variant1.ALT, inverseAlleleVariant2ALT)) {
                    if (alleleChecker.isEqual(AC11, AC12, AC21, AC22)) {
                        variant2.REF = variant1.REF;
                        variant2.ALT = variant1.ALT;
                        AC = AC22;
                    }
                } else if (Arrays.equals(variant1.REF, inverseAlleleVariant2ALT) && Arrays.equals(variant1.ALT, inverseAlleleVariant2REF)) {
                    if (alleleChecker.isEqual(AC11, AC12, AC22, AC21)) {
                        variant2.REF = variant1.ALT;
                        variant2.ALT = variant1.REF;
                        AC = AC21;
                    }
                } else if (Arrays.equals(variant1.REF, variant2.REF)) {
                    AC = AC22;
                }
            } else if (alleleNum1 == 2) {
                byte[] inverseAlleleVariant2REF = getInverseAllele(variant2.REF);
                if (Arrays.equals(variant1.REF, inverseAlleleVariant2REF)) {
                    if (alleleChecker.isEqual(AC11, AC12, AC21, AC22)) {
                        variant2.REF = variant1.REF;
                        variant2.ALT = getInverseAllele(variant2.ALT);
                        AC = AC22;
                    }
                } else if (Arrays.equals(variant1.ALT, inverseAlleleVariant2REF)) {
                    if (alleleChecker.isEqual(AC11, AC12, AC22, AC21)) {
                        variant2.REF = variant1.ALT;
                        variant2.ALT = getInverseAllele(variant2.ALT);
                    }
                } else if (Arrays.equals(variant1.REF, variant2.REF)) {
                    AC = AC22;
                }
            } else if (alleleNum2 == 2) {
                byte[] inverseAlleleVariant1REF = getInverseAllele(variant1.REF);
                if (Arrays.equals(variant2.REF, inverseAlleleVariant1REF)) {
                    if (alleleChecker.isEqual(AC11, AC12, AC21, AC22)) {
                        variant2.REF = variant1.REF;
                        variant2.ALT = getInverseAllele(variant2.ALT);
                        AC = AC22;
                    }
                } else if (Arrays.equals(variant2.ALT, inverseAlleleVariant1REF)) {
                    if (alleleChecker.isEqual(AC11, AC12, AC22, AC21)) {
                        variant2.ALT = variant1.REF;
                        variant2.REF = getInverseAllele(variant2.REF);
                    }
                } else if (Arrays.equals(variant1.REF, variant2.REF)) {
                    AC = AC22;
                }
            } else {
                // 都是多等位基因位点，只检查 ref
                if (Arrays.equals(variant1.REF, getInverseAllele(variant2.REF))) {
                    if (alleleChecker.isEqual(AC11, AC12, AC21, AC22)) {
                        variant2.REF = variant1.REF;
                        variant2.ALT = getInverseAllele(variant2.ALT);
                        AC = AC22;
                    }
                } else if (Arrays.equals(variant1.REF, variant2.REF)) {
                    AC = AC22;
                }
            }
        }

        // 合并位点到 target
        variant2.resetAlleles(variant1.REF, variant1.ALT);

        if (this.variantQC.filter(null, variant2.getAlternativeAlleleNum(), 0, 0, 0, 0)) {
            return null;
        }

        if (this.alleleQC.size() > 0) {
            // 需要进行 allele QC
            if (AC != -1) {
                if (this.alleleQC.filter(AC, AN)) {
                    return null;
                } else {
                    return variant2;
                }
            } else {
                if (this.alleleQC.filter(variant2.getAC(), AN)) {
                    return null;
                } else {
                    return variant2;
                }
            }
        } else {
            return variant2;
        }
    }

    void writeToFile(Variant variant, GTBWriter writer) throws IOException {
        if (this.variantQC.filter(null, variant.getAlternativeAlleleNum(), 0, 0, 0, 0)) {
            return;
        }

        if (this.alleleQC.size() > 0 && this.alleleQC.filter(variant.getAC(), variant.getAN())) {
            return;
        }

        writer.write(variant);
    }

    byte[] getInverseAllele(byte[] allele) {
        byte[] alleleInverse = new byte[allele.length];
        for (int i = 0; i < alleleInverse.length; i++) {
            if (complementaryBase.containsKey(allele[i])) {
                int code = complementaryBase.get(allele[i]);
                alleleInverse[i] = (byte) code;
            } else {
                alleleInverse[i] = allele[i];
            }
        }

        return alleleInverse;
    }
}
