package edu.sysu.pmglab.suranyi.gbc.core.build;

import edu.sysu.pmglab.suranyi.check.Assert;
import edu.sysu.pmglab.suranyi.check.ioexception.IOExceptionOptions;
import edu.sysu.pmglab.suranyi.compressor.ICompressor;
import edu.sysu.pmglab.suranyi.gbc.core.common.allelechecker.AlleleChecker;
import edu.sysu.pmglab.suranyi.gbc.core.common.allelechecker.Chi2TestChecker;
import edu.sysu.pmglab.suranyi.gbc.core.common.qualitycontrol.variant.VariantAllelesNumController;
import edu.sysu.pmglab.suranyi.gbc.core.exception.GBCExceptionOptions;
import edu.sysu.pmglab.suranyi.gbc.core.gtbcomponent.GTBManager;
import edu.sysu.pmglab.suranyi.gbc.core.gtbcomponent.GTBRootCache;

import java.io.File;
import java.io.IOException;

/**
 * @Data :2021/02/06
 * @Author :suranyi
 * @Contact :suranyi.sysu@gamil.com
 * @Description :重新构建 GTB 文件，不允许绕过 RebuildTake 发起任务
 */

public class AlignedTask extends IBuildTask {
    GTBManager templateFile;
    GTBManager inputFile;
    AlleleChecker alleleChecker;
    boolean keepAll;

    /**
     * 构造器
     *
     * @param inputFileName 输入文件名，可以是单个文件或文件夹
     */
    public AlignedTask(String templateFileName, String inputFileName, String outputFileName) {
        this.templateFile = GTBRootCache.get(templateFileName);
        this.inputFile = GTBRootCache.get(inputFileName);

        // 检查输出文件
        this.outputFileName = outputFileName;
    }

    /**
     * 设置质控的位点等位基因最大个数
     *
     * @param variantQualityControlAllelesNum 质控的等位基因最大个数
     */
    @Override
    public AlignedTask setVariantQualityControlAllelesNum(int variantQualityControlAllelesNum) {
        if (variantQualityControlAllelesNum < VariantAllelesNumController.MAX) {
            return (AlignedTask) addVariantQC(new VariantAllelesNumController(variantQualityControlAllelesNum));
        }

        return this;
    }

    /**
     * 保留所有位点
     */
    public AlignedTask setSiteMergeType(String type) {
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

    public AlignedTask setKeepAll(boolean keepAll) {
        synchronized (this) {
            this.keepAll = keepAll;
        }

        return this;
    }

    /**
     * 检查 allele frequency 并进行校正
     */
    public AlignedTask setAlleleChecker(AlleleChecker checker) {
        synchronized (this) {
            this.alleleChecker = checker;
        }
        return this;
    }

    /**
     * 检查 allele frequency 并进行校正
     */
    public AlignedTask setAlleleChecker(boolean checkAf) {
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
    public AlignedTask setAlleleChecker(boolean checkAf, double alpha) {
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
     * 运行，并发控制
     */
    @Override
    public void submit() throws IOException {
        synchronized (this) {
            if (this.outputFileName == null) {
                this.outputFileName = autoGenerateOutputFileName();
            }

            // 检查文件是否有序
            Assert.that(!templateFile.getFileName().equals(this.outputFileName), IOExceptionOptions.FileOccupiedException, "output file cannot be the same as the input file (" + this.outputFileName + ")");
            Assert.that(!inputFile.getFileName().equals(this.outputFileName), IOExceptionOptions.FileOccupiedException, "output file cannot be the same as the input file (" + this.outputFileName + ")");
            Assert.that(templateFile.isOrderedGTB(), GBCExceptionOptions.GTBComponentException, "GBC cannot merge unordered GTBs, please use `rebuild` to sort them first");
            Assert.that(inputFile.isOrderedGTB(), GBCExceptionOptions.GTBComponentException, "GBC cannot merge unordered GTBs, please use `rebuild` to sort them first");

            // 构建核心任务
            AlignedKernel.submit(this);

            // 清除缓存数据
            GTBRootCache.clear(this.outputFileName);
        }
    }

    /**
     * 打印方法，返回当前任务设置情况
     */
    @Override
    public String toString() {
        return "MergeTask {" +
                "\n\tinputFile: " + "" +
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
        return new File(inputFile.getFileName()).getParent() + "/align.gtb";
    }
}
