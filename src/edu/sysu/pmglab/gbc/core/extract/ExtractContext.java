package edu.sysu.pmglab.gbc.core.extract;

import edu.sysu.pmglab.compressor.IDecompressor;
import edu.sysu.pmglab.container.ShareCache;
import edu.sysu.pmglab.easytools.ByteCode;
import edu.sysu.pmglab.gbc.core.gtbcomponent.GTBManager;
import edu.sysu.pmglab.gbc.core.gtbcomponent.GTBNode;
import edu.sysu.pmglab.unifyIO.FileStream;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * @Data        :2021/03/08
 * @Author      :suranyi
 * @Contact     :suranyi.sysu@gamil.com
 * @Description :Extract 模式的上下文环境，用于复用数据池，提高并行性能
 */

class ExtractContext {
    /**
     * 带缓存数据的 ZSTD 解压器
     */
    final IDecompressor decompressor;

    /**
     * 变异位点列表
     */
    final TaskVariant[] tasks;
    final TaskVariant[] realTasks;

    /**
     * 未解压数据暂存缓冲区、解压后数据暂存缓冲区
     */
    final ShareCache globalCache;

    /**
     * 持续打开的 gtb 文件
     */
    final FileStream gtbFile;

    /**
     * 重构器
     */
    final IVCFFormatter rebuild;

    /**
     * 标准解压缩
     * @param gtbManager 当前任务对应的 gtb 文件
     */
    ExtractContext(final GTBManager gtbManager, final IVCFFormatter rebuild, ShareCache globalCache) throws IOException {
        // 设置文件重构器
        this.rebuild = rebuild;

        //this.globalCache.getCache(0), this.globalCache.getCache(1), this.globalCache.getCache(2)
        this.globalCache = globalCache;

        // 解压器及缓冲区
        this.decompressor = gtbManager.getDecompressorInstance();

        // 变异位点列表及实位点列表
        this.tasks = new TaskVariant[gtbManager.getBlockSize()];
        this.realTasks = new TaskVariant[gtbManager.getBlockSize()];
        for (int i = 0; i < tasks.length; i++) {
            this.tasks[i] = new TaskVariant();
        }

        // 打开 gtb 文件
        this.gtbFile = gtbManager.getFileStream();
    }

    /**
     * 绑定任务节点
     */
    ByteBuffer process(TaskGTBNode block) throws IOException {
        // 解压的任务节点
        GTBNode node = block.node;

        // 校验临时缓冲区大小
        this.globalCache.getCache(0).makeSureCapacity(node.compressedAlleleSize, node.compressedGenotypesSize, node.compressedPosSize);

        /* 读取位置数据并解压 */
        this.globalCache.getCache(0).reset();
        this.globalCache.getCache(2).reset();
        gtbFile.seek(node.blockSeek + node.compressedGenotypesSize);
        gtbFile.read(this.globalCache.getCache(0), node.compressedPosSize);
        decompressor.decompress(this.globalCache.getCache(0), this.globalCache.getCache(2));

        /* 设置位置数据、当前索引 */
        // 实际的任务数量
        int taskNums = 0;
        if (block.taskType == 0) {
            // 解压所有数据
            taskNums = node.numOfVariants();
            for (int i = 0; i < taskNums; i++) {
                this.tasks[i].setPosition(this.globalCache.getCache(2).cacheOf(i << 2), this.globalCache.getCache(2).cacheOf(1 + (i << 2)),
                        this.globalCache.getCache(2).cacheOf(2 + (i << 2)), this.globalCache.getCache(2).cacheOf(3 + (i << 2)))
                        .setIndex(i)
                        .setDecoderIndex(i < node.subBlockVariantNum[0] ? 0 : 1);
                this.realTasks[i] = this.tasks[i].setPositionInfo();
            }
        } else if (block.taskType == 1) {
            // 解压指定范围数据
            for (int i = 0; i < node.numOfVariants(); i++) {
                this.tasks[i].setPosition(this.globalCache.getCache(2).cacheOf(i << 2), this.globalCache.getCache(2).cacheOf(1 + (i << 2)),
                        this.globalCache.getCache(2).cacheOf(2 + (i << 2)), this.globalCache.getCache(2).cacheOf(3 + (i << 2)))
                        .checkBounds(block.minPos, block.maxPos, i);
                if (this.tasks[i].index != -1) {
                    // 如果该任务是有效任务
                    this.tasks[i].setDecoderIndex(i < node.subBlockVariantNum[0] ? 0 : 1);
                    this.realTasks[taskNums++] = this.tasks[i].setPositionInfo();
                }
            }
        } else if (block.taskType == 2) {
            // 解压指定任务列表的数据
            for (int i = 0; i < node.numOfVariants(); i++) {
                this.tasks[i].setPosition(this.globalCache.getCache(2).cacheOf(i << 2), this.globalCache.getCache(2).cacheOf(1 + (i << 2)),
                        this.globalCache.getCache(2).cacheOf(2 + (i << 2)), this.globalCache.getCache(2).cacheOf(3 + (i << 2)))
                        .checkBounds(block.taskPos, i);
                if (this.tasks[i].index != -1) {
                    // 如果该任务是有效任务
                    this.tasks[i].setDecoderIndex(i < node.subBlockVariantNum[0] ? 0 : 1);
                    this.realTasks[taskNums++] = this.tasks[i].setPositionInfo();
                }
            }
        }

        /* 如果没有任务位点，则直接返回空结果 */
        if (taskNums == 0) {
            return null;
        }

        /* 读取 allele 数据并解压 */
        this.globalCache.getCache(0).reset();
        this.globalCache.getCache(2).reset();
        gtbFile.read(this.globalCache.getCache(0), node.compressedAlleleSize);
        decompressor.decompress(this.globalCache.getCache(0), this.globalCache.getCache(2));

        /* 捕获 allele 数据 */
        int lastIndex = 0;
        int startPos;
        int endPos = this.realTasks[0].index == 0 ? -1 : 0;

        for (int i = 0; i < taskNums; i++) {
            startPos = this.globalCache.getCache(2).indexOfN(ByteCode.SLASH, endPos, this.realTasks[i].index - lastIndex);
            endPos = this.globalCache.getCache(2).indexOfN(ByteCode.SLASH, startPos + 1, 1);
            this.realTasks[i].setAlleleInfo(startPos + 1, endPos);

            // 更新 lastIndex 信息
            lastIndex = this.realTasks[i].index + 1;
        }

        /* 按照 position 进行局部重排序 */
        Arrays.sort(this.realTasks, 0, taskNums, TaskVariant::compareVariant);

        /* 读取 genotype 数据并解压 */
        this.globalCache.getCache(0).reset();
        this.globalCache.getCache(1).reset();
        gtbFile.seek(node.blockSeek);
        gtbFile.read(this.globalCache.getCache(0), node.compressedGenotypesSize);
        decompressor.decompress(this.globalCache.getCache(0), this.globalCache.getCache(1));

        /* 编码基因型数据 */
        return this.rebuild.decode(this.globalCache.getCache(1), this.globalCache.getCache(2), node, this.realTasks, taskNums);
    }

    /**
     * 关闭压缩器
     */
    public void close() throws IOException {
        this.gtbFile.close();
        this.decompressor.close();
        this.rebuild.close();
    }
}
