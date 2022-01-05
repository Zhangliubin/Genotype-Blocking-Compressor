package edu.sysu.pmglab.suranyi.gbc.core.extract;

import edu.sysu.pmglab.suranyi.container.VolumeByteStream;
import edu.sysu.pmglab.suranyi.easytools.ByteCode;
import edu.sysu.pmglab.suranyi.gbc.coder.decoder.BEGDecoder;
import edu.sysu.pmglab.suranyi.gbc.coder.decoder.MBEGDecoder;
import edu.sysu.pmglab.suranyi.gbc.constant.ChromosomeInfo;
import edu.sysu.pmglab.suranyi.gbc.core.common.qualitycontrol.allele.AlleleQC;
import edu.sysu.pmglab.suranyi.gbc.core.gtbcomponent.GTBNode;
import edu.sysu.pmglab.suranyi.unifyIO.partwriter.BGZOutputParam;

import java.nio.ByteBuffer;

/**
 * @Data :2021/03/09
 * @Author :suranyi
 * @Contact :suranyi.sysu@gamil.com
 * @Description :重构器
 */

class VCFFormatter extends IVCFFormatter {
    final int[] genotypeEncodedCache;

    VCFFormatter(BEGDecoder decoder, MBEGDecoder groupDecoder, IndexPair[] pairs, AlleleQC formatter, int totalSubjectNum, boolean originPhased, BGZOutputParam outputParam, int cacheSize) {
        super(decoder, groupDecoder, pairs, formatter, totalSubjectNum, originPhased, outputParam, cacheSize);
        this.genotypeEncodedCache = new int[pairs.length];
    }

    /**
     * 重构数据主方法
     */
    @Override
    ByteBuffer decode(VolumeByteStream genotypeEncodedStream, VolumeByteStream alleleStream, GTBNode node, TaskVariant[] task, int validTasksNum) {
        this.outputStream.start();

        int start;
        int alleleCounts;
        int missSubjectNum;
        int validAllelesNum;
        int secondBlockStart = this.eachLineSize * node.subBlockVariantNum[0];
        int ploidy = ChromosomeInfo.getPloidy(node.chromosomeIndex);

        // 先统计 allele 信息
        for (int i = 0; i < validTasksNum; i++) {
            // 标记该位点起始数据
            alleleCounts = 0;
            missSubjectNum = 0;

            if (task[i].decoderIndex == 0) {
                start = this.eachLineSize * task[i].index;

                // 还原编码值
                for (int j = 0; j < pairs.length; j++) {
                    // 获取其具体的编码值
                    this.genotypeEncodedCache[j] = groupDecoder.decode(genotypeEncodedStream.cacheOf(start + this.pairs[j].groupIndex) & 0xFF, this.pairs[j].codeIndex);

                    // 计算等位基因个数
                    alleleCounts += BEGDecoder.alternativeAlleleNumOf(ploidy, this.genotypeEncodedCache[j]);

                    // 计算 missGenotype 个数
                    missSubjectNum += BEGDecoder.isMiss(this.genotypeEncodedCache[j]) ? 1 : 0;
                }
            } else {
                start = secondBlockStart + (task[i].index - node.subBlockVariantNum[0]) * this.totalSubjectNum;

                for (int j = 0; j < pairs.length; j++) {
                    // 向缓冲区写入数据
                    this.genotypeEncodedCache[j] = genotypeEncodedStream.cacheOf(start + this.pairs[j].index) & 0xFF;

                    // 计算等位基因个数
                    alleleCounts += BEGDecoder.alternativeAlleleNumOf(ploidy, this.genotypeEncodedCache[j]);

                    // 计算 missGenotype 个数
                    missSubjectNum += BEGDecoder.isMiss(this.genotypeEncodedCache[j]) ? 1 : 0;
                }
            }

            // 有效样本数 = 筛选样本数 - 缺失样本数
            validAllelesNum = (this.pairs.length - missSubjectNum) * ploidy;

            // 等位基因数 = 有效样本数 * 倍型
            if (!this.formatter.filter(alleleCounts, validAllelesNum)) {
                writeNoGenotype(alleleStream, node.chromosomeIndex, task[i], alleleCounts, validAllelesNum);

                // 写入基因型数据
                for (int code : this.genotypeEncodedCache) {
                    this.outputStream.write(ByteCode.TAB);
                    this.outputStream.write(decoder.decode(ploidy, code));
                }
            }
        }

        this.outputStream.finish();
        return this.outputStream.getCache();
    }
}