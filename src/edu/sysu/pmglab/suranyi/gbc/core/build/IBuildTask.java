package edu.sysu.pmglab.suranyi.gbc.core.build;

import edu.sysu.pmglab.suranyi.check.Assert;
import edu.sysu.pmglab.suranyi.compressor.ICompressor;
import edu.sysu.pmglab.suranyi.easytools.ValueUtils;
import edu.sysu.pmglab.suranyi.gbc.coder.CoderConfig;
import edu.sysu.pmglab.suranyi.gbc.core.ITask;
import edu.sysu.pmglab.suranyi.gbc.core.common.qualitycontrol.allele.*;
import edu.sysu.pmglab.suranyi.gbc.core.common.qualitycontrol.variant.IVariantQC;
import edu.sysu.pmglab.suranyi.gbc.core.common.qualitycontrol.variant.VariantAllelesNumController;
import edu.sysu.pmglab.suranyi.gbc.core.common.qualitycontrol.variant.VariantQC;
import edu.sysu.pmglab.suranyi.gbc.core.common.switcher.ISwitcher;
import edu.sysu.pmglab.suranyi.gbc.core.gtbcomponent.FileBaseInfoManager;
import edu.sysu.pmglab.suranyi.unifyIO.FileStream;
import edu.sysu.pmglab.suranyi.unifyIO.options.FileOptions;

import java.io.IOException;

/**
 * @Data        :2021/06/06
 * @Author      :suranyi
 * @Contact     :suranyi.sysu@gamil.com
 * @Description :构建模式抽象接口
 */

public abstract class IBuildTask implements ITask {
    /**
     * 任务的输出文件名
     */
    String outputFileName;

    /**
     * 设置 GTB 的参数
     */
    boolean phased = CoderConfig.DEFAULT_PHASED_STATUS;
    boolean reordering = ISwitcher.DEFAULT_ENABLE;
    int windowSize = ISwitcher.DEFAULT_SIZE;
    int threads = INIT_THREADS;
    int compressor = ICompressor.DEFAULT;
    int compressionLevel = ICompressor.getDefaultCompressionLevel(this.compressor);
    int blockSizeType = BlockSizeParameter.DEFAULT_BLOCK_SIZE_TYPE;
    int blockSize = BlockSizeParameter.DEFAULT_BLOCK_SIZE;

    final AlleleQC alleleQC = new AlleleQC();
    final VariantQC variantQC = new VariantQC();

    @Override
    public String getOutputFileName() {
        return this.outputFileName;
    }

    /**
     * 获取当前任务是否将文件编码为有向数据
     */
    public boolean isPhased() {
        return this.phased;
    }

    /**
     * 获取当前任务是否要对位点进行重排列
     */
    public boolean isReordering() {
        return this.reordering;
    }

    /**
     * 获取排列窗口大小
     */
    public int getWindowSize() {
        return this.windowSize;
    }

    /**
     * 获取并行的线程数量
     */
    @Override
    public int getThreads() {
        return this.threads;
    }

    /**
     * 获取压缩器，0 代表默认的快速的 zstd，1 代表高压缩比的 lzma
     */
    public int getCompressor() {
        return this.compressor;
    }

    /**
     * 获取 ZSTD 压缩器压缩参数
     */
    public int getCompressionLevel() {
        return this.compressionLevel;
    }

    /**
     * 获取块大小类型
     */
    public int getBlockSizeType() {
        return this.blockSizeType;
    }

    /**
     * 获取块大小
     */
    public int getBlockSize() {
        return this.blockSize;
    }

    /**
     * 获取等位基因过滤器
     */
    public AlleleQC getAlleleQC() {
        return this.alleleQC;
    }

    /**
     * 获取位点质量控制器
     */
    public VariantQC getVariantQC() {
        return this.variantQC;
    }

    @Override
    public IBuildTask setOutputFileName(String outputFileName) {
        synchronized (this) {
            this.outputFileName = outputFileName;
        }

        return this;
    }

    /**
     * 设置基因型向型，将作用于 MBEG 编码器
     * @param phased 向型
     */
    public IBuildTask setPhased(boolean phased) {
        synchronized (this) {
            this.phased = phased;
        }

        return this;
    }

    /**
     * 设置是否使用重排列算法，能够显著 (> 10%) 提高压缩比
     * @param reordering 使用重排列算法
     */
    public IBuildTask setReordering(boolean reordering) {
        synchronized (this) {
            this.reordering = reordering;
        }

        return this;
    }

    /**
     * 设置排列窗口大小
     * @param windowSize 窗口大小
     */
    public IBuildTask setWindowSize(int windowSize) {
        synchronized (this) {
            Assert.valueRange(windowSize, ISwitcher.MIN, ISwitcher.MAX);
            this.windowSize = windowSize;
        }

        return this;
    }

