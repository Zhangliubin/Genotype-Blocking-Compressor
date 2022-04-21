package edu.sysu.pmglab.gbc.core.extract;

import edu.sysu.pmglab.container.VolumeByteStream;
import edu.sysu.pmglab.gbc.coder.decoder.BEGDecoder;
import edu.sysu.pmglab.gbc.coder.decoder.MBEGDecoder;
import edu.sysu.pmglab.gbc.constant.ChromosomeTags;
import edu.sysu.pmglab.gbc.core.common.qualitycontrol.allele.AlleleQC;
import edu.sysu.pmglab.gbc.core.gtbcomponent.GTBNode;
import edu.sysu.pmglab.unifyIO.partwriter.BGZOutputParam;

import java.nio.ByteBuffer;

/**
 * @Data :2021/03/09
 * @Author :suranyi
 * @Contact :suranyi.sysu@gamil.com
 * @Description :重构器
 */

class VCFFormatterHideGT extends IVCFFormatter {
    VCFFormatterHideGT(BEGDecoder decoder, MBEGDecoder groupDecoder, IndexPair[] pairs, AlleleQC formatter, int totalSubjectNum, boolean originPhased, BGZOutputParam outputParam, int cacheSize) {
        super(decoder, groupDecoder, pairs, formatter, totalSubjectNum, originPhased, outputParam, cacheSize);
    }

    /**
     * 重构数据主方法
     */
    @Override
    ByteBuffer decode(VolumeByteStream genotypeEncodedStream, VolumeByteStream alleleStream, GTBNode node, TaskVariant[] task, int validTasksNum) {
        this.outputStream.start();

        int start;
        int code;
        int alleleCounts;
        int missSubjectNum;
        int validAllelesNum;
        int secondBlockStart = this.eachLineSize * node.subBlockVariantNum[0];
        int ploidy = ChromosomeTags.getPloidy(node.chromosomeIndex);

        // 先统计 allele 信息
        for (int i = 0; i < validTasksNum; i++) {
            // 标记该位点起始数据
            alleleCounts = 0;
            missSubjectNum = 0;

            if (task[i].decoderIndex == 0) {
                start = this.eachLineSize * task[i].index;

                for (IndexPair pair : this.pairs) {
                    // 获取其具体的编码值
                    code = groupDecoder.decode(genotypeEncodedStream.cacheOf(start + pair.groupIndex) & 0xFF, pair.codeIndex);

                    // 计算等位基因个数
                    alleleCounts += BEGDecoder.alternativeAlleleNumOf(ploidy, code);

                    // 计算 missGenotype 个数
                    missSubjectNum += BEGDecoder.isMiss(code) ? 1 : 0;
                }
            } else {
                start = secondBlockStart + (task[i].index - node.subBlockVariantNum[0]) * this.totalSubjectNum;

                for (IndexPair pair : this.pairs) {
                    // 向缓冲区写入数据
                    code = genotypeEncodedStream.cacheOf(start + pair.index) & 0xFF;

                    // 计算等位基因个数
                    alleleCounts += BEGDecoder.alternativeAlleleNumOf(ploidy, code);

                    // 计算 missGenotype 个数
                    missSubjectNum += BEGDecoder.isMiss(code) ? 1 : 0;
                }
            }

            // 有效样本数 = 筛选样本数 - 缺失样本数
            validAllelesNum = (this.pairs.length - missSubjectNum) * ploidy;

            // 等位基因数 = 有效样本数 * 倍型
            if (!this.formatter.filter(alleleCounts, validAllelesNum)) {
                writeNoGenotype(alleleStream, node.chromosomeIndex, task[i], alleleCounts, validAllelesNum);
            }
        }

        this.outputStream.finish();
        return this.outputStream.getCache();
    }
}