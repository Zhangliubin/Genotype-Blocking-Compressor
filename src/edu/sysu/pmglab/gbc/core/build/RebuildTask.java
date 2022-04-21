package edu.sysu.pmglab.gbc.core.build;

import edu.sysu.pmglab.container.array.StringArray;
import edu.sysu.pmglab.gbc.constant.ChromosomeTags;
import edu.sysu.pmglab.gbc.core.common.allelechecker.AlleleChecker;
import edu.sysu.pmglab.gbc.core.common.qualitycontrol.variant.VariantAllelesNumController;
import edu.sysu.pmglab.gbc.core.gtbcomponent.GTBManager;
import edu.sysu.pmglab.gbc.core.gtbcomponent.GTBRootCache;
import edu.sysu.pmglab.check.Assert;
import edu.sysu.pmglab.check.ioexception.IOExceptionOptions;
import edu.sysu.pmglab.compressor.ICompressor;
import edu.sysu.pmglab.easytools.FileUtils;
import edu.sysu.pmglab.gbc.core.exception.GBCExceptionOptions;
import edu.sysu.pmglab.unifyIO.FileStream;
import edu.sysu.pmglab.unifyIO.options.FileOptions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * @Data :2021/02/06
 * @Author :suranyi
 * @Contact :suranyi.sysu@gamil.com
 * @Description :重新构建 GTB 文件，不允许绕过 RebuildTake 发起任务
 */

public class RebuildTask extends IBuildTask {
    private final GTBManager inputFile;

    /**
     * 设置有关筛选数据的参数
     */
    private StringArray subjects;

    private boolean inplace = false;

    /**
     * 构造器
     *
     * @param inputFileName 输入文件名，可以是单个文件或文件夹
     */
    public RebuildTask(String inputFileName) {
        this(inputFileName, inputFileName);
    }

    /**
     * 构造器
     *
     * @param inputFileName  输入文件名，长度不能等于 0，否则是无效的输入
     * @param outputFileName 输出文件名，只能是单个文件名
     */
    public RebuildTask(String inputFileName, String outputFileName) {
        this.inputFile = GTBRootCache.get(inputFileName);

        // 检查输出文件
        this.outputFileName = outputFileName;
    }

    /**
     * 获取管理器
     */
    public GTBManager getInputFile() {
        return inputFile;
    }

    /**
     * 选择提取的样本数据，默认提取所有数据
     *
     * @param subjects 样本序列
     */
    public RebuildTask selectSubjects(String... subjects) {
        synchronized (this) {
            if (subjects == null) {
                this.subjects = null;
            } else {
                this.subjects = new StringArray(subjects);
                this.subjects.dropDuplicated();
            }
        }
        return this;
    }

    /**
     * 添加要提取的样本数据
     *
     * @param subjects 添加的样本序列
     */
    public RebuildTask addSubjects(String... subjects) {
        synchronized (this) {
            if (subjects != null) {
                if (this.subjects == null) {
                    this.subjects = new StringArray(subjects);
                } else {
                    this.subjects.addAll(subjects);
                }

                this.subjects.dropDuplicated();
            }

            return this;
        }
    }

    /**
     * 选择提取的样本数据，默认重构所有数据
     *
     * @param subjectFileName 样本序列文件
     * @param regex           样本名的分隔符，一般为 \n,\t或逗号
     */
    public RebuildTask selectSubjectsFromFile(String subjectFileName, String regex) throws IOException {
        try (FileStream fs = new FileStream(subjectFileName, FileOptions.CHANNEL_READER)) {
            return selectSubjects(new String(fs.readAll()).split(regex));
        }
    }

    /**
     * 选择提取的样本数据，默认提取所有数据
     *
     * @param subjectFileName 样本序列文件
     */
    public RebuildTask addSubjectsFromFile(String subjectFileName, String regex) throws IOException {
        if (this.subjects == null) {
            return selectSubjectsFromFile(subjectFileName, regex);
        } else {
            try (FileStream fs = new FileStream(subjectFileName, FileOptions.CHANNEL_READER)) {
                return addSubjects(new String(fs.readAll()).split(regex));
            }
        }
    }

