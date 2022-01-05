package edu.sysu.pmglab.suranyi.gbc.core.gtbcomponent.gtbreader;

import edu.sysu.pmglab.suranyi.check.Assert;
import edu.sysu.pmglab.suranyi.check.exception.RuntimeExceptionOptions;
import edu.sysu.pmglab.suranyi.container.VolumeByteStream;
import edu.sysu.pmglab.suranyi.easytools.ArrayUtils;
import edu.sysu.pmglab.suranyi.easytools.ByteCode;
import edu.sysu.pmglab.suranyi.gbc.coder.BEGTransfer;
import edu.sysu.pmglab.suranyi.gbc.coder.decoder.MBEGDecoder;
import edu.sysu.pmglab.suranyi.gbc.constant.Chromosome;
import edu.sysu.pmglab.suranyi.gbc.constant.ChromosomeInfo;
import edu.sysu.pmglab.suranyi.gbc.core.exception.GBCExceptionOptions;
import edu.sysu.pmglab.suranyi.gbc.core.exception.GTBComponentException;
import edu.sysu.pmglab.suranyi.gbc.core.gtbcomponent.GTBManager;
import edu.sysu.pmglab.suranyi.gbc.core.gtbcomponent.GTBNode;
import edu.sysu.pmglab.suranyi.gbc.core.gtbcomponent.GTBNodes;
import edu.sysu.pmglab.suranyi.gbc.core.gtbcomponent.GTBRootCache;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collection;

/**
 * @Data        :2021/08/14
 * @Author      :suranyi
 * @Contact     :suranyi.sysu@gamil.com
 * @Description :GTB 读取器
 */

public class GTBReader implements Closeable, AutoCloseable {
    private final GTBManager manager;
    private final MBEGDecoder groupDecoder;
    private final boolean phased;
    private IndexPair[] pairs;
    private int[] subjectIndexes;
    private final boolean phasedTransfer;

    /**
     * 当前解压信息
     */
    Pointer pointer;
    final DecompressionCache cache;
    final int eachLineSize;

    /**
     * 缓冲数据
     */
    private final VolumeByteStream headerCache = new VolumeByteStream(0);

    /**
     * 加载重叠群文件
     * @param contig 重叠群文件
     */
    public void loadContig(String contig) throws IOException {
        ChromosomeInfo.load(contig);
    }

    public GTBReader(GTBManager manager) throws IOException {
        this(new Pointer(manager));
    }

    public GTBReader(String fileName) throws IOException {
        this(new Pointer(GTBRootCache.get(fileName)));
    }

    public GTBReader(Pointer pointer) throws IOException {
        this(pointer, pointer.manager.isPhased());
    }

    public GTBReader(GTBManager manager, boolean phased) throws IOException {
        this(new Pointer(manager), phased);
    }

    public GTBReader(String fileName, boolean phased) throws IOException {
        this(new Pointer(GTBRootCache.get(fileName)), phased);
    }

    public GTBReader(Pointer pointer, boolean phased) throws IOException {
        // 获取文件管理器
        this.manager = pointer.manager;

        // 仅有 randomAccessEnAble 的方法可以使用 GTBReader
        if (!this.manager.isOrderedGTB()) {
            throw new GTBComponentException("unsorted GTB does not support GTBReader, please sort it first (use `rebuild GTBFile`)");
        }

        // 设定解压的ID对
        initPairs();

        // 初始化指针
        this.pointer = pointer;
        this.cache = new DecompressionCache(this.manager);

        int eachCodeGenotypeNum = this.manager.isPhased() ? 3 : 4;
        int resBlockCodeGenotypeNum = this.manager.getSubjectNum() % eachCodeGenotypeNum;
        this.eachLineSize = (this.manager.getSubjectNum() / eachCodeGenotypeNum) + (resBlockCodeGenotypeNum == 0 ? 0 : 1);
        this.phased = phased;
        this.phasedTransfer = this.manager.isPhased() && !this.phased;

        // 锁定解码器
        this.groupDecoder = this.manager.getMBEGDecoder();
    }

    public boolean isPhased() {
        return this.phased;
    }

    /**
     * 限定访问的染色体
     * @param chromosomes 染色体编号
     */
    public void limit(String... chromosomes) {
        int[] chromosomeIndexes = new int[chromosomes.length];
        for (int i = 0; i < chromosomes.length; i++) {
            Assert.that(manager.contain(chromosomes[i]), GBCExceptionOptions.GTBComponentException, "chromosome=" + chromosomes[i] + " not in current GTB file");

            chromosomeIndexes[i] = ChromosomeInfo.getIndex(chromosomes[i]);
        }

        this.pointer = new LimitPointer(this.manager, chromosomeIndexes, -1, -1);
    }

