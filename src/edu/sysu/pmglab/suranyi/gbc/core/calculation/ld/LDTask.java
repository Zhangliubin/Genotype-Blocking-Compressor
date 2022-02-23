package edu.sysu.pmglab.suranyi.gbc.core.calculation.ld;

import edu.sysu.pmglab.suranyi.check.Assert;
import edu.sysu.pmglab.suranyi.check.ioexception.IOExceptionOptions;
import edu.sysu.pmglab.suranyi.container.SmartList;
import edu.sysu.pmglab.suranyi.easytools.FileUtils;
import edu.sysu.pmglab.suranyi.gbc.constant.ChromosomeInfo;
import edu.sysu.pmglab.suranyi.gbc.core.ITask;
import edu.sysu.pmglab.suranyi.gbc.core.gtbcomponent.GTBManager;
import edu.sysu.pmglab.suranyi.gbc.core.gtbcomponent.GTBRootCache;
import edu.sysu.pmglab.suranyi.unifyIO.FileStream;
import edu.sysu.pmglab.suranyi.unifyIO.options.FileOptions;
import edu.sysu.pmglab.suranyi.unifyIO.partwriter.BGZOutputParam;

import java.io.IOException;
import java.util.Collection;

/**
 * @author suranyi
 * @description LD 计算
 */

public class LDTask implements ITask {
    String outputFileName;

    /**
     * 计算的窗口大小，bp
     */
    int windowSizeBp = DEFAULT_WINDOW_SIZE_BP;

    /**
     * MAF: minor allele frequency
     */
    double maf = DEFAULT_MAF;

    /**
     * 并行线程数
     */
    int threads = INIT_THREADS;

    /**
     * 过滤最小 LD^2
     */
    double minR2 = DEFAULT_MIN_R2;

    /**
     * 核心文件管理器
     */
    GTBManager manager;

    /**
     * LD 模型, 根据文件向型自动设置
     */
    ILDModel LdModel;

    /**
     * 筛选样本名列表
     */
    SmartList<String> subjects;

    /**
     * BGZ 输出格式参数
     */
    BGZOutputParam outputParam = new BGZOutputParam();

    /**
     * 默认值
     */
    public static final int DEFAULT_WINDOW_SIZE_BP = 10000;
    public static final double DEFAULT_MAF = 0.05;
    public static final double DEFAULT_MIN_R2 = 0.2;

    public LDTask(String gtbFileName) {
        this(gtbFileName, null);
    }

    public LDTask(String gtbFileName, String outputFileName) {
        this(GTBRootCache.get(gtbFileName), outputFileName);
    }

    public LDTask(GTBManager manager, String outputFileName) {
        this.manager = manager;

        // 根据向型自动选择 LD 模型
        if (this.manager.isPhased()) {
            this.LdModel = ILDModel.HAPLOTYPE_LD;
        } else {
            this.LdModel = ILDModel.GENOTYPE_LD;
        }

        // 根据输出文件名判断
        if (outputFileName != null) {
            this.outputParam = new BGZOutputParam(outputFileName.endsWith(".gz"), this.outputParam.level);
        }
        this.outputFileName = outputFileName;
    }

    @Override
    public LDTask setOutputFileName(String outputFileName) {
        synchronized (this) {
            this.outputFileName = outputFileName;

            if (outputFileName != null) {
                this.outputParam = new BGZOutputParam(outputFileName.endsWith(".gz"), this.outputParam.level);
            }
        }
        return this;
    }

