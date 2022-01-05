package edu.sysu.pmglab.suranyi.gbc.core.gtbcomponent.gtbreader.formatter;

import edu.sysu.pmglab.suranyi.container.VolumeByteStream;
import edu.sysu.pmglab.suranyi.gbc.coder.BEGTransfer;
import edu.sysu.pmglab.suranyi.gbc.core.gtbcomponent.gtbreader.Variant;

/**
 * @author suranyi
 */

public enum UnphasedVariantFormatter implements VariantFormatter<Void, byte[]> {
    /**
     * 转为无向 BEGs
     */
    INSTANCE;

    @Override
    public byte[] apply(Variant variant) {
        VolumeByteStream cache = new VolumeByteStream(variant.BEGs.length);
        for (int i = 0; i < variant.BEGs.length; i++) {
            cache.write(BEGTransfer.toUnphased(variant.BEGs[i]));
        }
        return cache.getCache();
    }
}
