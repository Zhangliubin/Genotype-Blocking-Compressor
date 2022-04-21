package edu.sysu.pmglab.gbc.core.common.combiner;

import edu.sysu.pmglab.container.VolumeByteStream;
import edu.sysu.pmglab.gbc.coder.encoder.MBEGEncoder;
import edu.sysu.pmglab.gbc.core.common.block.VariantAbstract;

/**
 * @Data        :2021/05/30
 * @Author      :suranyi
 * @Contact     :suranyi.sysu@gamil.com
 * @Description :有向基因型编码组合器
 */

public class PhasedCodeCombiner implements ICodeCombiner {
    final int validSubjectNum;
    final int groupNum;
    final int resNum;

    PhasedCodeCombiner(int validSubjectNum) {
        this.validSubjectNum = validSubjectNum;
        this.groupNum = validSubjectNum / 3;
        this.resNum = validSubjectNum % 3;
    }

    @Override
    public void process(MBEGEncoder encoder, VariantAbstract[] variants, short[] subBlockVariantNum, byte[] encodedCache, VolumeByteStream dst) {
        /* 拼接并压缩基因型数据 */
        for (int i = 0; i < subBlockVariantNum[0]; i++) {
            for (int j = 0; j < groupNum; j++) {
                dst.write(encoder.encode(encodedCache[variants[i].encodedStart + j * 3],
                        encodedCache[variants[i].encodedStart + j * 3 + 1],
                        encodedCache[variants[i].encodedStart + j * 3 + 2]));
            }

            if (resNum == 1) {
                dst.write(encoder.encode(encodedCache[variants[i].encodedStart + validSubjectNum - 1]));
            } else if (resNum == 2) {
                dst.write(encoder.encode(encodedCache[variants[i].encodedStart + validSubjectNum - 2], encodedCache[variants[i].encodedStart + validSubjectNum - 1]));
            }
        }

        for (int i = subBlockVariantNum[0]; i < subBlockVariantNum[0] + subBlockVariantNum[1]; i++) {
            dst.write(encodedCache, variants[i].encodedStart, validSubjectNum);
        }
    }

    @Override
    public void process(MBEGEncoder encoder, byte[] BEGs, int encoderIndex, VolumeByteStream dst) {
        /* 拼接并压缩基因型数据 */
        if (encoderIndex == 0) {
            for (int j = 0; j < groupNum; j++) {
                dst.write(encoder.encode(BEGs[j * 3],
                        BEGs[j * 3 + 1],
                        BEGs[j * 3 + 2]));
            }

            if (resNum == 1) {
                dst.write(encoder.encode(BEGs[validSubjectNum - 1]));
            } else if (resNum == 2) {
                dst.write(encoder.encode(BEGs[validSubjectNum - 2], BEGs[validSubjectNum - 1]));
            }
        } else {
            dst.write(BEGs, 0, validSubjectNum);
        }
    }
}