    /**
     * 设置并行线程数
     * @param threads 并行线程数
     */
    @Override
    public IBuildTask setParallel(int threads) {
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
     * 设置压缩器
     * @param compressorIndex 压缩器索引
     */
    public IBuildTask setCompressor(int compressorIndex) {
        synchronized (this) {
            this.compressor = compressorIndex;
            this.compressionLevel = ICompressor.getDefaultCompressionLevel(compressorIndex);
        }

        return this;
    }

    /**
     * 设置压缩器
     * @param compressorIndex 压缩器索引
     * @param compressionLevel 压缩参数
     */
    public IBuildTask setCompressor(int compressorIndex, int compressionLevel) {
        // -1 表示自动设置
        if (compressionLevel == -1) {
            return setCompressor(compressorIndex);
        }

        synchronized (this) {
            Assert.valueRange(compressionLevel, ICompressor.getMinCompressionLevel(compressorIndex), ICompressor.getMaxCompressionLevel(compressorIndex));

            this.compressor = compressorIndex;
            this.compressionLevel = compressionLevel;
        }

        return this;
    }

    /**
     * 设置压缩器
     * @param compressorName 压缩器名
     */
    public IBuildTask setCompressor(String compressorName) {
        return setCompressor(ICompressor.getCompressorIndex(compressorName));
    }

    /**
     * 设置压缩器
     * @param compressorName 压缩器名
     * @param compressionLevel 压缩参数
     */
    public IBuildTask setCompressor(String compressorName, int compressionLevel) {
        return setCompressor(ICompressor.getCompressorIndex(compressorName), compressionLevel);
    }

    /**
     * 设置块大小参数
     * @param blockSizeType 块参数类型
     */
    public IBuildTask setBlockSizeType(int blockSizeType) {
        synchronized (this) {
            if (blockSizeType == -1) {
                this.blockSizeType = -1;
                this.blockSize = -1;
            } else {
                Assert.valueRange(blockSizeType, BlockSizeParameter.MIN_BLOCK_SIZE_TYPE, BlockSizeParameter.MAX_BLOCK_SIZE_TYPE);
                this.blockSize = BlockSizeParameter.getBlockSize(blockSizeType);
            }
        }

        return this;
    }

    /**
     * 设置过滤方式
     * @param minAc 最小 allele count 计数
     */
    public IBuildTask filterByAC(int minAc) {
        return filterByAC(minAc, AlleleACController.MAX);
    }

    /**
     * 设置过滤方式
     * @param minAc 最小 allele count 计数
     * @param maxAc 最大 allele count 计数
     */
    public IBuildTask filterByAC(int minAc, int maxAc) {
        return addAlleleQC(new AlleleACController(minAc, maxAc));
    }

    /**
     * 设置过滤方式
     * @param minAf 最小 allele frequency
     */
    public IBuildTask filterByAF(double minAf) {
        return filterByAF(minAf, AlleleAFController.MAX);
    }

    /**
     * 设置过滤方式
     * @param minAf 最小 allele frequency
     * @param maxAf 最大 allele frequency
     */
    public IBuildTask filterByAF(double minAf, double maxAf) {
        return addAlleleQC(new AlleleAFController(minAf, maxAf));
    }

    /**
     * 设置过滤方式
     * @param minAn 最小 allele 数
     */
    public IBuildTask filterByAN(int minAn) {
        return addAlleleQC(new AlleleANController(minAn, AlleleANController.MAX));
    }

    /**
     * 设置过滤方式
     * @param minAn 最小 allele 数
     */
    public IBuildTask filterByAN(int minAn, int maxAn) {
        return addAlleleQC(new AlleleANController(minAn, maxAn));
    }

    /**
     * 添加过滤器
     * @param filter 过滤器
     */
    public IBuildTask addAlleleQC(IAlleleQC filter) {
        synchronized (this) {
            this.alleleQC.add(filter);
        }
        return this;
    }

    /**
     * 清空过滤器
     */
    public IBuildTask clearAlleleQC() {
        synchronized (this) {
            this.alleleQC.clear();
        }
        return this;
    }

    /**
     * 重设过滤器
     */
    public IBuildTask resetAlleleQC() {
        synchronized (this) {
            this.alleleQC.clear();
        }
        return this;
    }

    /**
     * 添加位点质量控制器
     * @param variantQC 控制器
     */
    public IBuildTask addVariantQC(IVariantQC variantQC) {
        synchronized (this) {
            this.variantQC.add(variantQC);
        }
        return this;
    }

    /**
     * 重设位点质量控制器
     */
    public IBuildTask resetVariantQC() {
        synchronized (this) {
            this.variantQC.clear();
        }
        return this;
    }

    /**
     * 清空位点质量控制器
     */
    public IBuildTask clearVariantQC() {
        synchronized (this) {
            this.variantQC.clear();
        }
        return this;
    }

    /**
     * 设置质控的位点等位基因最大个数
     * @param variantQualityControlAllelesNum 质控的等位基因最大个数
     */
    public IBuildTask setVariantQualityControlAllelesNum(int variantQualityControlAllelesNum) {
        return addVariantQC(new VariantAllelesNumController(variantQualityControlAllelesNum));
    }

    /**
     * 不进行质量控制
     */
    public IBuildTask controlDisable() {
        clearVariantQC();
        clearAlleleQC();
        return this;
    }

    /**
     * 从现有的 GTB 文件中加载参数
     * 请注意，此处没有验证完整的 GTB 文件
     */
    public IBuildTask readyParas(String gtbFile) throws IOException {
        if (gtbFile != null) {
            synchronized (this) {
                try (FileStream params = new FileStream(gtbFile, FileOptions.CHANNEL_READER)) {
                    FileBaseInfoManager baseInfo = new FileBaseInfoManager(ValueUtils.byteArray2ShortValue(params.read(2)));

                    this.phased = baseInfo.isPhased();
                    this.compressor = baseInfo.getCompressorIndex();
                    this.blockSizeType = baseInfo.getBlockSizeType();
                    this.blockSize = baseInfo.getBlockSize();
                    this.compressionLevel = baseInfo.getCompressionLevel();
                }
            }
        }

        return this;
    }

    /**
     * 提交任务
     */
    public abstract void submit() throws IOException;
}