package edu.sysu.pmglab.suranyi.gbc.core.common.block;

import edu.sysu.pmglab.suranyi.container.VolumeByteStream;
import edu.sysu.pmglab.suranyi.easytools.ByteCode;
import edu.sysu.pmglab.suranyi.easytools.ValueUtils;
import edu.sysu.pmglab.suranyi.gbc.coder.encoder.MBEGEncoder;

/**
 * @Data :2021/05/31
 * @Author :suranyi
 * @Contact :suranyi.sysu@gamil.com
 * @Description :位点数据
 */

public class VariantAbstract {
    /**
     * 该位点的基因型字节编码值区域
     */
    public int encodedStart;
    public final int encodedLength;

    /**
     * 该位点编码器的索引
     */
    public int encoderIndex;

    /**
     * 该位点位置值
     */
    public int position;

    /**
     * 特征向量
     */
    public final int[] blockCounts;
    public final int featureLength;

    /**
     * 该位点数据的等位基因信息
     */
    final public VolumeByteStream allele = new VolumeByteStream(10);

    /**
     * 构造器方法
     *
     * @param validSubjectNum 有效样本数
     */
    public VariantAbstract(final int validSubjectNum) {
        this.encodedStart = 0;
        this.encodedLength = validSubjectNum;
        this.blockCounts = null;
        this.featureLength = -1;
    }

    /**
     * 构造器方法
     *
     * @param validSubjectNum 有效样本数
     * @param windowSize      采样窗口大小
     */
    public VariantAbstract(final int validSubjectNum, final int windowSize) {
        this.encodedStart = 0;
        this.encodedLength = validSubjectNum;
        this.blockCounts = new int[windowSize];

        // 每个特征的长度
        this.featureLength = (int) Math.ceil((float) validSubjectNum / windowSize);
    }

    /**
     * 构造器方法
     *
     * @param validSubjectNum 有效样本数
     * @param featureLength   特征向量长度
     * @param windowSize      采样窗口大小
     */
    public VariantAbstract(final int validSubjectNum, final int windowSize, final int featureLength) {
        this.encodedStart = 0;
        this.encodedLength = validSubjectNum;
        this.blockCounts = new int[windowSize];
        this.featureLength = featureLength;
    }

    /**
     * 提取位置数据，转为 4 字节整数，并写入到目标容器中
     */
    public void writePosTo(final VolumeByteStream dst) {
        dst.writeIntegerValue(this.position);
    }

    /**
     * 提取等位基因序列，并写入到目标容器中
     *
     * @param dst 目标容器
     */
    public void writeAlleleTo(final VolumeByteStream dst) {
        dst.write(this.allele);
        dst.write(ByteCode.SLASH);
    }

    public void setAllele(VolumeByteStream allele) {
        this.allele.writeSafety(allele);
    }

    public void setAllele(VolumeByteStream allele, int offset, int length) {
        this.allele.writeSafety(allele, offset, length);
    }

    public void setAllele(byte[] allele, int offset, int length) {
        this.allele.writeSafety(allele, offset, length);
    }

    public int alleleSize() {
        return this.allele.size() + 1;
    }

    /**
     * 比对两个位点的顺序
     *
     * @param v1 第一个比对位点
     * @param v2 第二个比对位点
     */
    public static int compareEncoderIndex(VariantAbstract v1, VariantAbstract v2) {
        return Integer.compare(v1.encoderIndex, v2.encoderIndex);
    }

    /**
     * 设置特征向量
     */
    public void setFeatureVector(MBEGEncoder encoder, byte[] encodedCache) {
        int upBound;

        for (int i = 0; i < this.blockCounts.length; i++) {
            this.blockCounts[i] = 0;
            upBound = ValueUtils.min(this.featureLength * (i + 1), this.encodedLength);
            for (int j = this.featureLength * i; j < upBound; j++) {
                this.blockCounts[i] += encoder.scoreOf(encodedCache[encodedStart + j] & 0xFF) * (upBound - j);
            }
        }

    }

    /**
     * 比对两个位点的顺序
     *
     * @param v1 第一个比对位点
     * @param v2 第二个比对位点
     */
    public static int comparatorBaseOnFeatureVector(VariantAbstract v1, VariantAbstract v2) {
        int status = compareEncoderIndex(v1, v2);

        if (status == 0) {
            // 若相等
            if (v1.encoderIndex == 0) {
                for (int i = 0; i < v1.blockCounts.length; i++) {
                    if (v1.blockCounts[i] < v2.blockCounts[i]) {
                        return 1;
                    } else if (v1.blockCounts[i] > v2.blockCounts[i]) {
                        return -1;
                    }
                }
            } else {
                for (int i = 0; i < v1.blockCounts.length; i++) {
                    if (v1.blockCounts[i] < v2.blockCounts[i]) {
                        return -1;
                    } else if (v1.blockCounts[i] > v2.blockCounts[i]) {
                        return 1;
                    }
                }
            }
        }

        return status;
    }
}