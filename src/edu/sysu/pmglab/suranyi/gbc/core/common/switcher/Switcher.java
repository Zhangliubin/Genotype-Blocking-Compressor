package edu.sysu.pmglab.suranyi.gbc.core.common.switcher;

import edu.sysu.pmglab.suranyi.gbc.coder.encoder.MBEGEncoder;
import edu.sysu.pmglab.suranyi.gbc.core.common.block.VariantAbstract;

import java.util.Arrays;

/**
 * @Data :2021/02/23
 * @Author :suranyi
 * @Contact :suranyi.sysu@gamil.com
 * @Description :特征交换器
 */

public enum Switcher implements ISwitcher {
    /**
     * 单例模式特征交换器
     */
    INSTANCE;

    @Override
    public void switchingRow(MBEGEncoder encoder, VariantAbstract[] variants, int variantsNum, byte[] encodedCache) {
        for (int i = 0; i < variantsNum; i++) {
            variants[i].setFeatureVector(encoder, encodedCache);
        }

        Arrays.sort(variants, 0, variantsNum, VariantAbstract::comparatorBaseOnFeatureVector);
    }
}
