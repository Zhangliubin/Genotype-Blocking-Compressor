package edu.sysu.pmglab.gbc.core.extract;

import edu.sysu.pmglab.gbc.coder.decoder.BEGDecoder;
import edu.sysu.pmglab.gbc.coder.decoder.MBEGDecoder;
import edu.sysu.pmglab.gbc.core.common.qualitycontrol.allele.AlleleQC;
import edu.sysu.pmglab.gbc.core.gtbcomponent.GTBManager;
import edu.sysu.pmglab.unifyIO.partwriter.BGZOutputParam;

/**
 * @Data        :2021/05/31
 * @Author      :suranyi
 * @Contact     :suranyi.sysu@gamil.com
 * @Description :
 */

class VCFFormatterBuilder {
    final IndexPair[] pairs;
    final int totalSubjectsNum;
    final AlleleQC formatter;
    final boolean hideGenotype;
    final boolean originPhased;
    final BGZOutputParam outputParam;
    final int cacheSize;
    final BEGDecoder decoder;
    final MBEGDecoder groupDecoder;

    VCFFormatterBuilder(final BEGDecoder decoder, final MBEGDecoder groupDecoder, final IndexPair[] pairs, final GTBManager gtbManager, final ExtractTask extractTask, final int maxOriginAllelesSize) {
        this.decoder = decoder;
        this.groupDecoder = groupDecoder;
        this.pairs = pairs;
        this.totalSubjectsNum = gtbManager.getSubjectNum();
        this.formatter = extractTask.getAlleleQC();
        this.hideGenotype = extractTask.isHideGenotype();
        this.originPhased = gtbManager.isPhased();
        this.outputParam = extractTask.outputParam;

        // 压缩为 bgzf 并且隐藏基因型信息
        if (this.hideGenotype) {
            // 等位基因阵列尺寸 (2M + 等位基因大小)
            this.cacheSize = Math.min(gtbManager.getFileBaseInfoManager().getEstimateDecompressedBlockSize(), maxOriginAllelesSize + 2097152);
        } else {
            this.cacheSize = gtbManager.getFileBaseInfoManager().getEstimateDecompressedBlockSize();
        }
    }

    IVCFFormatter getInstance() {
        if (hideGenotype) {
            return new VCFFormatterHideGT(decoder, groupDecoder, pairs, formatter, totalSubjectsNum, originPhased, this.outputParam, cacheSize);
        } else {
            return new VCFFormatter(decoder, groupDecoder, pairs, formatter, totalSubjectsNum, originPhased, this.outputParam, cacheSize);
        }
    }
}