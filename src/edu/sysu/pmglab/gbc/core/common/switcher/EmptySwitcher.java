package edu.sysu.pmglab.gbc.core.common.switcher;

import edu.sysu.pmglab.gbc.coder.encoder.MBEGEncoder;
import edu.sysu.pmglab.gbc.core.common.block.VariantAbstract;

import java.util.Arrays;

/**
 * @Data        :2021/03/26
 * @Author      :suranyi
 * @Contact     :suranyi.sysu@gamil.com
 * @Description :空交换器
 */

enum EmptySwitcher implements ISwitcher {
    /**
     * 单例模式特征交换器
     */
    INSTANCE;

    @Override
    public void switchingRow(MBEGEncoder encoder, VariantAbstract[] variants, int variantsNum, byte[] encodedCache) {
        Arrays.sort(variants, 0, variantsNum, VariantAbstract::compareEncoderIndex);
    }
}
