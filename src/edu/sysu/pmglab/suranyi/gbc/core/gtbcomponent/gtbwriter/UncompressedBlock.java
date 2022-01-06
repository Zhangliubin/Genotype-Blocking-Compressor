package edu.sysu.pmglab.suranyi.gbc.core.gtbcomponent.gtbwriter;

import edu.sysu.pmglab.suranyi.container.ShareCache;
import edu.sysu.pmglab.suranyi.container.VolumeByteStream;
import edu.sysu.pmglab.suranyi.gbc.core.build.IBuildTask;
import edu.sysu.pmglab.suranyi.gbc.core.common.block.VariantAbstract;
import edu.sysu.pmglab.suranyi.gbc.core.common.switcher.ISwitcher;

/**
 * @Data        :2021/04/25
 * @Author      :suranyi
 * @Contact     :suranyi.sysu@gamil.com
 * @Description :未压缩的块数据 (基因型数据保存为 BEG 编码形式)
 */

class UncompressedBlock {
    public int chromosomeIndex;
    public final VariantAbstract[] variants;
    public int seek;
    public final VolumeByteStream encodedCache;
    public final int validSubjectNum;

    ShareCache caches;
    GTBCompressionContext ctx;

    public UncompressedBlock(int validSubjectNum, IBuildTask task, int variantsNum) {
        this.variants = new VariantAbstract[variantsNum];
        this.validSubjectNum = validSubjectNum;

        if (task.isReordering()) {
            int windowSize = ISwitcher.getRealWindowSize(task.getWindowSize(), validSubjectNum);
            int distanceFeatureLength = ISwitcher.getFeatureLength(validSubjectNum, windowSize);

            for (int i = 0; i < variantsNum; i++) {
                this.variants[i] = new VariantAbstract(validSubjectNum, windowSize, distanceFeatureLength);
                this.variants[i].encodedStart = i * validSubjectNum;
            }
        } else {
            for (int i = 0; i < variantsNum; i++) {
                this.variants[i] = new VariantAbstract(validSubjectNum);
                this.variants[i].encodedStart = i * validSubjectNum;
            }
        }

        this.encodedCache = new VolumeByteStream(this.validSubjectNum * variantsNum);
        caches = new ShareCache(encodedCache, new VolumeByteStream((variantsNum * Math.max(20, validSubjectNum)) >> 1));
        ctx = new GTBCompressionContext(task, this.validSubjectNum, caches);
    }

    public void reset() {
        this.seek = 0;

        for (int i = 0; i < variants.length; i++) {
            this.variants[i].encodedStart = i * validSubjectNum;
            this.variants[i].allele.reset();
        }
    }

    public VariantAbstract getCurrentVariant() {
        return this.variants[this.seek];
    }

    public int remaining() {
        return this.variants.length - this.seek;
    }

    public boolean empty() {
        return this.seek == 0;
    }

    public void freeMemory() {
        caches.freeMemory();
        ctx.close();
    }
}