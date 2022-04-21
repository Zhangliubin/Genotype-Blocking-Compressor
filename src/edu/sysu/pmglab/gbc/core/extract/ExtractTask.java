package edu.sysu.pmglab.gbc.core.extract;

import edu.sysu.pmglab.container.array.StringArray;
import edu.sysu.pmglab.gbc.constant.ChromosomeTags;
import edu.sysu.pmglab.gbc.core.ITask;
import edu.sysu.pmglab.gbc.core.common.qualitycontrol.allele.*;
import edu.sysu.pmglab.gbc.core.gtbcomponent.GTBManager;
import edu.sysu.pmglab.gbc.core.gtbcomponent.GTBRootCache;
import edu.sysu.pmglab.check.Assert;
import edu.sysu.pmglab.check.ioexception.IOExceptionOptions;
import edu.sysu.pmglab.easytools.FileUtils;
import edu.sysu.pmglab.unifyIO.FileStream;
import edu.sysu.pmglab.unifyIO.options.FileOptions;
import edu.sysu.pmglab.unifyIO.partwriter.BGZOutputParam;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * @Data        :2021/03/05
 * @Author      :suranyi
 * @Contact     :suranyi.sysu@gamil.com
 * @Description :解压任务构造器，不允许绕过 ExtractTask 发起任务
 */

public class ExtractTask implements ITask {
    final GTBManager manager;
    String outputFileName;

    int threads = INIT_THREADS;

    boolean phased;
    StringArray subjects;
    boolean hideGenotype = false;
    final AlleleQC alleleQC = new AlleleQC();
    BGZOutputParam outputParam = new BGZOutputParam();

    /**
     * 构造器
     * @param inputFileName 输入文件名，只能是单个文件
     */
    public ExtractTask(String inputFileName) {
        this.manager = GTBRootCache.get(inputFileName);
        this.phased = this.manager.isPhased();
    }

    /**
     * 构造器
     * @param inputFileName 输入文件名，只能是单个文件
     * @param outputFileName 输出文件名，只能是单个文件
     */
    public ExtractTask(String inputFileName, String outputFileName) {
        this.manager = GTBRootCache.get(inputFileName);

        // 根据输出文件名判断
        if (outputFileName != null) {
            this.outputParam = new BGZOutputParam(outputFileName.endsWith(".gz"), this.outputParam.level);
        }
        this.outputFileName = outputFileName;
    }

    /**
     * 设置输出文件名
     * @param outputFileName 输出文件名，只能是单个文件名
     */
    @Override
    public ExtractTask setOutputFileName(String outputFileName) {
        synchronized (this) {
            this.outputFileName = outputFileName;

            if (outputFileName != null) {
                this.outputParam = new BGZOutputParam(outputFileName.endsWith(".gz"), this.outputParam.level);
            }
        }

        return this;
    }

