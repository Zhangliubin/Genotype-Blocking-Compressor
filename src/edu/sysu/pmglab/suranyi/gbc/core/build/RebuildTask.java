package edu.sysu.pmglab.suranyi.gbc.core.build;

import edu.sysu.pmglab.suranyi.check.Assert;
import edu.sysu.pmglab.suranyi.check.ioexception.IOExceptionOptions;
import edu.sysu.pmglab.suranyi.compressor.ICompressor;
import edu.sysu.pmglab.suranyi.container.SmartList;
import edu.sysu.pmglab.suranyi.gbc.constant.ChromosomeInfo;
import edu.sysu.pmglab.suranyi.gbc.core.common.allelechecker.AlleleChecker;
import edu.sysu.pmglab.suranyi.gbc.core.common.allelechecker.Chi2TestChecker;
import edu.sysu.pmglab.suranyi.gbc.core.common.qualitycontrol.variant.VariantAllelesNumController;
import edu.sysu.pmglab.suranyi.gbc.core.exception.GBCExceptionOptions;
import edu.sysu.pmglab.suranyi.gbc.core.gtbcomponent.GTBManager;
import edu.sysu.pmglab.suranyi.gbc.core.gtbcomponent.GTBRootCache;
import edu.sysu.pmglab.suranyi.unifyIO.FileStream;
import edu.sysu.pmglab.suranyi.unifyIO.options.FileOptions;

import java.io.IOException;
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
     * 对齐方法
     */
    private GTBManager templateFile;
    AlleleChecker alleleChecker;
    boolean keepAll;

    /**
     * 设置有关筛选数据的参数
     */
    private SmartList<String> subjects;

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
     * 坐标、碱基对齐
     *
     * @param templateFile 模版文件 (例如来自 1000GP3 的文件)
     */
    public RebuildTask alignWith(String templateFile) throws IOException {
        synchronized (this) {
            if (templateFile != null) {
                Assert.that(!templateFile.equals(this.inputFile.getFileName()), IOExceptionOptions.FileOccupiedException, "inputFile cannot be the same as templateFile");
                this.templateFile = GTBRootCache.get(templateFile);
            } else {
                this.templateFile = null;
            }
        }

        return this;
    }

    /**
     * 保留所有位点
     */
    public RebuildTask setSiteMergeType(String type) {
        synchronized (this) {
            if (type.equalsIgnoreCase("union")) {
                // 取并集
                this.keepAll = true;
            } else if (type.equalsIgnoreCase("intersection")) {
                this.keepAll = false;
            } else {
                throw new IllegalArgumentException("task.setSiteMergeType only supported union or intersection");
            }
        }
        return this;
    }

    public RebuildTask setKeepAll(boolean keepAll) {
        synchronized (this) {
            this.keepAll = keepAll;
        }

        return this;
    }

    /**
     * 检查 allele frequency 并进行校正
     */
    public RebuildTask setAlleleChecker(AlleleChecker checker) {
        synchronized (this) {
            this.alleleChecker = checker;
        }
        return this;
    }

    /**
     * 检查 allele frequency 并进行校正
     */
    public RebuildTask setAlleleChecker(boolean checkAf) {
        synchronized (this) {
            if (checkAf) {
                this.alleleChecker = new Chi2TestChecker();
            } else {
                this.alleleChecker = null;
            }
        }
        return this;
    }

    /**
     * 检查 allele frequency 并进行校正
     */
    public RebuildTask setAlleleChecker(boolean checkAf, double alpha) {
        synchronized (this) {
            if (checkAf) {
                this.alleleChecker = new Chi2TestChecker(alpha);
            } else {
                this.alleleChecker = null;
            }
        }
        return this;
    }

    /**
     * 获取等位基因检查器
     */
    public AlleleChecker getAlleleChecker() {
        return alleleChecker;
    }

    /**
     * 是否保留所有的位点
     */
    public boolean isKeepAll() {
        return keepAll;
    }

    /**
     * 获取管理器
     */
    public GTBManager getInputFile() {
        return inputFile;
    }

    /**
     * 获取模版文件
     */
    public GTBManager getTemplateFile() {
        return templateFile;
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
                this.subjects = new SmartList<>(subjects);
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
        return delete(ChromosomeInfo.getIndexes(chromosomes));
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
        return delete(ChromosomeInfo.getIndex(chromosome), nodeIndexes);
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
        return retain(ChromosomeInfo.getIndexes(chromosomes));
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
        return retain(ChromosomeInfo.getIndex(chromosome), nodeIndexes);
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
    @Override
    public void submit() throws IOException {
        synchronized (this) {
            if (this.outputFileName == null) {
                this.outputFileName = autoGenerateOutputFileName();
            }

            // if inputFileName = outputFileName
            this.inplace = this.inputFile.getFileName().equals(this.outputFileName) || (this.templateFile != null && this.inputFile.getFileName().equals(this.templateFile.getFileName()));

            if (templateFile != null) {
                // 对齐位点模式
                Assert.that(templateFile.isOrderedGTB(), GBCExceptionOptions.GTBComponentException, "GBC cannot merge unordered GTBs, please use `rebuild` to sort them first");
                Assert.that(inputFile.isOrderedGTB(), GBCExceptionOptions.GTBComponentException, "GBC cannot merge unordered GTBs, please use `rebuild` to sort them first");
                AlignedKernel.submit(this);
            } else {
                // 构建核心任务
                RebuildKernel.submit(this);
            }

            // 清除缓存数据
            GTBRootCache.clear(this.inputFile);
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
                (templateFile == null ? "" : "\n\ttemplateFile: " + this.templateFile.getFileName()) +
                "\n\toutputFile: " + this.outputFileName +
                "\n\tthreads: " + this.threads +
                "\n\tphased: " + this.phased +
                "\n\tsubjects: " + subjectInfo +
                "\n\treordering: " + this.reordering + (this.reordering ? " (" + this.windowSize + " - Accumulated Generating Sequence)" : "") +
                "\n\tblockSize: " + this.blockSize + " (-bs " + this.blockSizeType + ")" +
                "\n\tcompressionLevel: " + this.compressionLevel + " (" + ICompressor.getCompressorName(this.getCompressor()) + ")" +
                (templateFile == null ? "" : "\n\tsite selection: " + (this.keepAll ? "union" : "intersection") +
                        "\n\tcheck allele: " + (this.alleleChecker == null ? "false" : this.alleleChecker)) +
                (this.variantQC.size() == 0 ? "" : "\n\tvariantQC: " + this.variantQC) +
                (this.alleleQC.size() == 0 ? "" : "\n\talleleQC: " + this.alleleQC) +
                "\n}";
    }

    @Override
    public String autoGenerateOutputFileName() {
        return this.inputFile.getFileName();
    }
}
