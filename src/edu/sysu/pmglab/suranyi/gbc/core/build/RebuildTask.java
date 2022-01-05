package edu.sysu.pmglab.suranyi.gbc.core.build;

import edu.sysu.pmglab.suranyi.compressor.ICompressor;
import edu.sysu.pmglab.suranyi.container.SmartList;
import edu.sysu.pmglab.suranyi.gbc.constant.ChromosomeInfo;
import edu.sysu.pmglab.suranyi.gbc.core.common.qualitycontrol.variant.VariantAllelesNumController;
import edu.sysu.pmglab.suranyi.gbc.core.gtbcomponent.GTBManager;
import edu.sysu.pmglab.suranyi.gbc.core.gtbcomponent.GTBRootCache;
import edu.sysu.pmglab.suranyi.unifyIO.FileStream;
import edu.sysu.pmglab.suranyi.unifyIO.options.FileOptions;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @Data        :2021/02/06
 * @Author      :suranyi
 * @Contact     :suranyi.sysu@gamil.com
 * @Description :重新构建 GTB 文件，不允许绕过 RebuildTake 发起任务
 */

public class RebuildTask extends IBuildTask {
    private final GTBManager manager;

    /**
     * 设置有关筛选数据的参数
     */
    private SmartList<String> subjects;

    private boolean inplace = false;

    /**
     * 构造器
     * @param inputFileName 输入文件名，可以是单个文件或文件夹
     */
    public RebuildTask(String inputFileName) {
        this(inputFileName, inputFileName);
    }

    /**
     * 构造器
     * @param inputFileName 输入文件名，长度不能等于 0，否则是无效的输入
     * @param outputFileName 输出文件名，只能是单个文件名
     */
    public RebuildTask(String inputFileName, String outputFileName) {
        this.manager = GTBRootCache.get(inputFileName);

        // 检查输出文件
        this.outputFileName = outputFileName;
    }

    /**
     * 获取管理器
     */
    public GTBManager getManager() {
        return manager;
    }

    /**
     * 选择提取的样本数据，默认提取所有数据
     * @param subjects 样本序列
     */
    public RebuildTask selectSubjects(String... subjects) {
        synchronized (this) {
            if (subjects == null) {
                this.subjects = null;
            } else {
                this.subjects = new SmartList<>(subjects);
                this.subjects.dropDuplicated();
            }
        }
        return this;
    }

    /**
     * 添加要提取的样本数据
     * @param subjects 添加的样本序列
     */
    public RebuildTask addSubjects(String... subjects) {
        synchronized (this) {
            if (subjects != null) {
                if (this.subjects == null) {
                    this.subjects = new SmartList<>(subjects);
                } else {
                    this.subjects.add(subjects);
                }

                this.subjects.dropDuplicated();
            }

            return this;
        }
    }

    /**
     * 选择提取的样本数据，默认重构所有数据
     * @param subjectFileName 样本序列文件
     * @param regex 样本名的分隔符，一般为 \n,\t或逗号
     */
    public RebuildTask selectSubjectsFromFile(String subjectFileName, String regex) throws IOException {
        try (FileStream fs = new FileStream(subjectFileName, FileOptions.CHANNEL_READER)) {
            return selectSubjects(new String(fs.readAll()).split(regex));
        }
    }

    /**
     * 选择提取的样本数据，默认提取所有数据
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
    public SmartList<String> getSubjects() {
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
     * @param chromosomeIndexes 删除的染色体编号
     */
    public RebuildTask delete(int... chromosomeIndexes) {
        synchronized (this) {
            // 移除指定的染色体数据
            this.manager.getGtbTree().remove(chromosomeIndexes);
        }

        return this;
    }

    /**
     * 删除染色体数据
     * @param chromosomes 删除的染色体编号
     */
    public RebuildTask delete(String... chromosomes) {
        return delete(ChromosomeInfo.getIndexes(chromosomes));
    }

    /**
     * 删除染色体数据
     * @param chromosomeIndex 删除的染色体编号
     * @param nodeIndexes 该染色体编号的节点信息，若该值为 null，则删除全部信息
     */
    public RebuildTask delete(int chromosomeIndex, int[] nodeIndexes) {
        HashMap<Integer, int[]> chromosomeNodes = new HashMap<>(1);
        chromosomeNodes.put(chromosomeIndex, nodeIndexes);
        return delete(chromosomeNodes);
    }

    /**
     * 删除染色体数据
     * @param chromosome 删除的染色体编号
     * @param nodeIndexes 该染色体编号的节点信息，若该值为 null，则删除全部信息
     */
    public RebuildTask delete(String chromosome, int[] nodeIndexes) {
        return delete(ChromosomeInfo.getIndex(chromosome), nodeIndexes);
    }

    /**
     * 删除染色体数据
     * @param chromosomeNodeList 删除的染色体编号及对应编号下移除的节点整数数组
     */
    public <ChromosomeType> RebuildTask delete(Map<ChromosomeType, int[]> chromosomeNodeList) {
        synchronized (this) {
            // 移除指定的染色体数据
            this.manager.getGtbTree().remove(chromosomeNodeList);
        }

        return this;
    }

    /**
     * 保留染色体数据
     * @param chromosomeIndexes 保留的染色体编号
     */
    public RebuildTask retain(int... chromosomeIndexes) {
        synchronized (this) {
            // 移除指定的染色体数据
            this.manager.getGtbTree().retain(chromosomeIndexes);
        }

        return this;
    }

    /**
     * 保留染色体数据
     * @param chromosomes 保留的染色体编号
     */
    public RebuildTask retain(String... chromosomes) {
        return retain(ChromosomeInfo.getIndexes(chromosomes));
    }

    /**
     * 保留染色体数据
     * @param chromosomeNodeList 保留的染色体编号及对应编号下保留的节点整数数组
     */
    public <ChromosomeType> RebuildTask retain(Map<ChromosomeType, int[]> chromosomeNodeList) {
        synchronized (this) {
            // 移除指定的染色体数据
            this.manager.getGtbTree().retain(chromosomeNodeList);
        }

        return this;
    }

    /**
     * 保留染色体数据
     * @param chromosome 保留的染色体编号
     * @param nodeIndexes 该染色体编号的节点信息，若该值为 null，则保留全部信息
     */
    public RebuildTask retain(String chromosome, int[] nodeIndexes) {
        return retain(ChromosomeInfo.getIndex(chromosome), nodeIndexes);
    }

    /**
     * 保留染色体数据
     * @param chromosomeIndex 保留的染色体编号
     * @param nodeIndexes 该染色体编号的节点信息，若该值为 null，则保留全部信息
     */
    public RebuildTask retain(int chromosomeIndex, int[] nodeIndexes) {
        HashMap<Integer, int[]> chromosomeNodes = new HashMap<>(1);
        chromosomeNodes.put(chromosomeIndex, nodeIndexes);
        return retain(chromosomeNodes);
    }

    /**
     * 设置质控的位点等位基因最大个数
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
    @Override
    public void submit() throws IOException {
        synchronized (this) {
            if (this.outputFileName == null) {
                this.outputFileName = autoGenerateOutputFileName();
            }

            // if inputFileName = outputFileName
            this.inplace = this.manager.getFileName().equals(this.outputFileName);

            // 构建核心任务
            RebuildKernel.submit(this);

            // 清除缓存数据
            GTBRootCache.clear(this.manager);
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
                "\n\tinputFile: " + this.manager.getFileName() +
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
        return this.manager.getFileName();
    }
}
