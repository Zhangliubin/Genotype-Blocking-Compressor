package edu.sysu.pmglab.suranyi.compressor.empty;

import edu.sysu.pmglab.suranyi.compressor.ICompressor;
import edu.sysu.pmglab.suranyi.container.VolumeByteStream;
import edu.sysu.pmglab.suranyi.check.Assert;

import java.io.IOException;

/**
 * @author suranyi
 * @description 空压缩器
 */

public class EmptyCompressor extends ICompressor {
    public static final int MIN_LEVEL = 0;
    public static final int MAX_LEVEL = 31;
    public static final int DEFAULT_LEVEL = 0;
    public static final String COMPRESSOR_NAME = "EMPTY";

    public EmptyCompressor() {
        super();
    }

    public EmptyCompressor(int compressionLevel) {
        super(compressionLevel);
    }

    public EmptyCompressor(int compressionLevel, int cacheSize) {
        super(compressionLevel, cacheSize);
    }

    public EmptyCompressor(int compressionLevel, final VolumeByteStream cache) {
        super(compressionLevel, cache);
    }

    @Override
    public int getCompressBound(int length) {
        return compressBound(length);
    }

    @Override
    public int getMinCompressionLevel() {
        return MIN_LEVEL;
    }

    @Override
    public int getDefaultCompressionLevel() {
        return DEFAULT_LEVEL;
    }

    @Override
    public int getMaxCompressionLevel() {
        return MAX_LEVEL;
    }

    @Override
    public int compress(byte[] src, int srcOffset, int srcLength, byte[] dst, int dstOffset, int dstLength) throws IOException {
        System.arraycopy(src, srcOffset, dst, dstOffset, srcLength);
        return srcLength;
    }

    @Override
    public void close() {
    }

    public static int compressBound(int length) {
        Assert.valueRange(length, 0, 2147483642);
        return length;
    }

    public static VolumeByteStream compress(int compressionLevel, byte[] src, int offset, int length) throws IOException {
        EmptyCompressor compressor = new EmptyCompressor();
        compressor.compress(src, offset, length);
        compressor.close();
        return compressor.getCache();
    }
}