    /**
     * 限定访问的染色体及范围
     * @param chromosome 染色体编号
     * @param startNodeIndex 限定访问的节点起始范围
     */
    public void limit(String chromosome, int startNodeIndex) {
        limit(chromosome, startNodeIndex, -1);
    }

    /**
     * 限定访问的染色体及范围
     * @param chromosome 染色体编号
     * @param startNodeIndex 限定访问的节点起始范围
     * @param endNodeIndex 限定访问的节点终止范围
     */
    public void limit(String chromosome, int startNodeIndex, int endNodeIndex) {
        Assert.that(manager.contain(chromosome), GBCExceptionOptions.GTBComponentException, "chromosome=" + chromosome + " not in current GTB file");
        this.pointer = new LimitPointer(this.manager, new int[]{ChromosomeInfo.getIndex(chromosome)}, startNodeIndex, endNodeIndex);
    }

    /**
     * 限定访问的染色体
     * @param chromosomeIndexes 染色体编号
     */
    public void limit(int... chromosomeIndexes) {
        Assert.that(manager.containAll(chromosomeIndexes));
        this.pointer = new LimitPointer(this.manager, chromosomeIndexes, -1, -1);
    }

    /**
     * 限定访问的染色体及范围
     * @param chromosomeIndex 染色体编号
     * @param startNodeIndex 限定访问的节点起始范围
     */
    public void limit(int chromosomeIndex, int startNodeIndex) {
        limit(chromosomeIndex, startNodeIndex, -1);
    }

    /**
     * 限定访问的染色体及范围
     * @param chromosomeIndex 染色体编号
     * @param startNodeIndex 限定访问的节点起始范围
     * @param endNodeIndex 限定访问的节点终止范围
     */
    public void limit(int chromosomeIndex, int startNodeIndex, int endNodeIndex) {
        Chromosome info = ChromosomeInfo.get(chromosomeIndex);
        Assert.that(manager.contain(chromosomeIndex), GBCExceptionOptions.GTBComponentException, "chromosome=" + ChromosomeInfo.getString(chromosomeIndex) + " (index=" + chromosomeIndex + ") not in current GTB file");
        this.pointer = new LimitPointer(this.manager, new int[]{chromosomeIndex}, startNodeIndex, endNodeIndex);
    }

    private void initHeader() {
        // 重设缓冲信息
        headerCache.expansionTo(2 << 20);

        // 写入块头
        headerCache.write(("##fileformat=VCFv4.2" +
                "\n##FILTER=<ID=PASS,Description=\"All filters passed\">" +
                "\n##source=" + this.manager.getFileName() +
                "\n##Version=<gbc_version=1.5,java_version=" + System.getProperty("java.version") + ",zstd_jni=1.4.9-5>"));

        // 参考序列非空时，写入参考序列
        if (!this.manager.isReferenceEmpty()) {
            headerCache.write("\n##reference=");
            headerCache.write(this.manager.getReference());
        }

        // 写入 contig 信息
        for (int chromosomeIndex : manager.getChromosomeList()) {
            headerCache.write(ChromosomeInfo.getHeader(chromosomeIndex));
        }

        headerCache.write("\n##INFO=<ID=AC,Number=A,Type=Integer,Description=\"Allele count in genotypes\">" +
                "\n##INFO=<ID=AN,Number=1,Type=Integer,Description=\"Total number of alleles in called genotypes\">" +
                "\n##INFO=<ID=AF,Number=A,Type=Float,Description=\"Allele Frequency\">" +
                "\n##FORMAT=<ID=GT,Number=1,Type=String,Description=\"Genotype\">" +
                "\n#CHROM\tPOS\tID\tREF\tALT\tQUAL\tFILTER\tINFO\tFORMAT");
    }

    private void initPairs() {
        int eachGroupNum = this.manager.isPhased() ? 3 : 4;
        if (this.subjectIndexes == null) {
            pairs = new IndexPair[this.manager.getSubjectNum()];

            for (int i = 0; i < pairs.length; i++) {
                pairs[i] = new IndexPair(i, i / eachGroupNum, i % eachGroupNum);
            }
        }
    }

    /**
     * 读取下一个位点
     */
    public Variant readVariant() throws IOException {
        if (pointer.chromosomeIndex != -1) {
            this.cache.fill(pointer, this.pairs.length > 0);
            TaskVariant taskVariant = this.cache.taskVariants[pointer.variantIndex];
            GTBNode node = pointer.getNode();

            byte[] BEGs = new byte[this.pairs.length];
            fillBEGs(taskVariant, node, BEGs, this.phasedTransfer);

            pointer.next();
            return new Variant(node.chromosomeIndex, taskVariant.position, this.cache.allelesPosCache.cacheOf(taskVariant.alleleStart, taskVariant.alleleStart + taskVariant.alleleLength), BEGs, this.phased);
        } else {
            // 不存在下一个位点，此时 返回 null
            return null;
        }
    }

