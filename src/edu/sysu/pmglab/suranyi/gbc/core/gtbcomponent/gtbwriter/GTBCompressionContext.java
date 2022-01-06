package edu.sysu.pmglab.suranyi.gbc.core.gtbcomponent.gtbwriter;

import edu.sysu.pmglab.suranyi.check.Assert;
import edu.sysu.pmglab.suranyi.compressor.ICompressor;
import edu.sysu.pmglab.suranyi.container.Pair;
import edu.sysu.pmglab.suranyi.container.ShareCache;
import edu.sysu.pmglab.suranyi.container.VolumeByteStream;
import edu.sysu.pmglab.suranyi.gbc.coder.encoder.MBEGEncoder;
import edu.sysu.pmglab.suranyi.gbc.core.build.IBuildTask;
import edu.sysu.pmglab.suranyi.gbc.core.common.combiner.ICodeCombiner;
import edu.sysu.pmglab.suranyi.gbc.core.common.switcher.ISwitcher;
import edu.sysu.pmglab.suranyi.gbc.core.gtbcomponent.GTBNode;

import java.io.IOException;

/**
 * @author suranyi
 * @description GTB 压缩方法的上下文环境
 */

class GTBCompressionContext {
    /**
     * 排序合并后的等位基因容器、位置容器、等位基因
     * 该容器可复用，支持从外部导入，也可以由本类自动判断生成
     */
    final VolumeByteStream unCompressedCache;

    /**
     * 行特征交换器
     */
    final ISwitcher switcher;

    /**
     * 编码组合器
     */
    final ICodeCombiner codeCombiner;

    /**
     * 压缩器与编码器
     */
    private final ICompressor compressor;
    private final MBEGEncoder groupEncoder;

    /**
     * @param task 压缩任务
     * @param validSubjectNum 有效样本个数
     * @param caches 传入一个长度大于或等于 2 的缓冲区，缓冲区的第一部分为压缩器缓冲区，第二部分为合并数据流缓冲区
     */
    public GTBCompressionContext(IBuildTask task, int validSubjectNum, ShareCache caches) {
        Assert.that(caches.size() >= 2);

        // 将 BEG-EncodedCache 作为压缩输出数据的缓冲区
        this.compressor = ICompressor.getInstance(task.getCompressor(), task.getCompressionLevel(), caches.getCache(0));

        // 创建行特征交换器
        this.switcher = ISwitcher.getInstance(task.isReordering());

        // 创建编码组合器
        this.codeCombiner = ICodeCombiner.getInstance(task.isPhased(), validSubjectNum);

        // 排序之后的合并流，大型复用容器。缓冲区大小，默认设定为 BEG 阵列的一半，该设计可以容纳 1/4 的多等位基因位点，通常情况下他不会发生扩容。
        this.unCompressedCache = caches.getCache(1);

        // 组合编码器
        this.groupEncoder = MBEGEncoder.getEncoder(task.isPhased());
    }

    /**
     * 处理输入的 block
     * @param block 待处理的 block
     */
    public Pair<GTBNode, VolumeByteStream> process(UncompressedBlock block) throws IOException {
        this.compressor.reset();
        this.unCompressedCache.reset();
        int variantsNum = block.seek;

        // 最小最大位点, 位点种类计数
        int minPos = block.variants[0].position;
        int maxPos = block.variants[variantsNum - 1].position;
        short[] subBlockVariantNum = new short[2];

        for (int i = 0; i < variantsNum; i++) {
            if (block.variants[i].position < minPos) {
                minPos = block.variants[i].position;
            }

            if (block.variants[i].position > maxPos) {
                maxPos = block.variants[i].position;
            }

            subBlockVariantNum[block.variants[i].encoderIndex]++;
        }

        // 特征交换
        this.switcher.switchingRow(this.groupEncoder, block.variants, variantsNum, block.encodedCache.getCache());

        // 处理基因型数据并记录压缩流大小
        this.codeCombiner.process(this.groupEncoder, block.variants, subBlockVariantNum, block.encodedCache.getCache(), this.unCompressedCache);

        // 基因型数据未压缩前大小
        int originMBEGsSize = this.unCompressedCache.size();

        // 压缩基因型数据
        int compressedGenotypeSize = compress(this.unCompressedCache, this.compressor);

        // 压缩 position 数据
        for (int i = 0; i < variantsNum; i++) {
            block.variants[i].writePosTo(this.unCompressedCache);
        }
        int originPosSize = this.unCompressedCache.size();
        int compressedPosSize = compress(this.unCompressedCache, this.compressor);

        /* allele 处必须非常小心，有可能超过缓冲容器大小 */
        int originAllelesSize = check(variantsNum, block);
        for (int i = 0; i < variantsNum; i++) {
            block.variants[i].writeAlleleTo(this.unCompressedCache);
        }

        // 等位基因大小
        int compressedAlleleSize = compress(this.unCompressedCache, this.compressor);

        // 送出压缩完成的数据
        return new Pair<>(new GTBNode(block.chromosomeIndex, minPos, maxPos, 0, compressedGenotypeSize, compressedPosSize, compressedAlleleSize,
                originMBEGsSize, Math.max(originAllelesSize, originPosSize), subBlockVariantNum),
                this.compressor.getCache());
    }

    int check(int variantsNum, UncompressedBlock block) {
        int requestSize = 0;
        for (int i = 0; i < variantsNum; i++) {
            requestSize += block.variants[i].alleleSize();
        }

        if (this.unCompressedCache.getCapacity() < requestSize) {
            this.unCompressedCache.expansionTo(Math.min(requestSize << 1, Integer.MAX_VALUE - 2));
        }
        return requestSize;
    }

    int compress(VolumeByteStream src, ICompressor compressor) throws IOException {
        int blockSize = compressor.compress(src);
        src.reset();
        return blockSize;
    }

    /**
     * 关闭压缩机，清除资源
     */
    public void close() {
        this.compressor.close();
    }
}