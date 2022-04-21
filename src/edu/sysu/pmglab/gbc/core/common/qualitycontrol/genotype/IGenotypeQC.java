package edu.sysu.pmglab.gbc.core.common.qualitycontrol.genotype;

import edu.sysu.pmglab.container.VolumeByteStream;
import edu.sysu.pmglab.easytools.ByteCode;

/**
 * @Data        :2021/08/29
 * @Author      :suranyi
 * @Contact     :suranyi.sysu@gamil.com
 * @Description :基因型质控器接口
 */

public interface IGenotypeQC {
    /**
     * 执行过滤，true 时 移除该位点
     * @param variant 位点信息
     * @param seek 该基因型的起点
     * @param length 基因型及 info 的长度
     * @param index 字段索引
     * @return 是否移除该位点数据
     */
    boolean qualityControl(VolumeByteStream variant, int seek, int length, int index);

    /**
     * 是否为空过滤器
     * @return 是否为空过滤器
     */
    default boolean empty() {
        return false;
    }

    /**
     * 获取关键字
     * @return 关键字
     */
    byte[] getKeyWord();

    /**
     * 计算分数值
     * @param variant 变异位点序列
     * @param seek 当前计算的基因型终点
     * @param length 该基因型的长度
     * @return 获取该字段的数值
     */
    default int calculateScore(VolumeByteStream variant, int seek, int length, int index) {
        int score = 0;
        int count = 0;

        out:
        for (int i = 1; i < length; i++) {
            // 从第一个标签开始搜索
            if (variant.cacheOf(seek - length + i) == ByteCode.COLON) {
                count++;

                if (count == index) {
                    // 获得起始点 i
                    for (int j = i + 1; j < length; j++) {
                        if (variant.cacheOf(seek - length + j) == ByteCode.COLON || variant.cacheOf(seek - length + j) == ByteCode.PERIOD) {
                            // 抵达下一个 :，或识别到 . (截断处理)，或到达最后
                            break out;
                        }

                        score = score * 10 + (variant.cacheOf(seek - length + j) - 48);
                    }
                }
            }
        }

        return score;
    }

    /**
     * 重设控制器
     * @param genotypeQC 新控制器
     */
    void reset(IGenotypeQC genotypeQC);

    /**
     * 获取阈值
     */
    int getMethod();
}
