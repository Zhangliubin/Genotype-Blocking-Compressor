package edu.sysu.pmglab.suranyi.gbc.core.gtbcomponent.gtbreader.formatter;

import edu.sysu.pmglab.suranyi.container.VolumeByteStream;
import edu.sysu.pmglab.suranyi.easytools.ByteCode;
import edu.sysu.pmglab.suranyi.easytools.ValueUtils;
import edu.sysu.pmglab.suranyi.gbc.coder.decoder.BEGDecoder;
import edu.sysu.pmglab.suranyi.gbc.core.gtbcomponent.gtbreader.Variant;

/**
 * @author suranyi
 */

public enum EasyVCFVariantFormatter implements VariantFormatter<Void, byte[]> {
    /**
     * 简易 VCF 格式转换器 (不包含 INFO 信息)
     */
    INSTANCE;

    @Override
    public byte[] apply(Variant variant) {
        VolumeByteStream cache = new VolumeByteStream(estimateSize(variant));
        apply(variant, cache);
        return cache.values();
    }

    @Override
    public int apply(Variant variant, VolumeByteStream cache) {
        int originLength = cache.size();
        cache.write(variant.chromosome);
        cache.write(ByteCode.TAB);

        // pos 信息
        cache.write(ValueUtils.stringValueOfAndGetBytes(variant.position));
        cache.write(ByteCode.TAB);

        // id 信息
        cache.write(ByteCode.PERIOD);
        cache.write(ByteCode.TAB);

        // allele 信息
        cache.write(variant.REF);
        cache.write(ByteCode.TAB);
        cache.write(variant.ALT);
        cache.write(ByteCode.TAB);

        // qual 信息
        cache.write(ByteCode.PERIOD);
        cache.write(ByteCode.TAB);

        // filter 信息
        cache.write(ByteCode.PERIOD);
        cache.write(ByteCode.TAB);

        // info 信息
        cache.write(ByteCode.PERIOD);
        cache.write(ByteCode.TAB);

        // format 信息
        cache.write(ByteCode.GT_STRING);

        // genotype 信息
        BEGDecoder decoder = BEGDecoder.getDecoder(variant.phased);
        for (byte code : variant.BEGs) {
            cache.write(ByteCode.TAB);
            cache.write(decoder.decode(variant.ploidy, code));
        }

        return cache.size() - originLength;
    }

    int estimateSize(Variant variant) {
        if (variant.ploidy == 1) {
            return variant.chromosome.length() + ValueUtils.byteArrayOfValueLength(variant.position) + variant.REF.length + variant.ALT.length + 14 + variant.BEGs.length * 3;
        } else {
            return variant.chromosome.length() + ValueUtils.byteArrayOfValueLength(variant.position) + variant.REF.length + variant.ALT.length + 14 + variant.BEGs.length * 6;
        }
    }
}
