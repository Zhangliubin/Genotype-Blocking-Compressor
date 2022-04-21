package edu.sysu.pmglab.gbc.core.build;

import edu.sysu.pmglab.check.Assert;
import edu.sysu.pmglab.check.ioexception.IOExceptionOptions;
import edu.sysu.pmglab.compressor.ICompressor;
import edu.sysu.pmglab.container.array.StringArray;
import edu.sysu.pmglab.easytools.ArrayUtils;
import edu.sysu.pmglab.easytools.FileUtils;
import edu.sysu.pmglab.gbc.core.common.qualitycontrol.genotype.GenotypeDPController;
import edu.sysu.pmglab.gbc.core.common.qualitycontrol.genotype.GenotypeGQController;
import edu.sysu.pmglab.gbc.core.common.qualitycontrol.genotype.GenotypeQC;
import edu.sysu.pmglab.gbc.core.common.qualitycontrol.genotype.IGenotypeQC;
import edu.sysu.pmglab.gbc.core.common.qualitycontrol.variant.*;

import java.io.IOException;

/**
 * @Data        :2021/02/06
 * @Author      :suranyi
 * @Contact     :suranyi.sysu@gamil.com
 * @Description :构建 GTB 文件，不允许绕过 BuildTake 发起任务
 */

public class BuildTask extends IBuildTask {
    final StringArray inputFileNames;

    /**
     * 位点水平基因型质控器，基因型质控器只有 BuildTask 独有
     */
    GenotypeQC genotypeQC = new GenotypeQC();

    {
        // 初始化 QC 方法
        resetVariantQC();
        resetGenotypeQC();
    }

    /**
     * 构造器
     * @param inputFileName 输入文件名，可以是单个文件或文件夹
     */
    public BuildTask(String inputFileName) throws IOException {
        this(inputFileName, null);
    }

    /**
     * 构造器
     * @param inputFileNames 输入文件名，可以是单个文件或文件夹
     */
    public BuildTask(String[] inputFileNames) throws IOException {
        this(inputFileNames, null);
    }

    /**
     * 构造器
     * @param inputFileName 输入文件名，可以是单个文件或文件夹
     * @param outputFileName 输出文件名，只能是单个文件名
     */
    public BuildTask(String inputFileName, String outputFileName) throws IOException {
        this(new String[]{Assert.NotNull(inputFileName)}, outputFileName);
    }

    /**
     * 构造器
     * @param inputFileNames 输入文件名，可以是单个文件或文件夹
     * @param outputFileName 输出文件名，只能是单个文件名
     */
    public BuildTask(String[] inputFileNames, String outputFileName) throws IOException {
        this(new StringArray(inputFileNames), outputFileName);
    }

    /**
     * 构造器
     * @param inputFileNames 输入文件名，长度不能等于 0，否则是无效的输入
     * @param outputFileName 输出文件名，只能是单个文件名
     */
    public BuildTask(StringArray inputFileNames, String outputFileName) throws IOException {
        // 非空且非长度非 0
        Assert.NotEmpty(inputFileNames);

        // 将其包装为智能列表
        StringArray temps = new StringArray(inputFileNames);
        temps.dropDuplicated();

        // 检验文件是否都存在
        for (String fileName : temps) {
            Assert.NotNull(fileName);
            Assert.that(FileUtils.exists(fileName), fileName + " not found");
        }

        // 清除空文件，并按照文件扩展名进行过滤
        String[] files = ArrayUtils.dropDuplicated(FileUtils.listFilesDeeply(temps));

        if (files.length == 1) {
            this.inputFileNames = new StringArray(files);
        } else {
            // 长度不为 1, 则此时筛选扩展名为 vcf 和 gz 的文件 (传入多个文件或者文件夹时，需要遍历其中的子文件)
            this.inputFileNames = new StringArray(FileUtils.filterByExtension(files, ".vcf", ".gz"));

            Assert.NotEmpty(this.inputFileNames);
        }

        this.outputFileName = outputFileName;
    }

    /**
     * 获取输入文件名
     */
    public StringArray getInputFileNames() {
        return this.inputFileNames;
    }

    /**
     * 获取第 index 个输入文件名
     */
    public String getInputFileName(int index) {
        return this.inputFileNames.get(index);
    }

    /**
     * 获取质控的基因型 DP 阈值 (0 表示不进行 dp 质控)
     */
    public int getGenotypeQualityControlDp() {
        for (IGenotypeQC qc : this.genotypeQC) {
            if (qc instanceof GenotypeDPController) {
                return qc.getMethod();
            }
        }
        return 0;
    }

    /**
     * 获取质控的基因型 GQ 阈值
     */
    public int getGenotypeQualityControlGq() {
        for (IGenotypeQC qc : this.genotypeQC) {
            if (qc instanceof GenotypeGQController) {
                return qc.getMethod();
            }
        }
        return 0;
    }

    /**
     * 获取质控的位点 DP 阈值
     */
    public int getVariantQualityControlDp() {
        for (IVariantQC qc : this.variantQC) {
            if (qc instanceof VariantDPController) {
                return qc.getMethod();
            }
        }
        return 0;
    }

    /**
     * 获取质控的位点 DP 阈值
     */
    public int getVariantQualityControlMq() {
        for (IVariantQC qc : this.variantQC) {
            if (qc instanceof VariantMQController) {
                return qc.getMethod();
            }
        }
        return 0;
    }