    public boolean readVariant(Variant variant) throws IOException {
        if (pointer.chromosomeIndex != -1) {
            this.cache.fill(pointer, this.pairs.length > 0);
            TaskVariant taskVariant = this.cache.taskVariants[pointer.variantIndex];
            GTBNode node = pointer.getNode();

            if (this.pairs.length != variant.BEGs.length) {
                variant.BEGs = new byte[this.pairs.length];
            }

            byte[] BEGs = variant.BEGs;

            fillBEGs(taskVariant, node, BEGs, this.phasedTransfer);

            variant.chromosome = ChromosomeInfo.getString(node.chromosomeIndex);
            variant.position = taskVariant.position;
            variant.ploidy = ChromosomeInfo.getPloidy(node.chromosomeIndex);

            int sepIndex = this.cache.allelesPosCache.indexOf(ByteCode.TAB, taskVariant.alleleStart, taskVariant.alleleStart + taskVariant.alleleLength);
            variant.REF = this.cache.allelesPosCache.cacheOf(taskVariant.alleleStart, sepIndex);
            variant.ALT = this.cache.allelesPosCache.cacheOf(sepIndex + 1, taskVariant.alleleStart + taskVariant.alleleLength);
            variant.phased = this.phased;

            pointer.next();
            return true;
        } else {
            // 不存在下一个位点，此时 返回 null
            variant.chromosome = null;
            variant.position = 0;
            variant.ploidy = 0;

            variant.REF = null;
            variant.ALT = null;
            variant.phased = this.phased;
            return false;
        }
    }

    private void fillBEGs(TaskVariant taskVariant, GTBNode node, byte[] BEGs, boolean phasedTransfer) {
        if (taskVariant.decoderIndex == 0) {
            // 二等位基因位点
            int start = this.eachLineSize * taskVariant.index;
            if (phasedTransfer) {
                for (int j = 0; j < pairs.length; j++) {
                    BEGs[j] = BEGTransfer.toUnphased(this.groupDecoder.decode(this.cache.genotypesCache.cacheOf(start + this.pairs[j].groupIndex) & 0xFF, this.pairs[j].codeIndex));
                }

            } else {
                for (int j = 0; j < pairs.length; j++) {
                    BEGs[j] = this.groupDecoder.decode(this.cache.genotypesCache.cacheOf(start + this.pairs[j].groupIndex) & 0xFF, this.pairs[j].codeIndex);
                }
            }
        } else {
            // 多等位基因位点
            int start = this.eachLineSize * node.subBlockVariantNum[0] + (taskVariant.index - node.subBlockVariantNum[0]) * this.manager.getSubjectNum();

            if (phasedTransfer) {
                for (int j = 0; j < pairs.length; j++) {
                    BEGs[j] = BEGTransfer.toUnphased(this.cache.genotypesCache.cacheOf(start + this.pairs[j].index));
                }
            } else {
                for (int j = 0; j < pairs.length; j++) {
                    BEGs[j] = this.cache.genotypesCache.cacheOf(start + this.pairs[j].index);
                }
            }
        }
    }

    public void skip(int variantNums) throws IOException {
        for (int i = 0; i < variantNums; i++) {
            pointer.next();
        }
    }

    public boolean reset() {
        return pointer.seek(0);
    }

    /**
     * 通过指针类进行跳转，请注意，该方法可能会破坏 limit 信息
     * @param pointer 指针管理类
     */
    public boolean seek(Pointer pointer) {
        this.pointer = pointer;
        return true;
    }

    /**
     * 设置指针
     * @param chromosome 染色体信息
     * @param position 位置值
     */
    public boolean seek(String chromosome, int position) throws IOException {
        return seek(ChromosomeInfo.getIndex(chromosome), position);
    }

