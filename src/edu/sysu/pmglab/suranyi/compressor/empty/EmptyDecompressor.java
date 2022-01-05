package edu.sysu.pmglab.suranyi.compressor.empty;

import edu.sysu.pmglab.suranyi.compressor.IDecompressor;
import edu.sysu.pmglab.suranyi.container.VolumeByteStream;

import java.io.IOException;

/**
 * @author suranyi
 * @description 空解压器
 */

public final class EmptyDecompressor extends IDecompressor {
    public static final String COMPRESSOR_NAME = "EMPTY";

    public EmptyDecompressor() {
        super();
    }

    public EmptyDecompressor(int cacheSize) {
        super(cacheSize);
    }

    public EmptyDecompressor(VolumeByteStream cache) {
        super(cache);
    }

    @Override
    public int getDecompressBound(byte[] src, int offset, int length) {
        return decompressBound(src, offset, length);
    }

    @Override
    public final int decompress(byte[] src, int srcOffset, int srcLength, byte[] dst, int dstOffset, int dstLength) throws IOException {
        System.arraycopy(src, srcOffset, dst, dstOffset, dstLength);
        return dstLength;
    }

    @Override
    public void close() {
    }

    public static int decompressBound(byte[] src, int offset, int length) {
        return length;
    }

    public static VolumeByteStream decompress(byte[] src, int offset, int length) throws IOException {
        EmptyDecompressor decompressor = new EmptyDecompressor();
        decompressor.decompress(src, offset, length, decompressor.cache);
        decompressor.close();
        return decompressor.getCache();
    }
}
