package edu.sysu.pmglab.suranyi.gbc.core.build;

import edu.sysu.pmglab.suranyi.check.Assert;
import edu.sysu.pmglab.suranyi.easytools.ValueUtils;
import edu.sysu.pmglab.suranyi.gbc.core.exception.GTBComponentException;

/**
 * @author suranyi
 * @description 块大小参数池
 */

public enum BlockSizeParameter {
    /**
     * 块大小参数
     */
    ZERO(0, 128, 16777215),
    ONE(1, 256, 8388607),
    TWO(2, 512, 4194303),
    THREE(3, 1024, 2097151),
    FOUR(4, 2048, 1048575),
    FIVE(5, 4096, 524287),
    SIX(6, 8192, 262143),
    SEVEN(7, 16384, 131071),

    /**
     * 建议的参数值大小，该数值基于 512 MB 缓冲大小设计，以尽可能提高并行性能
     */
    SUGGEST_ZERO(0, 128, 16777215),
    SUGGEST_ONE(1, 256, 2097151),
    SUGGEST_TWO(2, 512, 1048575),
    SUGGEST_THREE(3, 1024, 524287),
    SUGGEST_FOUR(4, 2048, 262143),
    SUGGEST_FIVE(5, 4096, 131071),
    SUGGEST_SIX(6, 8192, 65535),
    SUGGEST_SEVEN(7, 16384, 32767);

    final int blockSizeType;
    final int blockSize;
    final int maxSubjectNum;

    public static final BlockSizeParameter[] VALUES = {ZERO, ONE, TWO, THREE, FOUR, FIVE, SIX, SEVEN};
    public static final BlockSizeParameter[] SUGGEST_VALUES = {SUGGEST_ZERO, SUGGEST_ONE, SUGGEST_TWO, SUGGEST_THREE, SUGGEST_FOUR, SUGGEST_FIVE, SUGGEST_SIX, SUGGEST_SEVEN};

    /**
     * 块大小默认参数，-1 代表自动选择
     */
    public static final int DEFAULT_BLOCK_SIZE_TYPE = -1;
    public static final int DEFAULT_BLOCK_SIZE = -1;
    public static final int MIN_BLOCK_SIZE_TYPE = 0;
    public static final int MAX_BLOCK_SIZE_TYPE = 7;

    BlockSizeParameter(int blockSizeType, int blockSize, int maxSubjectNum) {
        this.blockSizeType = blockSizeType;
        this.blockSize = blockSize;
        this.maxSubjectNum = maxSubjectNum;
    }

    /**
     * 传入块参数值对应的块大小真实值
     * @param blockSizeType 块大小参数，该值的取值范围为 0～7
     * @return 块大小参数值，2 ^ (7 + x)
     */
    public static int getBlockSize(int blockSizeType) {
        Assert.valueRange(blockSizeType, MIN_BLOCK_SIZE_TYPE, MAX_BLOCK_SIZE_TYPE);
        return VALUES[blockSizeType].blockSize;
    }

    /**
     * 获取 GTB 最大可容纳的样本数
     * @return 当前最大可处理的样本个数
     */
    public static int getMaxSubjectNum() {
        return VALUES[0].maxSubjectNum;
    }

    /**
     * 获取对应的块大小参数值最大可容纳的样本数
     * @param blockSizeType 块大小参数，该值的取值范围为 0～7；-1 表示
     * @return 该块大小参数值可以处理的最大样本个数
     */
    public static int getMaxSubjectNum(int blockSizeType) {
        Assert.valueRange(blockSizeType, MIN_BLOCK_SIZE_TYPE, MAX_BLOCK_SIZE_TYPE);
        return VALUES[blockSizeType].maxSubjectNum;
    }

    /**
     * 获取支持的最大块大小参数
     * 计算方法：位点数 * 样本数 <= 2GB - 2
     */
    public static int getMaxBlockSizeType(int validSubjectNum) {
        Assert.NotNegativeValue(validSubjectNum);

        // 从后往前检索 (越靠后，能够处理的样本个数越少)
        for (int i = VALUES.length - 1; i >= 0; i--) {
            if (validSubjectNum < VALUES[i].maxSubjectNum) {
                return VALUES[i].blockSizeType;
            }
        }

        throw new GTBComponentException(String.format("validSubjectNum is greater than the current maximum that GBC can handle (%d > %d)", validSubjectNum, VALUES[0].maxSubjectNum));
    }

    /**
     * 获取支持的最大块大小参数
     * 计算方法：位点数 * 样本数 <= 2GB - 2, 取 1GB 大小，以平衡解压时的内存开销
     */
    public static int getSuggestBlockSizeType(int validSubjectNum) {
        Assert.NotNegativeValue(validSubjectNum);

        // 从后往前检索 (越靠后，能够处理的样本个数越少)
        for (int i = SUGGEST_VALUES.length - 1; i >= 0; i--) {
            if (validSubjectNum < SUGGEST_VALUES[i].maxSubjectNum) {
                return SUGGEST_VALUES[i].blockSizeType;
            }
        }

        throw new GTBComponentException(String.format("validSubjectNum is greater than the current maximum that GBC can handle (%d > %d)", validSubjectNum, VALUES[0].maxSubjectNum));
    }

    /**
     * 获取支持的最大块大小参数
     * 计算方法：位点数 * 样本数 <= 2GB - 2, 取 1GB 大小，以平衡解压时的内存开销
     */
    public static int getSuggestBlockSizeType(int currentBlockSizeType, int validSubjectNum) {
        if (currentBlockSizeType == -1) {
            // 自动选择合适参数
            return getSuggestBlockSizeType(validSubjectNum);
        } else {
            // 判断该参数是否合法，如果不合法则自动向下选择
            return ValueUtils.min(currentBlockSizeType, getMaxBlockSizeType(validSubjectNum));
        }
    }

    /**
     * 获取支持的块大小参数
     */
    public static Integer[] getSupportedBlockSizes() {
        Integer[] sizes = new Integer[VALUES.length + 1];
        sizes[0] = -1;
        for (int i = 0; i < VALUES.length; i++) {
            sizes[i + 1] = VALUES[i].blockSize;
        }
        return sizes;
    }
}