    /**
     * 设置质控的基因型 DP 阈值
     * @param genotypeQualityControlDp 质控的 DP 值
     */
    public BuildTask setGenotypeQualityControlDp(int genotypeQualityControlDp) {
        return addGenotypeQC(new GenotypeDPController(genotypeQualityControlDp));
    }

    /**
     * 获取质控的位点 QUAL 阈值
     */
    public int getVariantPhredQualityScore() {
        for (IVariantQC qc : this.variantQC) {
            if (qc instanceof VariantPhredQualityScoreController) {
                return qc.getMethod();
            }
        }
        return 0;
    }

    /**
     * 不进行质量控制
     */
    @Override
    public BuildTask controlDisable() {
        clearVariantQC();
        clearGenotypeQC();
        clearAlleleQC();
        return this;
    }

    /**
     * 设置质控的基因型 GQ 阈值
     * @param genotypeQualityControlGq 质控的 GQ 值
     */
    public BuildTask setGenotypeQualityControlGq(int genotypeQualityControlGq) {
        return addGenotypeQC(new GenotypeGQController(genotypeQualityControlGq));
    }

    /**
     * 设置质控的位点 DP 阈值
     * @param variantQualityControlDp 质控的 GQ 值
     */
    public BuildTask setVariantQualityControlDp(int variantQualityControlDp) {
        return (BuildTask) addVariantQC(new VariantDPController(variantQualityControlDp));
    }

    /**
     * 设置质控的位点 MQ 阈值
     * @param variantQualityControlMq 质控的 MQ 值
     */
    public BuildTask setVariantQualityControlMq(int variantQualityControlMq) {
        return (BuildTask) addVariantQC(new VariantMQController(variantQualityControlMq));
    }

    /**
     * 设置质控的位点 QUAL 阈值
     * @param variantPhredQualityScore 质控的 QUAL 阈值
     */
    public BuildTask setVariantPhredQualityScore(int variantPhredQualityScore) {
        return (BuildTask) addVariantQC(new VariantPhredQualityScoreController(variantPhredQualityScore));
    }


    /**
     * 添加基因型质量控制器
     * @param genotypeQC 控制器
     */
    public BuildTask addGenotypeQC(IGenotypeQC genotypeQC) {
        synchronized (this) {
            this.genotypeQC.add(genotypeQC);
        }
        return this;
    }

    /**
     * 获取基因型质量控制器
     */
    public GenotypeQC getGenotypeQC() {
        return this.genotypeQC;
    }

    /**
     * 清空基因型质量控制器
     */
    public BuildTask clearGenotypeQC() {
        synchronized (this) {
            this.genotypeQC.clear();
        }
        return this;
    }

    /**
     * 重设基因型质量控制器
     */
    public BuildTask resetGenotypeQC() {
        synchronized (this) {
            this.genotypeQC.clear();
            this.genotypeQC.add(new GenotypeGQController());
            this.genotypeQC.add(new GenotypeDPController());
        }
        return this;
    }

    /**
     * 清空位点质量控制器
     */
    @Override
    public BuildTask clearVariantQC() {
        synchronized (this) {
            this.variantQC.clear();
            this.variantQC.add(new VariantAllelesNumController());
        }
        return this;
    }

    /**
     * 重设位点质量控制器
     */
    @Override
    public BuildTask resetVariantQC() {
        synchronized (this) {
            this.variantQC.clear();
            this.variantQC.add(new VariantPhredQualityScoreController());
            this.variantQC.add(new VariantDPController());
            this.variantQC.add(new VariantMQController());
            this.variantQC.add(new VariantAllelesNumController());
        }
        return this;
    }

    /**
     * 打印方法，返回当前任务设置情况
     */
    @Override
    public String toString() {
        return "BuildTask {" +
                "\n\tinputFile(s): " + this.inputFileNames.toString() +
                "\n\toutputFile: " + ((this.outputFileName != null) ? this.outputFileName : "") +
                "\n\tthreads: " + this.threads +
                "\n\tphased: " + this.phased +
                "\n\treordering: " + this.reordering + (this.reordering ? " (" + this.windowSize + " - Accumulated Generating Sequence)" : "") +
                "\n\tblockSize: " + this.blockSize + " (-bs " + this.blockSizeType + ")" +
                "\n\tcompressionLevel: " + this.compressionLevel + " (" + ICompressor.getCompressorName(this.getCompressor()) + ")" +
                (genotypeQC.size() == 0 ? "" : "\n\tgenotypeQC: " + genotypeQC.toString()) +
                "\n\tvariantQC: " + this.variantQC +
                (this.alleleQC.size() == 0 ? "" : "\n\talleleQC: " + this.alleleQC) +
                "\n}";
    }

    /**
     * 运行，并发控制
     */
    public void submit() throws IOException {
        synchronized (this) {
            if (this.outputFileName == null) {
                this.outputFileName = autoGenerateOutputFileName();
            }

            // 输出文件名不能与任何输入文件名重复
            for (String fileName : this.inputFileNames) {
                Assert.that(!fileName.equals(this.outputFileName), IOExceptionOptions.FileOccupiedException, "output file cannot be the same as the input file (" + this.outputFileName + ")");
            }

            // 构建核心任务
            if (this.inputFileNames.size() > 1) {
                BuildKernelMultiFile.submit(this);
            } else {
                BuildKernel.submit(this);
            }
        }
    }

    @Override
    public String autoGenerateOutputFileName() {
        return FileUtils.fixExtension(this.inputFileNames.get(0), this.phased ? ".phased.gtb" : ".unphased.gtb", ".vcf.gz", ".vcf", ".gz");
    }
}