    /**
     * 获取 样本名序列
     */
    public StringArray getSubjects() {
        return this.subjects;
    }

    /**
     * 是否替换文件
     */
    public boolean isInplace() {
        return this.inplace;
    }

    /**
     * 删除染色体数据
     *
     * @param chromosomeIndexes 删除的染色体编号
     */
    public RebuildTask delete(int... chromosomeIndexes) {
        synchronized (this) {
            // 移除指定的染色体数据
            this.inputFile.getGtbTree().remove(chromosomeIndexes);
        }

        return this;
    }

    /**
     * 删除染色体数据
     *
     * @param chromosomes 删除的染色体编号
     */
    public RebuildTask delete(String... chromosomes) {
        return delete(ChromosomeTags.getIndexes(chromosomes));
    }

    /**
     * 删除染色体数据
     *
     * @param chromosomeIndex 删除的染色体编号
     * @param nodeIndexes     该染色体编号的节点信息，若该值为 null，则删除全部信息
     */
    public RebuildTask delete(int chromosomeIndex, int[] nodeIndexes) {
        HashMap<Integer, int[]> chromosomeNodes = new HashMap<>(1);
        chromosomeNodes.put(chromosomeIndex, nodeIndexes);
        return delete(chromosomeNodes);
    }

    /**
     * 删除染色体数据
     *
     * @param chromosome  删除的染色体编号
     * @param nodeIndexes 该染色体编号的节点信息，若该值为 null，则删除全部信息
     */
    public RebuildTask delete(String chromosome, int[] nodeIndexes) {
        return delete(ChromosomeTags.getIndex(chromosome), nodeIndexes);
    }

    /**
     * 删除染色体数据
     *
     * @param chromosomeNodeList 删除的染色体编号及对应编号下移除的节点整数数组
     */
    public <ChromosomeType> RebuildTask delete(Map<ChromosomeType, int[]> chromosomeNodeList) {
        synchronized (this) {
            // 移除指定的染色体数据
            this.inputFile.getGtbTree().remove(chromosomeNodeList);
        }

        return this;
    }

    /**
     * 保留染色体数据
     *
     * @param chromosomeIndexes 保留的染色体编号
     */
    public RebuildTask retain(int... chromosomeIndexes) {
        synchronized (this) {
            // 移除指定的染色体数据
            this.inputFile.getGtbTree().retain(chromosomeIndexes);
        }

        return this;
    }

    /**
     * 保留染色体数据
     *
     * @param chromosomes 保留的染色体编号
     */
    public RebuildTask retain(String... chromosomes) {
        return retain(ChromosomeTags.getIndexes(chromosomes));
    }

    /**
     * 保留染色体数据
     *
     * @param chromosomeNodeList 保留的染色体编号及对应编号下保留的节点整数数组
     */
    public <ChromosomeType> RebuildTask retain(Map<ChromosomeType, int[]> chromosomeNodeList) {
        synchronized (this) {
            // 移除指定的染色体数据
            this.inputFile.getGtbTree().retain(chromosomeNodeList);
        }

        return this;
    }

    /**
     * 保留染色体数据
     *
     * @param chromosome  保留的染色体编号
     * @param nodeIndexes 该染色体编号的节点信息，若该值为 null，则保留全部信息
     */
    public RebuildTask retain(String chromosome, int[] nodeIndexes) {
        return retain(ChromosomeTags.getIndex(chromosome), nodeIndexes);
    }

    /**
     * 保留染色体数据
     *
     * @param chromosomeIndex 保留的染色体编号
     * @param nodeIndexes     该染色体编号的节点信息，若该值为 null，则保留全部信息
     */
    public RebuildTask retain(int chromosomeIndex, int[] nodeIndexes) {
        HashMap<Integer, int[]> chromosomeNodes = new HashMap<>(1);
        chromosomeNodes.put(chromosomeIndex, nodeIndexes);
        return retain(chromosomeNodes);
    }

