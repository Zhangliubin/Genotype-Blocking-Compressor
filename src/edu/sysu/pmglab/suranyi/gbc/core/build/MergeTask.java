package edu.sysu.pmglab.suranyi.gbc.core.build;

import edu.sysu.pmglab.suranyi.check.Assert;
import edu.sysu.pmglab.suranyi.check.ioexception.IOExceptionOptions;
import edu.sysu.pmglab.suranyi.compressor.ICompressor;
import edu.sysu.pmglab.suranyi.container.SmartList;
import edu.sysu.pmglab.suranyi.gbc.core.common.allelechecker.AlleleChecker;
import edu.sysu.pmglab.suranyi.gbc.core.common.allelechecker.Chi2TestChecker;
import edu.sysu.pmglab.suranyi.gbc.core.common.qualitycontrol.variant.VariantAllelesNumController;
import edu.sysu.pmglab.suranyi.gbc.core.exception.GBCExceptionOptions;
import edu.sysu.pmglab.suranyi.gbc.core.gtbcomponent.GTBManager;
import edu.sysu.pmglab.suranyi.gbc.core.gtbcomponent.GTBRootCache;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

/**
 * @Data :2021/02/06
 * @Author :suranyi
 * @Contact :suranyi.sysu@gamil.com
 * @Description :合并多个 GTB 文件，不允许绕过 MergeTask 发起任务
 */

public class MergeTask extends IBuildTask {
    final SmartList<GTBManager> managers;
    AlleleChecker alleleChecker;
    boolean keepAll;

    /**
     * 构造器
     *
     * @param inputFileName 输入文件名，可以是单个文件或文件夹
     */
    public MergeTask(String inputFileName) {
        this(inputFileName, inputFileName);
    }

    /**
     * 构造器
     *
     * @param inputFileNames 输入文件名，可以是单个文件或文件夹
     */
    public MergeTask(String[] inputFileNames) {
        this(inputFileNames, inputFileNames[0]);
    }


    /**
     * 构造器
     *
     * @param inputFileName  输入文件名，单个文件
     * @param outputFileName 输出文件名，只能是单个文件名
     */
    public MergeTask(String inputFileName, String outputFileName) {
        this(new String[]{Assert.NotNull(inputFileName)}, outputFileName);
    }

    /**
     * 构造器
     *
     * @param inputFileNames 输入文件名，长度不能等于 0，否则是无效的输入
     * @param outputFileName 输出文件名，只能是单个文件名
     */
    public MergeTask(String[] inputFileNames, String outputFileName) {
        this(new SmartList<>(inputFileNames), outputFileName);
    }

    /**
     * 构造器
     *
     * @param inputFileNames 输入文件名，长度不能等于 0，否则是无效的输入
     * @param outputFileName 输出文件名，只能是单个文件名
     */
    public MergeTask(Collection<String> inputFileNames, String outputFileName) {
        // 非空且非长度非 0
        Assert.NotEmpty(inputFileNames);

        // 将其包装为智能列表
        SmartList<String> temps = new SmartList<>(inputFileNames);
        temps.dropDuplicated();

        // 获取不重复的文件管理器
        this.managers = new SmartList<>(GTBRootCache.get(temps));

        // 检查输出文件
        this.outputFileName = outputFileName;
    }

    /**
     * 设置质控的位点等位基因最大个数
     *
     * @param variantQualityControlAllelesNum 质控的等位基因最大个数
     */
    @Override
    public MergeTask setVariantQualityControlAllelesNum(int variantQualityControlAllelesNum) {
        if (variantQualityControlAllelesNum < VariantAllelesNumController.MAX) {
            return (MergeTask) addVariantQC(new VariantAllelesNumController(variantQualityControlAllelesNum));
        }

        return this;
    }

    /**
     * 保留所有位点
     */
    public MergeTask setSiteMergeType(String type) {
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

    public MergeTask setKeepAll(boolean keepAll) {
        synchronized (this) {
            this.keepAll = keepAll;
        }

        return this;
    }

    /**
     * 检查 allele frequency 并进行校正
     */
    public MergeTask setAlleleChecker(AlleleChecker checker) {
        synchronized (this) {
            this.alleleChecker = checker;
        }
        return this;
    }

    /**
     * 检查 allele frequency 并进行校正
     */
    public MergeTask setAlleleChecker(boolean checkAf) {
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
    public MergeTask setAlleleChecker(boolean checkAf, double alpha) {
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
    public SmartList<GTBManager> getManagers() {
        return this.managers;
    }

    /**
     * 获取管理器
     */
    public GTBManager getManager(int index) {
        return this.managers.get(index);
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

            // 检查文件是否有序
            for (GTBManager manager : this.managers) {
                Assert.that(!manager.getFileName().equals(this.outputFileName), IOExceptionOptions.FileOccupiedException, "output file cannot be the same as the input file (" + this.outputFileName + ")");
                Assert.that(manager.isOrderedGTB(), GBCExceptionOptions.GTBComponentException, "GBC cannot merge unordered GTBs, please use `rebuild` to sort them first");
            }

            Assert.that(this.managers.size() >= 2, GBCExceptionOptions.GTBComponentException, "the number of input files is less than 2");

            // 构建核心任务
            MergeKernel.submit(this);

            // 清除缓存数据
            GTBRootCache.clear(this.outputFileName);
        }
    }

    /**
     * 打印方法，返回当前任务设置情况
     */
    @Override
    public String toString() {
        StringBuilder inputInfo = new StringBuilder();
        inputInfo.append(this.managers.get(0).getFileName());

        for (int i = 1; i < this.managers.size(); i++) {
            inputInfo.append(", ").append(this.managers.get(i).getFileName());
        }

        return "MergeTask {" +
                "\n\tinputFile: " + inputInfo +
                "\n\toutputFile: " + this.outputFileName +
                "\n\tthreads: " + this.threads +
                "\n\tphased: " + this.phased +
                "\n\treordering: " + this.reordering + (this.reordering ? " (" + this.windowSize + " - Accumulated Generating Sequence)" : "") +
                "\n\tblockSize: " + this.blockSize + " (-bs " + this.blockSizeType + ")" +
                "\n\tcompressionLevel: " + this.compressionLevel + " (" + ICompressor.getCompressorName(this.getCompressor()) + ")" +
                "\n\tsite selection: " + (this.keepAll ? "union" : "intersection") +
                "\n\tcheck allele: " + (this.alleleChecker == null ? "false" : this.alleleChecker) +
                (this.variantQC.size() == 0 ? "" : "\n\tvariantQC: " + this.variantQC.toString()) +
                (this.alleleQC.size() == 0 ? "" : "\n\talleleQC: " + this.alleleQC.toString()) +
                "\n}";
    }

    @Override
    public String autoGenerateOutputFileName() {
        return new File(this.managers.get(0).getFileName()).getParent() + "/merge.gtb";
    }
}
