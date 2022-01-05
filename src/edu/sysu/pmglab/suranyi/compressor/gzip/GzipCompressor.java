package edu.sysu.pmglab.suranyi.compressor.gzip;

import edu.sysu.pmglab.suranyi.check.Assert;
import edu.sysu.pmglab.suranyi.compressor.ICompressor;
import edu.sysu.pmglab.suranyi.container.VolumeByteStream;

import java.io.IOException;

/**
 * @author suranyi
 * @description gzip 压缩器
 */

public class GzipCompressor extends ICompressor {
    public static final int MIN_LEVEL = 0;
    public static final int MAX_LEVEL = 9;
    public static final int DEFAULT_LEVEL = 5;
    public static final String COMPRESSOR_NAME = "GZIP";

    final GzipCompressStreamCtx compressor;

    public GzipCompressor() {
        super();
        this.compressor = new GzipCompressStreamCtx(DEFAULT_LEVEL);
    }

    public GzipCompressor(int compressionLevel) {
        super(compressionLevel);
        this.compressor = new GzipCompressStreamCtx(compressionLevel);
    }

    public GzipCompressor(int compressionLevel, int cacheSize) {
        super(compressionLevel, cacheSize);
        this.compressor = new GzipCompressStreamCtx(compressionLevel);
    }

    public GzipCompressor(int compressionLevel, final VolumeByteStream cache) {
        super(compressionLevel, cache);
        this.compressor = new GzipCompressStreamCtx(compressionLevel);
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
        return this.compressor.compress(src, srcOffset, srcLength, dst, dstOffset, dstLength);
    }

    @Override
    public void close() {
    }

    public static int compressBound(int length) {
        Assert.valueRange(length, 0, 2140847020);
        return (int) Math.max(7680, 1.003100 * length);
    }

    public static VolumeByteStream compress(int compressionLevel, byte[] src, int offset, int length) throws IOException {
        GzipCompressor compressor = new GzipCompressor(compressionLevel);
        compressor.compress(src, offset, length);
        compressor.close();
        return compressor.getCache();
    }
}