    /**
     * 设置质控的位点等位基因最大个数
     *
     * @param variantQualityControlAllelesNum 质控的等位基因最大个数
     */
    @Override
    public RebuildTask setVariantQualityControlAllelesNum(int variantQualityControlAllelesNum) {
        if (variantQualityControlAllelesNum < VariantAllelesNumController.MAX) {
            return (RebuildTask) addVariantQC(new VariantAllelesNumController(variantQualityControlAllelesNum));
        }

        return this;
    }

    /**
     * 运行，并发控制
     */
    public void rebuildAll() throws IOException {
        synchronized (this) {
            if (this.outputFileName == null) {
                this.outputFileName = autoGenerateOutputFileName();
            }

            // if inputFileName = outputFileName
            this.inplace = this.inputFile.getFileName().equals(this.outputFileName);

            // 构建核心任务
            RebuildKernel.rebuildAll(this);

            // 清除缓存数据
            GTBRootCache.clear(this.outputFileName);
        }
    }

    /**
     * 重压缩指定的多个染色体数据
     * @param chromosomeIndexes 重压缩的染色体
     */
    public void rebuildByChromosome(int... chromosomeIndexes) throws IOException {
        synchronized (this) {
            if (this.outputFileName == null) {
                this.outputFileName = autoGenerateOutputFileName();
            }

            // if inputFileName = outputFileName
            this.inplace = this.inputFile.getFileName().equals(this.outputFileName);

            // 构建核心任务
            RebuildKernel.rebuildByChromosome(this, chromosomeIndexes);

            // 清除缓存数据
            GTBRootCache.clear(this.outputFileName);
        }
    }

    /**
     * 重压缩指定的多个染色体数据
     * @param chromosomes 重压缩的染色体
     */
    public void rebuildByChromosome(String... chromosomes) throws IOException {
        rebuildByChromosome(ChromosomeTags.getIndexes(chromosomes));
    }


    /**
     * 重压缩指定染色体
     * @param chromosome 染色体编号
     */
    public void rebuildByRange(String chromosome) throws IOException {
        rebuildByRange(ChromosomeTags.getIndex(chromosome), 0);
    }

    /**
     * 重压缩指定染色体从 minPos 开始的位点
     * @param chromosomeIndex 染色体编号
     * @param startPos 开始的位点
     */
    public void rebuildByRange(int chromosomeIndex, int startPos) throws IOException {
        rebuildByRange(chromosomeIndex, startPos, Integer.MAX_VALUE);
    }

    /**
     * 重压缩指定染色体从 minPos 开始的位点
     * @param chromosome 染色体编号
     * @param startPos 开始的位点
     */
    public void rebuildByRange(String chromosome, int startPos) throws IOException {
        rebuildByRange(ChromosomeTags.getIndex(chromosome), startPos);
    }

    /**
     * 重压缩指定染色体从 minPos-endPos 的位点
     * @param chromosomeIndex 染色体编号
     * @param startPos 开始的位点
     * @param endPos 结束的位点
     */
    public void rebuildByRange(int chromosomeIndex, int startPos, int endPos) throws IOException {
        synchronized (this) {
            if (this.outputFileName == null) {
                this.outputFileName = autoGenerateOutputFileName();
            }

            // if inputFileName = outputFileName
            this.inplace = this.inputFile.getFileName().equals(this.outputFileName);

            // 构建核心任务
            RebuildKernel.rebuildByRange(this, chromosomeIndex, startPos, endPos);

            // 清除缓存数据
            GTBRootCache.clear(this.outputFileName);
        }
    }

