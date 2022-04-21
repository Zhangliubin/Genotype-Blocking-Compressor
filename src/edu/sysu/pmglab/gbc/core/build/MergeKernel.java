package edu.sysu.pmglab.gbc.core.build;

import edu.sysu.pmglab.container.array.Array;
import edu.sysu.pmglab.easytools.ArrayUtils;
import edu.sysu.pmglab.easytools.FileUtils;
import edu.sysu.pmglab.gbc.core.common.allelechecker.AlleleChecker;
import edu.sysu.pmglab.gbc.core.common.qualitycontrol.allele.AlleleQC;
import edu.sysu.pmglab.gbc.core.common.qualitycontrol.variant.VariantQC;
import edu.sysu.pmglab.gbc.core.gtbcomponent.GTBManager;
import edu.sysu.pmglab.gbc.core.gtbcomponent.GTBRootCache;
import edu.sysu.pmglab.gbc.core.gtbcomponent.gtbreader.GTBReader;
import edu.sysu.pmglab.gbc.core.gtbcomponent.gtbreader.Variant;
import edu.sysu.pmglab.gbc.core.gtbcomponent.gtbwriter.GTBWriter;
import edu.sysu.pmglab.gbc.core.gtbcomponent.gtbwriter.GTBWriterBuilder;

import java.io.IOException;
import java.util.*;

/**
 * @author suranyi
 * @description
 */

class MergeKernel {
    final VariantQC variantQC;
    final AlleleQC alleleQC;
    final MergeTask task;
    final AlleleChecker alleleChecker;
    final HashSet<Integer> loadInChromosomes;
    final boolean convert2Biallelic;
    final Array<GTBManager> managers;

    static HashMap<Byte, Byte> complementaryBase = new HashMap<>();

    /**
     * 文件最小堆
     */
    final Queue<GTBManager> fileQueue;

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

