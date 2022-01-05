package edu.sysu.pmglab.suranyi.compressor.gzip;

import edu.sysu.pmglab.suranyi.check.Assert;
import edu.sysu.pmglab.suranyi.compressor.IDecompressor;
import edu.sysu.pmglab.suranyi.container.VolumeByteStream;
import edu.sysu.pmglab.suranyi.easytools.ValueUtils;

import java.io.IOException;

/**
 * @author suranyi
 * @description gzip 解压器
 */

public final class GzipDecompressor extends IDecompressor {
    public static final String COMPRESSOR_NAME = "LZMA";

    final GzipDecompressStreamCtx decompressor = new GzipDecompressStreamCtx();

    public GzipDecompressor() {
        super();
    }

    public GzipDecompressor(int cacheSize) {
        super(cacheSize);
    }

    public GzipDecompressor(VolumeByteStream cache) {
        super(cache);
    }

    @Override
    public int getDecompressBound(byte[] src, int offset, int length) {
        return decompressBound(src, offset, length);
    }

    @Override
    public final int decompress(byte[] src, int srcOffset, int srcLength, byte[] dst, int dstOffset, int dstLength) throws IOException {
        return this.decompressor.decompress(src, srcOffset, srcLength, dst, dstOffset, dstLength);
    }

    @Override
    public void close() {
    }

    public static int decompressBound(byte[] src, int offset, int length) {
        Assert.that(length >= 4);

        return ValueUtils.byteArray2IntegerValue(src[offset + length - 4], src[offset + length - 3], src[offset + length - 2], src[offset + length - 1]);
    }

    public static VolumeByteStream decompress(byte[] src, int offset, int length) throws IOException {
        GzipDecompressor decompressor = new GzipDecompressor();
        decompressor.decompress(src, offset, length, decompressor.cache);
        decompressor.close();
        return decompressor.getCache();
    }
}