    /**
     * 重压缩指定染色体从 minPos-endPos 的位点
     * @param chromosome 染色体编号
     * @param startPos 开始的位点
     * @param endPos 结束的位点
     */
    public void rebuildByRange(String chromosome, int startPos, int endPos) throws IOException {
        rebuildByRange(ChromosomeTags.getIndex(chromosome), startPos, endPos);
    }

    /**
     * 按照指定位置重压缩
     * @param chromosomeIndex 染色体编号
     * @param positions 待重压缩的位置数据
     */
    public void rebuildByPosition(int chromosomeIndex, int[] positions) throws IOException {
        HashMap<Integer, int[]> chromosomeMap = new HashMap<>(1);
        chromosomeMap.put(chromosomeIndex, positions);
        rebuildByPosition(chromosomeMap);
    }

    /**
     * 按照指定位置重压缩
     * @param chromosome 染色体编号
     * @param positions 待重压缩的位置数据
     */
    public void rebuildByPosition(String chromosome, int[] positions) throws IOException {
        rebuildByPosition(ChromosomeTags.getIndex(chromosome), positions);
    }

    /**
     * 按照指定位置重压缩
     * @param chromosomePositions 染色体-位置数据对
     */
    public <ChromosomeType> void rebuildByPosition(Map<ChromosomeType, int[]> chromosomePositions) throws IOException {
        synchronized (this) {
            if (this.outputFileName == null) {
                this.outputFileName = autoGenerateOutputFileName();
            }

            // if inputFileName = outputFileName
            this.inplace = this.inputFile.getFileName().equals(this.outputFileName);

            // 构建核心任务
            RebuildKernel.rebuildByPosition(this, chromosomePositions);

            // 清除缓存数据
            GTBRootCache.clear(this.outputFileName);
        }
    }

    /**
     * 传入存放的位置数据文件进行重压缩
     * @param fileName 位置数据文件
     * @param groupRegex 不同组位置数据的分隔符
     * @param positionRegex 染色体-位置 之间的分割符
     */
    public void rebuildByPositionFromFile(String fileName, String groupRegex, String positionRegex) throws IOException {
        try (FileStream fs = new FileStream(fileName, FileOptions.CHANNEL_READER)) {
            HashMap<Integer, ArrayList<Integer>> chromosomePositions = new HashMap<>(24);
            String[] groups = new String(fs.readAll()).split(groupRegex);

            for (String group : groups) {
                String[] groupData = group.split(positionRegex);
                int chromosomeIndex = ChromosomeTags.getIndex(groupData[0]);

                if (!chromosomePositions.containsKey(chromosomeIndex)) {
                    chromosomePositions.put(chromosomeIndex, new ArrayList<>(32));
                }
                chromosomePositions.get(chromosomeIndex).add(Integer.valueOf(groupData[1]));
            }

            HashMap<Integer, int[]> realChromosomePositions = new HashMap<>(chromosomePositions.size());
            for (int key : chromosomePositions.keySet()) {
                ArrayList<Integer> value = chromosomePositions.get(key);
                int[] cache = new int[value.size()];
                for (int i = 0; i < cache.length; i++) {
                    cache[i] = value.get(i);
                }
                realChromosomePositions.put(key, cache);
            }

            rebuildByPosition(realChromosomePositions);
        }
    }

    /**
     * 坐标、碱基对齐
     *
     * @param templateFile 模版文件 (例如来自 1000GP3 的文件)
     */
    public void alignWith(String templateFile, boolean keepAll, AlleleChecker checker) throws IOException {
        synchronized (this) {
            Assert.that(FileUtils.exists(templateFile), IOExceptionOptions.FileNotFoundException, templateFile + " not found");

            if (this.outputFileName == null) {
                this.outputFileName = autoGenerateOutputFileName();
            }

            // if inputFileName = outputFileName
            this.inplace = this.inputFile.getFileName().equals(this.outputFileName) || (templateFile.equals(this.outputFileName));

            // 对齐位点模式
            GTBManager templateManager = GTBRootCache.get(templateFile);
            Assert.that(!templateFile.equals(this.inputFile.getFileName()), IOExceptionOptions.FileOccupiedException, "inputFile cannot be the same as templateFile");
            Assert.that(templateManager.isOrderedGTB(), GBCExceptionOptions.GTBComponentException, "GBC cannot align unordered GTBs, please use `rebuild` to sort them first");
            Assert.that(inputFile.isOrderedGTB(), GBCExceptionOptions.GTBComponentException, "GBC cannot align unordered GTBs, please use `rebuild` to sort them first");

            AlignedKernel.submit(this, templateManager, keepAll, checker);
            GTBRootCache.clear(this.outputFileName);
        }
    }

