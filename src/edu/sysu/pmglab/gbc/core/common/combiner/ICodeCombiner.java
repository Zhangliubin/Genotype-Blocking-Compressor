package edu.sysu.pmglab.gbc.core.common.combiner;

import edu.sysu.pmglab.container.VolumeByteStream;
import edu.sysu.pmglab.gbc.coder.encoder.MBEGEncoder;
import edu.sysu.pmglab.gbc.core.common.block.VariantAbstract;

/**
 * @Data        :2021/05/30
 * @Author      :suranyi
 * @Contact     :suranyi.sysu@gamil.com
 * @Description :编码组合器接口
 */

public interface ICodeCombiner {
    /**
     * 构造器，初始化编码组合器
     * @param phased 是否有向
     * @param validSubjectNum 有效样本数
     * @return 根据是否组合编码信息获取对应的组合器
     */
    static ICodeCombiner getInstance(boolean phased, int validSubjectNum) {
        return phased ? new PhasedCodeCombiner(validSubjectNum) : new UnphasedCodeCombiner(validSubjectNum);
    }

    /**
     * 组合编码数据
     * @param encoder 组合编码器
     * @param variants 变异位点列表
     * @param subBlockVariantNum 子块各类位点计数
     * @param dst 写入容器
     * @param encodedCache 编码缓冲区
     */
    void process(MBEGEncoder encoder, VariantAbstract[] variants, short[] subBlockVariantNum, byte[] encodedCache, VolumeByteStream dst);

    /**
     * 组合编码数据
     * @param BEGs 字节编码基因型
     * @param encoderIndex 编码器索引
     * @param dst 写入容器
     */
    void process(MBEGEncoder encoder, byte[] BEGs, int encoderIndex, VolumeByteStream dst);
}