    MergeKernel(MergeTask task) throws IOException {
        this.task = task;
        this.alleleChecker = this.task.getAlleleChecker();
        this.convert2Biallelic = this.task.isConvertToBiallelic();
        this.managers = (Array<GTBManager>) this.task.getManagers().clone();

        if (this.alleleChecker != null) {
            // sort managers (large -> small)
            this.managers.sort((o1, o2) -> -Integer.compare(o1.getSubjectNum(), o2.getSubjectNum()));
            fileQueue = new ArrayDeque<>();
        } else {
            // 其余情况, 使用最小堆
            fileQueue = new PriorityQueue<>(Comparator.comparingInt(GTBManager::getSubjectNum));
        }

        // 往队列中添加所有的文件
        fileQueue.addAll(this.managers.toArrayList());

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

    public static void submit(MergeTask task) throws IOException {
        new MergeKernel(task);
    }

    HashSet<Integer> recordChromosome() {
        // 记录坐标，如果坐标太多，可以考虑分染色体读取 (使用 reader.limit 语句)
        HashSet<Integer> loadInChromosomes = new HashSet<>();

        if (this.task.isKeepAll()) {
            // 合并所有坐标
            for (GTBManager manager : this.managers) {
                loadInChromosomes.addAll(ArrayUtils.toArrayList(manager.getChromosomeList()));
            }
        } else {
            // 取交集 (先保留第一个文件的染色体)
            loadInChromosomes.addAll(ArrayUtils.toArrayList(managers.get(0).getChromosomeList()));

            for (GTBManager manager : this.managers) {
                loadInChromosomes.retainAll(ArrayUtils.toArrayList(manager.getChromosomeList()));
            }
        }

        return loadInChromosomes;
    }

    HashSet<Integer> recordPosition(GTBReader reader1, GTBReader reader2, int chromosomeIndex) throws IOException {
        if (this.task.isKeepAll()) {
            return null;
        } else {
            HashSet<Integer> loadInPosition1 = new HashSet<>();

            reader1 = new GTBReader(reader1.getManager(), this.task.isPhased(), false);
            reader1.limit(chromosomeIndex);
            reader1.seek(chromosomeIndex, 0);

            // 先加载第一个文件全部的位点
            for (Variant variant : reader1) {
                loadInPosition1.add(variant.position);
            }

            reader1.close();

            // 再移除第二个文件中没有的位点
            reader2 = new GTBReader(reader2.getManager(), this.task.isPhased(), false);
            reader2.limit(chromosomeIndex);
            reader2.seek(chromosomeIndex, 0);

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
        int index = 0;
        while (fileQueue.size() > 1) {
            GTBReader reader1 = new GTBReader(fileQueue.poll(), this.task.isPhased());
            GTBReader reader2 = new GTBReader(fileQueue.poll(), this.task.isPhased());

            GTBWriterBuilder writerBuilder = new GTBWriterBuilder(this.task.getOutputFileName() + "." + (index++) + ".~$temp");
            writerBuilder.setPhased(this.task.isPhased());
            writerBuilder.setSubject(ArrayUtils.merge(reader1.getAllSubjects(), reader2.getAllSubjects()));
            writerBuilder.setReference(reader1.getManager().getReference());
            // 最后一个文件之前都使用快速压缩模式
            if (fileQueue.size() == 0) {
                writerBuilder.setCompressor(this.task.getCompressor(), this.task.getCompressionLevel());
                writerBuilder.setReordering(this.task.isReordering());
                writerBuilder.setWindowSize(this.task.getWindowSize());
            } else {
                writerBuilder.setBlockSizeType(this.task.getBlockSizeType());
                writerBuilder.setCompressor(0, 3);
                writerBuilder.setReordering(false);
            }
            writerBuilder.setThreads(this.task.getThreads());
            GTBWriter writer = writerBuilder.build();

            // 元信息
            GTBManager manager1 = reader1.getManager();
            GTBManager manager2 = reader2.getManager();
            Array<Variant> variants1 = null;
            Array<Variant> variants1Cache = new Array<>();
            Array<Variant> variants2 = null;
            Array<Variant> variants2Cache = new Array<>();
            Variant mergeVariant = new Variant();
            Variant mergeVariant1 = new Variant();
            Variant mergeVariant2 = new Variant();
            mergeVariant.BEGs = new byte[manager1.getSubjectNum() + manager2.getSubjectNum()];
            mergeVariant1.BEGs = new byte[manager1.getSubjectNum() + manager2.getSubjectNum()];
            mergeVariant2.BEGs = new byte[manager1.getSubjectNum() + manager2.getSubjectNum()];

            this.alleleChecker.setReader(reader1.getManager(), reader2.getManager());

            for (int i = 0; i < 1; i++) {
                variants1Cache.add(new Variant());
                variants2Cache.add(new Variant());
            }

            for (int chromosomeIndex : loadInChromosomes) {
                boolean condition1 = false;
                boolean condition2 = false;
                HashSet<Integer> position = recordPosition(reader1, reader2, chromosomeIndex);

                this.alleleChecker.setPosition(position);

                if (manager1.contain(chromosomeIndex)) {
                    reader1.limit(chromosomeIndex);
                    reader1.seek(chromosomeIndex, 0);
                    variants1 = reader1.readVariants(variants1Cache, position);
                    condition1 = variants1 != null;
                }

                if (manager2.contain(chromosomeIndex)) {
                    reader2.limit(chromosomeIndex);
                    reader2.seek(chromosomeIndex, 0);
                    variants2 = reader2.readVariants(variants2Cache, position);
                    condition2 = variants2 != null;
                }

                while (condition1 || condition2) {
                    if (condition1 && !condition2) {
                        // v1 有效位点, v2 无效位点
                        do {
                            for (Variant variant1 : variants1) {
                                mergeVariant1.chromosome = variant1.chromosome;
                                mergeVariant1.position = variant1.position;
                                mergeVariant1.ploidy = variant1.ploidy;
                                mergeVariant1.phased = variant1.phased;
                                mergeVariant1.ALT = variant1.ALT;
                                mergeVariant1.REF = variant1.REF;
                                System.arraycopy(variant1.BEGs, 0, mergeVariant1.BEGs, 0, variant1.BEGs.length);
                                writeToFile(mergeVariant1, writer);
                            }
                            variants1Cache.addAll(variants1);
                            variants1.clear();
                            variants1 = reader1.readVariants(variants1Cache, position);
                            condition1 = variants1 != null;
                        } while (condition1);
                    } else if (!condition1) {
                        // v2 有效位点, v1 无效位点
                        do {
                            for (Variant variant2 : variants2) {
                                mergeVariant2.chromosome = variant2.chromosome;
                                mergeVariant2.position = variant2.position;
                                mergeVariant2.ploidy = variant2.ploidy;
                                mergeVariant2.phased = variant2.phased;
                                mergeVariant2.ALT = variant2.ALT;
                                mergeVariant2.REF = variant2.REF;
                                System.arraycopy(variant2.BEGs, 0, mergeVariant2.BEGs, manager1.getSubjectNum(), variant2.BEGs.length);
                                writeToFile(mergeVariant2, writer);
                            }
                            variants2Cache.addAll(variants2);
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
                                for (Variant variant1 : variants1) {
                                    mergeVariant1.chromosome = variant1.chromosome;
                                    mergeVariant1.position = variant1.position;
                                    mergeVariant1.ploidy = variant1.ploidy;
                                    mergeVariant1.phased = variant1.phased;
                                    mergeVariant1.ALT = variant1.ALT;
                                    mergeVariant1.REF = variant1.REF;
                                    System.arraycopy(variant1.BEGs, 0, mergeVariant1.BEGs, 0, variant1.BEGs.length);
                                    writeToFile(mergeVariant1, writer);
                                }
                                variants1Cache.addAll(variants1);
                                variants1.clear();
                                variants1 = reader1.readVariants(variants1Cache, position);
                                condition1 = variants1 != null;
                                position1 = condition1 ? variants1.get(0).position : -1;
                            } while (condition1 && position1 < position2);
                        } else if (compareStatue > 0) {
                            // 写入所有的 2 位点，并移动 2 的指针
                            do {
                                for (Variant variant2 : variants2) {
                                    mergeVariant2.chromosome = variant2.chromosome;
                                    mergeVariant2.position = variant2.position;
                                    mergeVariant2.ploidy = variant2.ploidy;
                                    mergeVariant2.phased = variant2.phased;
                                    mergeVariant2.ALT = variant2.ALT;
                                    mergeVariant2.REF = variant2.REF;
                                    System.arraycopy(variant2.BEGs, 0, mergeVariant2.BEGs, manager1.getSubjectNum(), variant2.BEGs.length);
                                    writeToFile(mergeVariant2, writer);
                                }
                                variants2Cache.addAll(variants2);
                                variants2.clear();
                                variants2 = reader2.readVariants(variants2Cache, position);
                                condition2 = variants2 != null;
                                position2 = condition2 ? variants2.get(0).position : -1;
                            } while (condition2 && position1 > position2);
                        } else {
                            // 位点位置值一样
                            int sizeVariants1 = variants1.size();
                            int sizeVariants2 = variants2.size();
                            // 检查 allele 时
                            if (sizeVariants1 == sizeVariants2 && sizeVariants1 == 1) {
                                // 只有一个位点，直接合并
                                Variant variant1 = variants1.get(0);
                                Variant variant2 = variants2.get(0);
                                mergeVariantWithAlleleCheckAndWriteToFile(variant1, variant2, mergeVariant, writer);
                            } else {
                                // 多对多 (先找一致项，不一致的再进行匹配)
                                Array<Variant> variants1new = new Array<>();

                                out:
                                for (int i = 0; i < variants1.size(); i++) {
                                    Variant variant1 = variants1.get(i);
                                    for (int j = 0; j < variants2.size(); j++) {
                                        Variant variant2 = variants2.get(j);
                                        if ((Arrays.equals(variant1.REF, variant2.REF) && Arrays.equals(variant1.ALT, variant2.ALT)) ||
                                                (Arrays.equals(variant1.REF, variant2.ALT) && Arrays.equals(variant1.ALT, variant2.REF))) {
                                            // 基本逻辑: 完全一致的碱基序列的位点直接合并，并且只合并一次
                                            mergeVariantWithAlleleCheckAndWriteToFile(variant1, variant2, mergeVariant, writer);
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
                                            mergeVariant2.chromosome = variant2.chromosome;
                                            mergeVariant2.position = variant2.position;
                                            mergeVariant2.ploidy = variant2.ploidy;
                                            mergeVariant2.phased = variant2.phased;
                                            mergeVariant2.ALT = variant2.ALT;
                                            mergeVariant2.REF = variant2.REF;
                                            System.arraycopy(variant2.BEGs, 0, mergeVariant2.BEGs, manager1.getSubjectNum(), variant2.BEGs.length);
                                            writeToFile(mergeVariant2, writer);
                                        }

                                        break;
                                    } else if (variants2.size() == 0) {
                                        // 所有来自文件 2 的位点都被匹配完成，此时文件 1 直接写入
                                        for (Variant variant1 : variants1new) {
                                            mergeVariant1.chromosome = variant1.chromosome;
                                            mergeVariant1.position = variant1.position;
                                            mergeVariant1.ploidy = variant1.ploidy;
                                            mergeVariant1.phased = variant1.phased;
                                            mergeVariant1.ALT = variant1.ALT;
                                            mergeVariant1.REF = variant1.REF;
                                            System.arraycopy(variant1.BEGs, 0, mergeVariant1.BEGs, 0, variant1.BEGs.length);
                                            writeToFile(mergeVariant1, writer);
                                        }

                                        break;
                                    } else {
                                        // 文件 1 和文件 2 都有位点，此时按照顺序匹配
                                        Array<Variant> variants1NewTemp = new Array<>();
                                        for (int i = 0; i < variants1new.size(); i++) {
                                            Variant variant1 = variants1new.get(i);
                                            if (variants2.size() > 0) {
                                                Variant variant2 = variants2.popFirst();
                                                mergeVariantWithAlleleCheckAndWriteToFile(variant1, variant2, mergeVariant, writer);
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

                            variants1Cache.addAll(variants1);
                            variants1.clear();
                            variants1 = reader1.readVariants(variants1Cache, position);
                            condition1 = variants1 != null;
                            variants2Cache.addAll(variants2);
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

            // 添加生成的文件
            if (fileQueue instanceof ArrayDeque) {
                ((ArrayDeque<GTBManager>) fileQueue).addFirst(GTBRootCache.get(writerBuilder.getOutputFileName()));
            } else {
                fileQueue.add(GTBRootCache.get(writerBuilder.getOutputFileName()));
            }

            if (reader1.getManager().getFileName().endsWith(".~$temp")) {
                FileUtils.delete(reader1.getManager().getFileName());
            }

            if (reader2.getManager().getFileName().endsWith(".~$temp")) {
                FileUtils.delete(reader2.getManager().getFileName());
            }
        }

        // 最后把文件名改为最终文件名
        assert this.fileQueue.size() > 0;
        FileUtils.rename(this.fileQueue.poll().getFileName(), this.task.getOutputFileName());
    }

    void startWithoutAlleleCheck() throws IOException {
        int index = 0;
        while (fileQueue.size() > 1) {
            GTBReader reader1 = new GTBReader(fileQueue.poll(), this.task.isPhased());
            GTBReader reader2 = new GTBReader(fileQueue.poll(), this.task.isPhased());

            GTBWriterBuilder writerBuilder = new GTBWriterBuilder(this.task.getOutputFileName() + "." + (index++) + ".~$temp");
            writerBuilder.setPhased(this.task.isPhased());
            writerBuilder.setSubject(ArrayUtils.merge(reader1.getAllSubjects(), reader2.getAllSubjects()));
            writerBuilder.setReference(reader1.getManager().getReference());
            // 最后一个文件之前都使用快速压缩模式
            if (fileQueue.size() == 0) {
                writerBuilder.setCompressor(this.task.getCompressor(), this.task.getCompressionLevel());
                writerBuilder.setReordering(this.task.isReordering());
                writerBuilder.setWindowSize(this.task.getWindowSize());
            } else {
                writerBuilder.setBlockSizeType(this.task.getBlockSizeType());
                writerBuilder.setCompressor(0, 3);
                writerBuilder.setReordering(false);
            }
            writerBuilder.setThreads(this.task.getThreads());
            GTBWriter writer = writerBuilder.build();

            // 元信息
            GTBManager manager1 = reader1.getManager();
            GTBManager manager2 = reader2.getManager();
            Array<Variant> variants1 = null;
            Array<Variant> variants1Cache = new Array<>();
            Array<Variant> variants2 = null;
            Array<Variant> variants2Cache = new Array<>();
            Variant mergeVariant = new Variant();
            Variant mergeVariant1 = new Variant();
            Variant mergeVariant2 = new Variant();
            mergeVariant.BEGs = new byte[manager1.getSubjectNum() + manager2.getSubjectNum()];
            mergeVariant1.BEGs = new byte[manager1.getSubjectNum() + manager2.getSubjectNum()];
            mergeVariant2.BEGs = new byte[manager1.getSubjectNum() + manager2.getSubjectNum()];

            for (int i = 0; i < 1; i++) {
                variants1Cache.add(new Variant());
                variants2Cache.add(new Variant());
            }

            for (int chromosomeIndex : loadInChromosomes) {
                boolean condition1 = false;
                boolean condition2 = false;
                HashSet<Integer> position = recordPosition(reader1, reader2, chromosomeIndex);

                if (manager1.contain(chromosomeIndex)) {
                    reader1.limit(chromosomeIndex);
                    reader1.seek(chromosomeIndex, 0);
                    variants1 = reader1.readVariants(variants1Cache, position);
                    condition1 = variants1 != null;
                }

                if (manager2.contain(chromosomeIndex)) {
                    reader2.limit(chromosomeIndex);
                    reader2.seek(chromosomeIndex, 0);
                    variants2 = reader2.readVariants(variants2Cache, position);
                    condition2 = variants2 != null;
                }

                while (condition1 || condition2) {
                    if (condition1 && !condition2) {
                        // v1 有效位点, v2 无效位点
                        do {
                            for (Variant variant1 : variants1) {
                                mergeVariant1.chromosome = variant1.chromosome;
                                mergeVariant1.position = variant1.position;
                                mergeVariant1.ploidy = variant1.ploidy;
                                mergeVariant1.phased = variant1.phased;
                                mergeVariant1.ALT = variant1.ALT;
                                mergeVariant1.REF = variant1.REF;
                                System.arraycopy(variant1.BEGs, 0, mergeVariant1.BEGs, 0, variant1.BEGs.length);
                                writeToFile(mergeVariant1, writer);
                            }
                            variants1Cache.addAll(variants1);
                            variants1.clear();
                            variants1 = reader1.readVariants(variants1Cache, position);
                            condition1 = variants1 != null;
                        } while (condition1);
                    } else if (!condition1) {
                        // v2 有效位点, v1 无效位点
                        do {
                            for (Variant variant2 : variants2) {
                                mergeVariant2.chromosome = variant2.chromosome;
                                mergeVariant2.position = variant2.position;
                                mergeVariant2.ploidy = variant2.ploidy;
                                mergeVariant2.phased = variant2.phased;
                                mergeVariant2.ALT = variant2.ALT;
                                mergeVariant2.REF = variant2.REF;
                                System.arraycopy(variant2.BEGs, 0, mergeVariant2.BEGs, manager1.getSubjectNum(), variant2.BEGs.length);
                                writeToFile(mergeVariant2, writer);
                            }
                            variants2Cache.addAll(variants2);
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
                                for (Variant variant1 : variants1) {
                                    mergeVariant1.chromosome = variant1.chromosome;
                                    mergeVariant1.position = variant1.position;
                                    mergeVariant1.ploidy = variant1.ploidy;
                                    mergeVariant1.phased = variant1.phased;
                                    mergeVariant1.ALT = variant1.ALT;
                                    mergeVariant1.REF = variant1.REF;
                                    System.arraycopy(variant1.BEGs, 0, mergeVariant1.BEGs, 0, variant1.BEGs.length);
                                    writeToFile(mergeVariant1, writer);
                                }
                                variants1Cache.addAll(variants1);
                                variants1.clear();
                                variants1 = reader1.readVariants(variants1Cache, position);
                                condition1 = variants1 != null;
                                position1 = variants1 != null ? variants1.get(0).position : -1;
                            } while (condition1 && position1 < position2);

                        } else if (compareStatue > 0) {
                            // 写入所有的 2 位点，并移动 2 的指针
                            do {
                                for (Variant variant2 : variants2) {
                                    mergeVariant2.chromosome = variant2.chromosome;
                                    mergeVariant2.position = variant2.position;
                                    mergeVariant2.ploidy = variant2.ploidy;
                                    mergeVariant2.phased = variant2.phased;
                                    mergeVariant2.ALT = variant2.ALT;
                                    mergeVariant2.REF = variant2.REF;
                                    System.arraycopy(variant2.BEGs, 0, mergeVariant2.BEGs, manager1.getSubjectNum(), variant2.BEGs.length);
                                    writeToFile(mergeVariant2, writer);
                                }
                                variants2Cache.addAll(variants2);
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
                                // 只有一个位点，直接合并
                                writeToFile(variants1.get(0).merge(variants2.get(0), mergeVariant), writer);
                            } else {
                                // 多对多 (先找一致项，不一致的再进行匹配)
                                Array<Variant> variants1new = new Array<>();

                                out:
                                for (int i = 0; i < variants1.size(); i++) {
                                    Variant variant1 = variants1.get(i);
                                    for (int j = 0; j < variants2.size(); j++) {
                                        Variant variant2 = variants2.get(j);
                                        if ((Arrays.equals(variant1.REF, variant2.REF) && Arrays.equals(variant1.ALT, variant2.ALT)) ||
                                                (Arrays.equals(variant1.REF, variant2.ALT) && Arrays.equals(variant1.ALT, variant2.REF))) {
                                            // 基本逻辑: 完全一致的碱基序列的位点直接合并，并且只合并一次
                                            writeToFile(variant1.merge(variant2, mergeVariant), writer);
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
                                            mergeVariant2.chromosome = variant2.chromosome;
                                            mergeVariant2.position = variant2.position;
                                            mergeVariant2.ploidy = variant2.ploidy;
                                            mergeVariant2.phased = variant2.phased;
                                            mergeVariant2.ALT = variant2.ALT;
                                            mergeVariant2.REF = variant2.REF;
                                            System.arraycopy(variant2.BEGs, 0, mergeVariant2.BEGs, manager1.getSubjectNum(), variant2.BEGs.length);
                                            writeToFile(mergeVariant2, writer);
                                        }

                                        break;
                                    } else if (variants2.size() == 0) {
                                        // 所有来自文件 2 的位点都被匹配完成，此时文件 1 直接写入
                                        for (Variant variant1 : variants1new) {
                                            mergeVariant1.chromosome = variant1.chromosome;
                                            mergeVariant1.position = variant1.position;
                                            mergeVariant1.ploidy = variant1.ploidy;
                                            mergeVariant1.phased = variant1.phased;
                                            mergeVariant1.ALT = variant1.ALT;
                                            mergeVariant1.REF = variant1.REF;
                                            System.arraycopy(variant1.BEGs, 0, mergeVariant1.BEGs, 0, variant1.BEGs.length);
                                            writeToFile(mergeVariant1, writer);
                                        }

                                        break;
                                    } else {
                                        // 文件 1 和文件 2 都有位点，此时按照顺序匹配
                                        Array<Variant> variants1NewTemp = new Array<>();
                                        for (int i = 0; i < variants1new.size(); i++) {
                                            Variant variant1 = variants1new.get(i);
                                            if (variants2.size() > 0) {
                                                Variant variant2 = variants2.popFirst();
                                                writeToFile(variant1.merge(variant2, mergeVariant), writer);
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

                            variants1Cache.addAll(variants1);
                            variants1.clear();
                            variants1 = reader1.readVariants(variants1Cache, position);
                            condition1 = variants1 != null;
                            variants2Cache.addAll(variants2);
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

            // 添加生成的文件
            if (fileQueue instanceof ArrayDeque) {
                ((ArrayDeque<GTBManager>) fileQueue).addFirst(GTBRootCache.get(writerBuilder.getOutputFileName()));
            } else {
                fileQueue.add(GTBRootCache.get(writerBuilder.getOutputFileName()));
            }

            if (reader1.getManager().getFileName().endsWith(".~$temp")) {
                FileUtils.delete(reader1.getManager().getFileName());
            }

            if (reader2.getManager().getFileName().endsWith(".~$temp")) {
                FileUtils.delete(reader2.getManager().getFileName());
            }
        }

        // 最后把文件名改为最终文件名
        assert this.fileQueue.size() > 0;
        FileUtils.rename(this.fileQueue.poll().getFileName(), this.task.getOutputFileName());
    }

    void mergeVariantWithAlleleCheckAndWriteToFile(Variant variant1, Variant variant2, Variant target, GTBWriter writer) throws IOException {
        int AC12 = variant1.getAC();
        int AN1 = variant1.getAN();
        int AC11 = AN1 - AC12;
        int AC22 = variant2.getAC();
        int AN2 = variant2.getAN();
        int AC21 = AN2 - AC22;
        int alleleNum1 = variant1.getAlternativeAlleleNum();
        int alleleNum2 = variant2.getAlternativeAlleleNum();
        int AC = -1;
        int AN = AN1 + AN2;

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
                if (alleleNum2 == 2 && Arrays.equals(variant1.REF, inverseAlleleVariant2ALT) && alleleChecker.isEqual(variant1, variant2, AC11, AC12, AC22, AC21, true)) {
                    variant2.REF = variant2.ALT;
                    variant2.ALT = variant1.REF;
                    variant1.ALT = variant2.REF;
                    AC = AC21;
                } else {
                    variant1.ALT = variant2.ALT;
                    AC = AC22;
                }
            } else if (Arrays.equals(variant1.REF, variant2.ALT)) {
                if (Arrays.equals(variant1.REF, inverseAlleleVariant2REF) && alleleChecker.isEqual(variant1, variant2, AC11, AC12, AC21, AC22, false)) {
                    variant2.ALT = variant2.REF;
                    variant2.REF = variant1.REF;
                    variant1.ALT = variant2.ALT;
                    AC = AC22;
                } else {
                    variant1.ALT = variant2.REF;
                    AC = AC21;
                }
            } else if (Arrays.equals(variant1.REF, inverseAlleleVariant2REF) && alleleChecker.isEqual(variant1, variant2, AC11, AC12, AC21, AC22, false)) {
                variant2.REF = variant1.REF;
                variant2.ALT = inverseAlleleVariant2ALT;
                variant1.ALT = variant2.ALT;
                AC = AC22;
            } else if (alleleNum2 == 2 && Arrays.equals(variant1.REF, inverseAlleleVariant2ALT) && alleleChecker.isEqual(variant1, variant2, AC11, AC12, AC22, AC21, true)) {
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
                if (alleleNum1 == 2 && Arrays.equals(variant2.REF, inverseAlleleVariant1ALT) && alleleChecker.isEqual(variant1, variant2, AC11, AC12, AC22, AC21, true)) {
                    variant2.REF = variant1.ALT;
                    variant2.ALT = variant1.REF;
                    AC = AC12 + AC21;
                } else {
                    variant2.ALT = variant1.ALT;
                    AC = AC12;
                }
            } else if (Arrays.equals(variant2.REF, variant1.ALT)) {
                if (Arrays.equals(variant2.REF, inverseAlleleVariant1REF) && alleleChecker.isEqual(variant1, variant2, AC11, AC12, AC21, AC22, false)) {
                    variant2.REF = variant1.REF;
                    variant2.ALT = variant1.ALT;
                    AC = AC12;
                } else {
                    variant2.ALT = variant1.REF;
                    AC = AC12 + AC21;
                }
            } else if (Arrays.equals(variant2.REF, inverseAlleleVariant1REF) && alleleChecker.isEqual(variant1, variant2, AC11, AC12, AC21, AC22, false)) {
                variant2.REF = variant1.REF;
                variant2.ALT = variant1.ALT;
                AC = AC12;
            } else if (alleleNum1 == 2 && Arrays.equals(variant2.REF, inverseAlleleVariant1ALT) && alleleChecker.isEqual(variant1, variant2, AC11, AC12, AC22, AC21, true)) {
                variant2.REF = variant1.ALT;
                variant2.ALT = variant1.REF;
                AC = AC12 + AC21;
            } else {
                variant2.ALT = variant1.REF;

                if (alleleNum1 == 2) {
                    AC = AC12 + AC21;
                }
            }
        } else {
            // 都不为 .
            if (alleleNum1 == 2 && alleleNum2 == 2) {
                // 都是 2 等位基因位点
                byte[] inverseAlleleVariant2ALT = getInverseAllele(variant2.ALT);
                byte[] inverseAlleleVariant2REF = getInverseAllele(variant2.REF);
                if (Arrays.equals(variant1.REF, inverseAlleleVariant2REF) && Arrays.equals(variant1.ALT, inverseAlleleVariant2ALT)) {
                    if (alleleChecker.isEqual(variant1, variant2, AC11, AC12, AC21, AC22, false)) {
                        variant2.REF = variant1.REF;
                        variant2.ALT = variant1.ALT;
                        AC = AC12 + AC22;
                    }
                } else if (Arrays.equals(variant1.REF, inverseAlleleVariant2ALT) && Arrays.equals(variant1.ALT, inverseAlleleVariant2REF)) {
                    if (alleleChecker.isEqual(variant1, variant2, AC11, AC12, AC22, AC21, true)) {
                        variant2.REF = variant1.ALT;
                        variant2.ALT = variant1.REF;
                        AC = AC12 + AC21;
                    }
                } else if (Arrays.equals(variant1.REF, variant2.REF)) {
                    AC = AC12 + AC22;
                }
            } else if (alleleNum1 == 2) {
                byte[] inverseAlleleVariant2REF = getInverseAllele(variant2.REF);
                if (Arrays.equals(variant1.REF, inverseAlleleVariant2REF)) {
                    if (alleleChecker.isEqual(variant1, variant2, AC11, AC12, AC21, AC22, false)) {
                        variant2.REF = variant1.REF;
                        variant2.ALT = getInverseAllele(variant2.ALT);
                        AC = AC12 + AC22;
                    }
                } else if (Arrays.equals(variant1.ALT, inverseAlleleVariant2REF)) {
                    if (alleleChecker.isEqual(variant1, variant2, AC11, AC12, AC22, AC21, true)) {
                        variant2.REF = variant1.ALT;
                        variant2.ALT = getInverseAllele(variant2.ALT);
                    }
                } else if (Arrays.equals(variant1.REF, variant2.REF)) {
                    AC = AC12 + AC22;
                }
            } else if (alleleNum2 == 2) {
                byte[] inverseAlleleVariant1REF = getInverseAllele(variant1.REF);
                if (Arrays.equals(variant2.REF, inverseAlleleVariant1REF)) {
                    if (alleleChecker.isEqual(variant1, variant2, AC11, AC12, AC21, AC22, false)) {
                        variant2.REF = variant1.REF;
                        variant2.ALT = getInverseAllele(variant2.ALT);
                        AC = AC12 + AC22;
                    }
                } else if (Arrays.equals(variant2.ALT, inverseAlleleVariant1REF)) {
                    if (alleleChecker.isEqual(variant1, variant2, AC11, AC12, AC22, AC21, true)) {
                        variant2.ALT = variant1.REF;
                        variant2.REF = getInverseAllele(variant2.REF);
                    }
                } else if (Arrays.equals(variant1.REF, variant2.REF)) {
                    AC = AC12 + AC22;
                }
            } else {
                // 都是多等位基因位点，只检查 ref
                if (Arrays.equals(variant1.REF, getInverseAllele(variant2.REF))) {
                    if (alleleChecker.isEqual(variant1, variant2, AC11, AC12, AC21, AC22, false)) {
                        variant2.REF = variant1.REF;
                        variant2.ALT = getInverseAllele(variant2.ALT);
                        AC = AC12 + AC22;
                    }
                } else if (Arrays.equals(variant1.REF, variant2.REF)) {
                    AC = AC12 + AC22;
                }
            }
        }

        // 合并位点到 target
        variant1.merge(variant2, target);

        if (this.convert2Biallelic && target.getAlternativeAlleleNum() > 2) {
            for (Variant subVariant : target.split()) {
                if (this.variantQC.filter(null, 2, 0, 0, 0, 0)) {
                    return;
                }

                if (this.alleleQC.size() > 0 && this.alleleQC.filter(subVariant.getAC(), AN)) {
                    return;
                }

                writer.write(subVariant);
            }
        } else {
            if (this.variantQC.filter(null, target.getAlternativeAlleleNum(), 0, 0, 0, 0)) {
                return;
            }

            if (this.alleleQC.size() > 0) {
                // 需要进行 allele QC
                if (AC != -1) {
                    if (!this.alleleQC.filter(AC, AN)) {
                        writer.write(target);
                    }
                } else {
                    if (!this.alleleQC.filter(target.getAC(), AN)) {
                        writer.write(target);
                    }
                }
            } else {
                writer.write(target);
            }
        }
    }

    void writeToFile(Variant variant, GTBWriter writer) throws IOException {
        if (this.convert2Biallelic && variant.getAlternativeAlleleNum() > 2) {
            int AN = variant.getAN();
            for (Variant subVariant : variant.split()) {
                if (this.variantQC.filter(null, 2, 0, 0, 0, 0)) {
                    return;
                }

                if (this.alleleQC.size() > 0 && this.alleleQC.filter(subVariant.getAC(), AN)) {
                    return;
                }

                writer.write(subVariant);
            }
        } else {
            if (this.variantQC.filter(null, variant.getAlternativeAlleleNum(), 0, 0, 0, 0)) {
                return;
            }

            if (this.alleleQC.size() > 0 && this.alleleQC.filter(variant.getAC(), variant.getAN())) {
                return;
            }

            writer.write(variant);
        }
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