    /**
     * 设置并行线程数
     * @param threads 并行线程数
     */
    @Override
    public ExtractTask setParallel(int threads) {
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
     * 设置向型，默认与文件相同
     */
    public ExtractTask setPhased(boolean phased) {
        synchronized (this) {
            this.phased = phased;
        }

        return this;
    }

    /**
     * 选择提取的样本数据，默认提取所有数据
     * @param subjects 样本序列
     */
    public ExtractTask selectSubjects(String... subjects) {
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
     * @param subjects 添加的样本序列
     */
    public ExtractTask addSubjects(String... subjects) {
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
     * 选择提取的样本数据，默认提取所有数据
     * @param subjectFileName 样本序列文件
     * @param regex 样本名的分隔符，一般为 \n 或 \t
     */
    public ExtractTask selectSubjectsFromFile(String subjectFileName, String regex) throws IOException {
        try (FileStream fs = new FileStream(subjectFileName, FileOptions.CHANNEL_READER)) {
            return selectSubjects(new String(fs.readAll()).split(regex));
        }
    }

    /**
     * 选择提取的样本数据，默认提取所有数据
     * @param subjectFileName 样本序列文件
     */
    public ExtractTask addSubjectsFromFile(String subjectFileName, String regex) throws IOException {
        if (this.subjects == null) {
            return selectSubjectsFromFile(subjectFileName, regex);
        } else {
            try (FileStream fs = new FileStream(subjectFileName, FileOptions.CHANNEL_READER)) {
                return addSubjects(new String(fs.readAll()).split(regex));
            }
        }
    }

    /**
     * 设置输出文件的压缩方式
     * @param param 输出参数 (是否进行压缩、压缩级别)
     */
    public ExtractTask setCompressToBGZF(BGZOutputParam param) {
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
    public ExtractTask setCompressToBGZF(boolean compress, int compressionLevel) {
        synchronized (this) {
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
    public ExtractTask setCompressToBGZF(boolean compress) {
        return setCompressToBGZF(compress, this.outputParam.level);
    }

    /**
     * 设置输出文件的压缩方式
     */
    public ExtractTask setCompressToBGZF() {
        return setCompressToBGZF(true);
    }

    /**
     * 设置过滤方式
     * @param minAc 最小 allele count 计数
     */
    public ExtractTask filterByAC(int minAc) {
        return filterByAC(minAc, AlleleACController.MAX);
    }

    /**
     * 设置过滤方式
     * @param minAc 最小 allele count 计数
     * @param maxAc 最大 allele count 计数
     */
    public ExtractTask filterByAC(int minAc, int maxAc) {
        return addAlleleQC(new AlleleACController(minAc, maxAc));
    }

    /**
     * 设置过滤方式
     * @param minAf 最小 allele frequency
     */
    public ExtractTask filterByAF(double minAf) {
        return filterByAF(minAf, AlleleAFController.MAX);
    }

    /**
     * 设置过滤方式
     * @param minAf 最小 allele frequency
     * @param maxAf 最大 allele frequency
     */
    public ExtractTask filterByAF(double minAf, double maxAf) {
        return addAlleleQC(new AlleleAFController(minAf, maxAf));
    }

    /**
     * 设置过滤方式
     * @param minAn 最小 allele 数
     */
    public ExtractTask filterByAN(int minAn) {
        return addAlleleQC(new AlleleANController(minAn, AlleleANController.MAX));
    }

    /**
     * 设置过滤方式
     * @param minAn 最小 allele 数
     */
    public ExtractTask filterByAN(int minAn, int maxAn) {
        return addAlleleQC(new AlleleANController(minAn, maxAn));
    }

    /**
     * 设置过滤方式 (自定义过滤器)
     * @param filter 自定义过滤器
     */
    public ExtractTask addAlleleQC(IAlleleQC filter) {
        synchronized (this) {
            this.alleleQC.add(filter);
        }
        return this;
    }

    /**
     * 隐藏基因型数据，selectSubjects 是选择指定的样本，而该参数为不打印基因型数据
     */
    public ExtractTask hideGenotype(boolean hideGenotype) {
        synchronized (this) {
            this.hideGenotype = hideGenotype;
        }

        return this;
    }

    /**
     * 隐藏基因型数据
     */
    public ExtractTask hideGenotype() {
        return hideGenotype(true);
    }

    /**
     * 获取设置的过滤器
     */
    public AlleleQC getAlleleQC() {
        return this.alleleQC;
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
     * 获取 样本名序列
     */
    public StringArray getSubjects() {
        return this.subjects;
    }

    /**
     * 获取输出文件名
     */
    @Override
    public String getOutputFileName() {
        return this.outputFileName;
    }

    /**
     * 获取线程数
     */
    @Override
    public int getThreads() {
        return this.threads;
    }

    /**
     * 获取压缩参数
     */
    public BGZOutputParam getOutputParam() {
        return this.outputParam;
    }

    /**
     * 是否有向
     */
    public Boolean isPhased() {
        return this.phased;
    }

    /**
     * 是否隐藏基因型数据
     */
    public boolean isHideGenotype() {
        return this.hideGenotype;
    }

    /**
     * 获取输入文件名
     */
    public String getInputFileName() {
        return this.manager.getFileName();
    }

    /**
     * 运行前参数检查，检查文件路径
     */
    private void runningCheck() throws IOException {
        if (this.outputFileName == null) {
            this.outputFileName = autoGenerateOutputFileName();
        }

        Assert.that(!getInputFileName().equals(this.outputFileName), IOExceptionOptions.FileOccupiedException, "output file cannot be the same as the input file (" + this.outputFileName + ")");

        if (manager.isSuggestToBGZF() && !this.outputParam.toBGZF && !this.hideGenotype) {
            throw new ExtractException("some block contains more than 1GB of text format, so this GTB file can only be decompressed in BGZ format");
        }
    }

    /**
     * 解压指定的多个染色体数据
     * @param chromosomeIndexes 解压的染色体
     */
    public void decompressByChromosome(int... chromosomeIndexes) throws IOException {
        synchronized (this) {
            // 运行前检查
            runningCheck();

            // 构建核心任务
            ExtractKernel.decompressByChromosome(this, chromosomeIndexes);
        }
    }

    /**
     * 解压指定的多个染色体数据
     * @param chromosomes 解压的染色体
     */
    public void decompressByChromosome(String... chromosomes) throws IOException {
        decompressByChromosome(ChromosomeTags.getIndexes(chromosomes));
    }

    /**
     * 解压指定染色体
     * @param chromosomeIndex 染色体编号
     */
    public void decompressByRange(int chromosomeIndex) throws IOException {
        decompressByRange(chromosomeIndex, 0);
    }

    /**
     * 解压指定染色体
     * @param chromosome 染色体编号
     */
    public void decompressByRange(String chromosome) throws IOException {
        decompressByRange(ChromosomeTags.getIndex(chromosome), 0);
    }

    /**
     * 解压指定染色体从 minPos 开始的位点
     * @param chromosomeIndex 染色体编号
     * @param startPos 开始的位点
     */
    public void decompressByRange(int chromosomeIndex, int startPos) throws IOException {
        decompressByRange(chromosomeIndex, startPos, Integer.MAX_VALUE);
    }

    /**
     * 解压指定染色体从 minPos 开始的位点
     * @param chromosome 染色体编号
     * @param startPos 开始的位点
     */
    public void decompressByRange(String chromosome, int startPos) throws IOException {
        decompressByRange(ChromosomeTags.getIndex(chromosome), startPos);
    }

    /**
     * 解压指定染色体从 minPos-endPos 的位点
     * @param chromosomeIndex 染色体编号
     * @param startPos 开始的位点
     * @param endPos 结束的位点
     */
    public void decompressByRange(int chromosomeIndex, int startPos, int endPos) throws IOException {
        synchronized (this) {
            // 运行前检查
            runningCheck();

            // 构建核心任务
            ExtractKernel.decompressByRange(this, chromosomeIndex, startPos, endPos);
        }
    }

    /**
     * 解压指定染色体从 minPos-endPos 的位点
     * @param chromosome 染色体编号
     * @param startPos 开始的位点
     * @param endPos 结束的位点
     */
    public void decompressByRange(String chromosome, int startPos, int endPos) throws IOException {
        decompressByRange(ChromosomeTags.getIndex(chromosome), startPos, endPos);
    }

    /**
     * 按照指定位置解压
     * @param chromosomeIndex 染色体编号
     * @param positions 待解压的位置数据
     */
    public void decompressByPosition(int chromosomeIndex, int[] positions) throws IOException {
        HashMap<Integer, int[]> chromosomeMap = new HashMap<>(1);
        chromosomeMap.put(chromosomeIndex, positions);
        decompressByPosition(chromosomeMap);
    }

    /**
     * 按照指定位置解压
     * @param chromosome 染色体编号
     * @param positions 待解压的位置数据
     */
    public void decompressByPosition(String chromosome, int[] positions) throws IOException {
        decompressByPosition(ChromosomeTags.getIndex(chromosome), positions);
    }

    /**
     * 按照指定位置解压
     * @param chromosomePositions 染色体-位置数据对
     */
    public <ChromosomeType> void decompressByPosition(Map<ChromosomeType, int[]> chromosomePositions) throws IOException {
        synchronized (this) {
            // 运行前检查
            runningCheck();

            // 构建核心任务
            ExtractKernel.decompressByPosition(this, chromosomePositions);
        }
    }

    /**
     * 解压全部数据
     */
    public void decompressAll() throws IOException {
        synchronized (this) {
            // 运行前检查
            runningCheck();

            // 构建核心任务
            ExtractKernel.decompressAll(this);
        }
    }

    /**
     * 按照节点索引解压指定的染色体数据
     * @param nodeIndexes 节点的索引数据
     */
    public <ChromosomeType> void decompressByNodeIndex(Map<ChromosomeType, int[]> nodeIndexes) throws IOException {
        synchronized (this) {
            // 运行前检查
            runningCheck();

            // 构建核心任务
            ExtractKernel.decompressByNodeIndex(this, nodeIndexes);
        }
    }

    /**
     * 按照节点索引解压指定的染色体数据
     * @param chromosomeIndex 染色体编号
     * @param nodeIndexes 待解压的节点索引
     */
    public void decompressByNodeIndex(int chromosomeIndex, int[] nodeIndexes) throws IOException {
        HashMap<Integer, int[]> chromosomePositions = new HashMap<>(1);
        chromosomePositions.put(chromosomeIndex, nodeIndexes);
        decompressByNodeIndex(chromosomePositions);
    }

    /**
     * 按照节点索引解压指定的染色体数据
     * @param chromosome 染色体编号
     * @param nodeIndexes 待解压的位置数据
     */
    public void decompressByNodeIndex(String chromosome, int[] nodeIndexes) throws IOException {
        HashMap<Integer, int[]> chromosomePositions = new HashMap<>(1);
        chromosomePositions.put(ChromosomeTags.getIndex(chromosome), nodeIndexes);
        decompressByNodeIndex(chromosomePositions);
    }

    /**
     * 传入存放的位置数据文件进行解压
     * @param fileName 位置数据文件
     * @param groupRegex 不同组位置数据的分隔符
     * @param positionRegex 染色体-位置 之间的分割符
     */
    public void decompressByPositionFromFile(String fileName, String groupRegex, String positionRegex) throws IOException {
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

            decompressByPosition(realChromosomePositions);
        }
    }

    @Override
    public String autoGenerateOutputFileName() {
        String phased = this.phased ? ".phased" : ".unphased";
        String toBgzf = this.outputParam.toBGZF ? ".vcf.gz" : ".vcf";
        String extension = this.hideGenotype ? ".site" + toBgzf : phased + toBgzf;

        return FileUtils.fixExtension(getInputFileName(), extension, ".vcf.gz.gtb", ".vcf.gtb", ".gz.gtb", ".phased.gtb", ".unphased.gtb", ".gtb");
    }

    @Override
    public String toString() {
        String subjectInfo;
        if (this.subjects == null) {
            subjectInfo = "<all subjects>";
        } else {
            subjectInfo = this.subjects.toString(5) + " (" + this.subjects.size() + " subjects in total)";
        }
        return "ExtractTask {" +
                "\n\tinputFile: " + getInputFileName() +
                "\n\toutputFile: " + (this.outputFileName == null ? "" : this.outputFileName) +
                "\n\tthreads: " + this.threads +
                "\n\t" + this.outputParam +
                "\n\thideGenotype: " + this.hideGenotype +
                "\n\tphased: " + this.phased +
                "\n\tsubjects: " + subjectInfo +
                (this.alleleQC.size() == 0 ? "" : "\n\tfilter: " + this.alleleQC) +
                "\n}";
    }
}
