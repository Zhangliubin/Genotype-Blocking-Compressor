package edu.sysu.pmglab.gbc.core.build;

import edu.sysu.pmglab.gbc.core.common.block.VariantAbstract;
import edu.sysu.pmglab.gbc.core.common.switcher.ISwitcher;
import edu.sysu.pmglab.container.VolumeByteStream;

/**
 * @Data :2021/04/25
 * @Author :suranyi
 * @Contact :suranyi.sysu@gamil.com
 * @Description :未压缩的块数据 (基因型数据保存为 BEG 编码形式)
 */

class UncompressedBlock {
    public int chromosomeIndex;
    public final VariantAbstract[] variants;
    public int seek;
    public final VolumeByteStream encodedCache;
    public final int validSubjectNum;

    public UncompressedBlock(int validSubjectNum, IBuildTask task, int variantsNum, VolumeByteStream encodedCache) {
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

        this.encodedCache = encodedCache;
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
}