    /**
     * 设置指针
     * @param originChromosomeIndex 染色体信息
     * @param position 位置值
     */
    public boolean seek(int originChromosomeIndex, int position) throws IOException {
        GTBNodes nodes = this.manager.getGTBNodes(originChromosomeIndex);
        Assert.that(nodes != null, RuntimeExceptionOptions.NullPointerException, "pointer exception");

        // 查看传入的染色体在当前的限定指针中的范围
        int chromosomeIndex = ArrayUtils.indexOf(pointer.chromosomeList, originChromosomeIndex);

        // 先检查边界
        if ((nodes.numOfNodes() == 0) || (nodes.get(nodes.numOfNodes() - 1).maxPos < position)) {
            if (chromosomeIndex == pointer.chromosomeList.length - 1) {
                // 已经到了最后的染色体
                return pointer.seek(-1);
            } else {
                return pointer.seek(chromosomeIndex + 1, 0, 0);
            }
        } else if (nodes.get(0).minPos >= position) {
            return pointer.seek(chromosomeIndex, 0, 0);
        }

        // 优先在本块中查找, 因为该方式无需解压数据
        if (pointer.node.contain(position)) {
            this.cache.fill(pointer, this.pairs.length > 0);

            if (this.cache.taskVariants[0].position == position) {
                return pointer.seek(chromosomeIndex, pointer.nodeIndex, 0);
            }

            for (int j = 0; j < pointer.variantLength - 1; j++) {
                if ((this.cache.taskVariants[j].position < position) && (position <= this.cache.taskVariants[j + 1].position)) {
                    return pointer.seek(chromosomeIndex, pointer.nodeIndex, j + 1);
                }
            }
        }

        // 使用类似二分搜索的方法
        if (pointer.node.minPos > position) {
            for (int i = 0; i < pointer.nodeIndex; i++) {
                if (nodes.get(i).contain(position)) {
                    pointer.seek(chromosomeIndex, i);
                    this.cache.fill(pointer, this.pairs.length > 0);

                    if (this.cache.taskVariants[0].position == position) {
                        return pointer.seek(chromosomeIndex, i, 0);
                    }

                    for (int j = 0; j < pointer.variantLength - 1; j++) {
                        if ((this.cache.taskVariants[j].position < position) && (position <= this.cache.taskVariants[j + 1].position)) {
                            return pointer.seek(chromosomeIndex, i, j + 1);
                        }
                    }
                }

                // 在两个块的间隔区
                if ((i < nodes.numOfNodes() - 1) && (nodes.get(i).maxPos < position) && (nodes.get(i + 1).minPos >= position)) {
                    return pointer.seek(chromosomeIndex, i + 1, 0);
                }
            }
        } else {
            for (int i = pointer.nodeIndex; i < nodes.numOfNodes(); i++) {
                if (nodes.get(i).contain(position)) {
                    pointer.seek(chromosomeIndex, i);
                    this.cache.fill(pointer, this.pairs.length > 0);

                    if (this.cache.taskVariants[0].position == position) {
                        return pointer.seek(chromosomeIndex, i, 0);
                    }

                    for (int j = 0; j < pointer.variantLength - 1; j++) {
                        if ((this.cache.taskVariants[j].position < position) && (position <= this.cache.taskVariants[j + 1].position)) {
                            return pointer.seek(chromosomeIndex, i, j + 1);
                        }
                    }
                }

                // 在两个块的间隔区
                if ((i < nodes.numOfNodes() - 1) && (nodes.get(i).maxPos < position) && (nodes.get(i + 1).minPos >= position)) {
                    return pointer.seek(chromosomeIndex, i + 1, 0);
                }
            }
        }

        throw new IOException();
    }

    public Pointer tell() {
        return pointer.clone();
    }

    public void selectSubjects(int... subjectIndexes) {
        int eachGroupNum = this.manager.isPhased() ? 3 : 4;
        this.subjectIndexes = subjectIndexes;

        pairs = new IndexPair[this.subjectIndexes.length];
        for (int i = 0; i < pairs.length; i++) {
            pairs[i] = new IndexPair(this.subjectIndexes[i], this.subjectIndexes[i] / eachGroupNum, this.subjectIndexes[i] % eachGroupNum);
        }
    }

    public void selectSubjects(String... subjects) {
        selectSubjects(subjects == null ? null : this.manager.getSubjectIndex(subjects));
    }

    public <SubjectType> void selectSubjects(Collection<SubjectType> subjects) {
        selectSubjects(this.manager.getSubjectIndex(subjects));
    }

    public void removeAllSubjects() {
        this.pairs = new IndexPair[0];
        this.subjectIndexes = new int[0];
    }

    public void selectAllSubjects() {
        initPairs();
    }

    public String[] getAllSubjects() {
        return this.manager.getSubjectManager().getAllSubjects();
    }

    public String[] getSelectedSubjects() {
        return this.manager.getSubject(subjectIndexes);
    }

    public int[] getSelectedSubjectsIndex() {
        return this.subjectIndexes;
    }

    public GTBManager getManager() {
        return manager;
    }

    public byte[] getHeader() {
        return getHeader(false);
    }

    public byte[] getHeader(boolean hideGT) {
        synchronized (headerCache) {
            if (headerCache.size() == 0) {
                initHeader();
            }
        }

        if (hideGT) {
            return headerCache.values();
        } else {
            VolumeByteStream out = new VolumeByteStream(headerCache.size() + 1 + this.manager.getSubjects().length);
            out.write(headerCache);
            out.write(ByteCode.TAB);

            if (this.subjectIndexes == null) {
                out.write(this.manager.getSubjects());
            } else {
                out.writeSafety(String.join("\t", this.manager.getSubject(this.subjectIndexes)).getBytes());
            }
            return out.values();
        }
    }

    @Override
    public void close() throws IOException {
        this.cache.close();
        this.pointer = null;
    }
}

