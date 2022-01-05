package edu.sysu.pmglab.suranyi.compressor.lzma;

import edu.sysu.pmglab.suranyi.container.VolumeByteStream;
import edu.sysu.pmglab.suranyi.compressor.IDecompressor;
import edu.sysu.pmglab.suranyi.check.Assert;

import java.io.IOException;

/**
 * @author suranyi
 * @description LZMA 解压器，基于 LZMA-SDK 进行二次开发
 */

public class LzmaDecompressor extends IDecompressor {
    public static final String COMPRESSOR_NAME = "LZMA";

    final ILzmaDecompressCtx decompressor = new LzmaDecompressCtx();

    public LzmaDecompressor() {
        this(0);
    }

    public LzmaDecompressor(int cacheSize) {
        super(cacheSize);
    }

    public LzmaDecompressor(VolumeByteStream cache) {
        super(cache);
    }

    @Override
    public int getDecompressBound(byte[] src, int offset, int length) {
        return decompressBound(src, offset, length);
    }

    @Override
    public final int decompress(byte[] src, int srcOffset, int srcLength, byte[] dst, int dstOffset, int detLength) throws IOException {
        return this.decompressor.decompress(src, srcOffset, srcLength, dst, dstOffset, detLength);
    }

    @Override
    public void close() {
        this.decompressor.close();
    }

    public static int decompressBound(byte[] src, int offset, int length) {
        Assert.that(length >= 18);

        long size = 0;
        for (int i = 0; i < 8; ++i) {
            size |= (long) (src[i + offset + 5] & 0xFF) << (8 * i);
        }

        Assert.valueRange(size, 0, Integer.MAX_VALUE - 2);
        return (int) size;
    }

    public static VolumeByteStream decompress(byte[] src, int offset, int length) throws IOException {
        LzmaDecompressor decompressor = new LzmaDecompressor();
        decompressor.decompress(src, offset, length, decompressor.cache);
        decompressor.close();
        return decompressor.getCache();
    }
}