    /**
     * 选择提取的样本数据，默认提取所有数据
     * @param subjects 样本序列
     */
    public LDTask selectSubjects(String... subjects) {
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
    public LDTask addSubjects(String... subjects) {
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
     * 选择提取的样本数据，默认提取所有数据
     * @param subjectFileName 样本序列文件
     * @param regex 样本名的分隔符，一般为 \n 或 \t
     */
    public LDTask selectSubjectsFromFile(String subjectFileName, String regex) throws IOException {
        try (FileStream fs = new FileStream(subjectFileName, FileOptions.CHANNEL_READER)) {
            return selectSubjects(new String(fs.readAll()).split(regex));
        }
    }

    /**
     * 选择提取的样本数据，默认提取所有数据
     * @param subjectFileName 样本序列文件
     */
    public LDTask addSubjectsFromFile(String subjectFileName, String regex) throws IOException {
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
     * 设置计算的 LD 模型，默认根据文件向型自动设置
     * @param ldModel LD 模型
     */
    public LDTask setLdModel(ILDModel ldModel) {
        synchronized (this) {
            this.LdModel = (ldModel == null) ? (this.manager.isPhased() ? ILDModel.HAPLOTYPE_LD : ILDModel.GENOTYPE_LD) : ldModel;
        }
        return this;
    }

    /**
     * 计算的窗口大小，最大覆盖的 bp 长度
     * @param windowSizeBp 最大 Bp 长度
     */
    public LDTask setWindowSizeBp(int windowSizeBp) {
        synchronized (this) {
            if (windowSizeBp == -1) {
                this.windowSizeBp = DEFAULT_WINDOW_SIZE_BP;
            } else {
                Assert.valueRange(windowSizeBp, 1, Integer.MAX_VALUE);
                this.windowSizeBp = windowSizeBp;
            }
        }
        return this;
    }

    /**
     * 设置次级等位基因频率
     * @param maf 次级等位基因频率值
     */
    public LDTask setMaf(double maf) {
        synchronized (this) {
            if (maf == -1) {
                this.maf = DEFAULT_MAF;
            } else {
                Assert.valueRange(maf, 0.0, 1.0);
                this.maf = maf;
            }
        }
        return this;
    }

    /**
     * 设置输出的最小 R^2
     * @param minR2 最小 R^2
     */
    public LDTask setMinR2(double minR2) {
        synchronized (this) {
            if (minR2 == -1) {
                this.minR2 = DEFAULT_MIN_R2;
            } else {
                Assert.valueRange(minR2, 0.0, 1.0);
                this.minR2 = minR2;
            }
        }
        return this;
    }

    /**
     * 设置并行线程数
     * @param threads 并行线程数
     */
    @Override
    public LDTask setParallel(int threads) {
        synchronized (this) {
            if (threads == -1) {
                this.threads = INIT_THREADS;
            } else {
                Assert.valueRange(threads, 1, AVAILABLE_PROCESSORS);
                this.threads = threads;
            }
        }

        return this;
    }

    /**
     * 设置输出文件的压缩方式
     */
    public LDTask setCompressToBGZF(BGZOutputParam param) {
        synchronized (this) {
            if (param != null) {
                this.outputParam = param;
            } else {
                if (this.outputFileName != null) {
                    this.outputParam = new BGZOutputParam(outputFileName.endsWith(".gz"), this.outputParam.level);
                } else {
                    // 回到默认值
                    this.outputParam = new BGZOutputParam();
                }
            }
        }
        return this;
    }

    /**
     * 设置输出文件的压缩方式
     * @param compress 是否进行压缩
     * @param compressionLevel 压缩级别
     */
    public LDTask setCompressToBGZF(boolean compress, int compressionLevel) {
        synchronized (this) {
            if (compressionLevel == -1) {
                compressionLevel = BGZOutputParam.DEFAULT_LEVEL;
            }

            if (compress != this.outputParam.toBGZF || compressionLevel != this.outputParam.level) {
                this.outputParam = new BGZOutputParam(compress, compressionLevel);
            }
        }

        return this;
    }

    /**
     * 设置输出文件的压缩方式
     * @param compress 是否进行压缩
     */
    public LDTask setCompressToBGZF(boolean compress) {
        return setCompressToBGZF(compress, this.outputParam.level);
    }

    /**
     * 设置输出文件的压缩方式
     */
    public LDTask setCompressToBGZF() {
        return setCompressToBGZF(true);
    }

    /**
     * 获取窗口大小
     */
    public int getWindowSizeBp() {
        return this.windowSizeBp;
    }

    /**
     * 获取次级等位基因频率阈值
     */
    public double getMAF() {
        return this.maf;
    }

    /**
     * 获取并行线程数
     */
    @Override
    public int getThreads() {
        return this.threads;
    }

    /**
     * 获取最小 R^2
     */
    public double getMinR2() {
        return this.minR2;
    }

    /**
     * 获取文件管理器
     */
    public GTBManager getManager() {
        return this.manager;
    }

    /**
     * 获取指定的 LD 模型
     */
    public ILDModel getLdModel() {
        return this.LdModel;
    }

    /**
     * 获取输出文件名
     */
    @Override
    public String getOutputFileName() {
        return this.outputFileName;
    }

    /**
     * 获取压缩类型
     */
    public boolean isCompressToBGZF() {
        return this.outputParam.toBGZF;
    }

    /**
     * 获取压缩类型
     */
    public int getCompressionLevel() {
        return this.outputParam.level;
    }

    /**
     * 获取压缩信息
     */
    public BGZOutputParam getOutputParam() {
        return this.outputParam;
    }

    /**
     * 提交 LD 计算，输出到文件
     */
    public void submit() throws IOException {
        submit(this.manager.getChromosomeList());
    }

    /**
     * 提交 LD 计算，输出到文件
     * 计算指定的 chromosome 染色体
     */
    public void submit(int... chromosomes) throws IOException {
        synchronized (this) {
            if (this.outputFileName == null) {
                this.outputFileName = autoGenerateOutputFileName();
            }

            Assert.that(!this.outputFileName.equals(this.manager.getFileName()), IOExceptionOptions.FileOccupiedException, "inputFileName cannot be the same as outputFileName");

            // 构建核心任务
            LDKernel.calculate(this, chromosomes);
        }
    }

    /**
     * 提交 LD 计算，输出到文件
     * 计算指定的 chromosome 染色体
     */
    public void submit(String... chromosomes) throws IOException {
        submit(ChromosomeInfo.getIndexes(chromosomes));
    }

    /**
     * 提交 LD 计算，输出到文件
     * 计算指定的 chromosome 染色体
     */
    public <ChromosomeType> void submit(Collection<ChromosomeType> chromosomes) throws IOException {
        submit(ChromosomeInfo.identifyChromosome(chromosomes));
    }


    /**
     * 提交 LD 计算，输出到文件
     * 计算指定的 chromosome 染色体，从 startPos 到 endPos 范围的位点对
     */
    public void submit(int chromosomeIndex, int startPos) throws IOException {
        submit(chromosomeIndex, startPos, Integer.MAX_VALUE);
    }

    /**
     * 提交 LD 计算，输出到文件
     * 计算指定的 chromosome 染色体，从 startPos 到 endPos 范围的位点对
     */
    public void submit(String chromosome, int startPos) throws IOException {
        submit(chromosome, startPos, Integer.MAX_VALUE);
    }

    /**
     * 提交 LD 计算，输出到文件
     * 计算指定的 chromosome 染色体，从 startPos 到 endPos 范围的位点对
     */
    public void submit(String chromosome, int startPos, int endPos) throws IOException {
        submit(ChromosomeInfo.getIndex(chromosome), startPos, endPos);
    }

    /**
     * 提交 LD 计算，输出到文件
     * 计算指定的 chromosome 染色体，从 startPos 到 endPos 范围的位点对
     */
    public void submit(int chromosomeIndex, int startPos, int endPos) throws IOException {
        synchronized (this) {
            if (this.outputFileName == null) {
                this.outputFileName = autoGenerateOutputFileName();
            }

            Assert.that(!this.outputFileName.equals(this.manager.getFileName()), IOExceptionOptions.FileOccupiedException, "inputFileName cannot be the same as outputFileName");

            // 构建核心任务
            LDKernel.calculate(this, chromosomeIndex, startPos, endPos);
        }
    }

    @Override
    public String toString() {
        String subjectInfo;
        if (this.subjects == null) {
            subjectInfo = "<all subjects>";
        } else {
            subjectInfo = this.subjects.toString(5) + " (" + this.subjects.size() + " subjects in total)";
        }

        return "LDTask {" +
                "\n\tinputFile: " + this.manager.getFileName() +
                "\n\toutputFile: " + (this.outputFileName == null ? "" : this.outputFileName) +
                "\n\tthreads: " + this.threads +
                "\n\t" + this.outputParam +
                "\n\tsubjects: " + subjectInfo +
                "\n\tLD Model: " + LdModel +
                "\n\tfilter: MAF >= " + String.format("%g", maf) + ", R^2 >= " + String.format("%g", minR2) +
                "\n\twindow size: " + windowSizeBp + " bp" +
                "\n}";
    }

    @Override
    public String autoGenerateOutputFileName() {
        String modelName;
        if (this.LdModel instanceof GenotypeLD) {
            modelName = GenotypeLD.GENOTYPE_LD.getExtension();
        } else if (this.LdModel instanceof HaplotypeLD) {
            modelName = HaplotypeLD.HAPLOTYPE_LD.getExtension();
        } else {
            throw new UnsupportedOperationException(this.LdModel.toString());
        }
        return FileUtils.fixExtension(this.manager.getFileName(), this.outputParam.toBGZF ? modelName + ".gz" : modelName, ".vcf.gz.gtb", ".vcf.gtb", ".gz.gtb", ".gtb");
    }
}