    /**
     * 坐标、碱基对齐
     *
     * @param templateFile 模版文件 (例如来自 1000GP3 的文件)
     */
    public void alignWith(String templateFile, String type, AlleleChecker checker) throws IOException {
        boolean keepAll;

        if ("union".equalsIgnoreCase(type)) {
            // 取并集
            keepAll = true;
        } else if ("intersection".equalsIgnoreCase(type)) {
            keepAll = false;
        } else {
            throw new IllegalArgumentException("task.setSiteMergeType only supported union or intersection");
        }

        alignWith(templateFile, keepAll, checker);
    }

    /**
     * 转为二等位基因位点
     */
    public void convertToBiallelic() throws IOException {
        synchronized (this) {
            if (this.outputFileName == null) {
                this.outputFileName = autoGenerateOutputFileName();
            }

            // if inputFileName = outputFileName
            this.inplace = this.inputFile.getFileName().equals(this.outputFileName);

            Assert.that(inputFile.isOrderedGTB(), GBCExceptionOptions.GTBComponentException, "GBC cannot normalize unordered GTBs, please use `rebuild` to sort them first");

            // 构建核心任务
            NormKernel.submit(this, false);

            // 清除缓存数据
            GTBRootCache.clear(this.outputFileName);
        }
    }

    /**
     * 转为多等位基因位点
     */
    public void convertToMultiallelic() throws IOException {
        synchronized (this) {
            if (this.outputFileName == null) {
                this.outputFileName = autoGenerateOutputFileName();
            }

            // if inputFileName = outputFileName
            this.inplace = this.inputFile.getFileName().equals(this.outputFileName);

            Assert.that(inputFile.isOrderedGTB(), GBCExceptionOptions.GTBComponentException, "GBC cannot normalize unordered GTBs, please use `rebuild` to sort them first");

            // 构建核心任务
            NormKernel.submit(this, true);

            // 清除缓存数据
            GTBRootCache.clear(this.outputFileName);
        }
    }

    /**
     * 打印方法，返回当前任务设置情况
     */
    @Override
    public String toString() {
        String subjectInfo;
        if (this.subjects == null) {
            subjectInfo = "<all subjects>";
        } else {
            subjectInfo = this.subjects.toString(5) + " (" + this.subjects.size() + " subjects in total)";
        }

        return "RebuildTask {" +
                "\n\tinputFile: " + this.inputFile.getFileName() +
                "\n\toutputFile: " + this.outputFileName +
                "\n\tthreads: " + this.threads +
                "\n\tphased: " + this.phased +
                "\n\tsubjects: " + subjectInfo +
                "\n\treordering: " + this.reordering + (this.reordering ? " (" + this.windowSize + " - Accumulated Generating Sequence)" : "") +
                "\n\tblockSize: " + this.blockSize + " (-bs " + this.blockSizeType + ")" +
                "\n\tcompressionLevel: " + this.compressionLevel + " (" + ICompressor.getCompressorName(this.getCompressor()) + ")" +
                (this.variantQC.size() == 0 ? "" : "\n\tvariantQC: " + this.variantQC) +
                (this.alleleQC.size() == 0 ? "" : "\n\talleleQC: " + this.alleleQC) +
                "\n}";
    }

    @Override
    public String autoGenerateOutputFileName() {
        return this.inputFile.getFileName();
    }
}
