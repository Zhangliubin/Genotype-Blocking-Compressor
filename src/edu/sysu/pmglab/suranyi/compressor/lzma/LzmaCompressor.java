package edu.sysu.pmglab.suranyi.compressor.lzma;

import edu.sysu.pmglab.suranyi.check.Assert;
import edu.sysu.pmglab.suranyi.compressor.ICompressor;
import edu.sysu.pmglab.suranyi.container.VolumeByteStream;
import org.tukaani.xz.UnsupportedOptionsException;

import java.io.IOException;

/**
 * @author suranyi
 * @description LZMA 压缩器，基于 LZMA-SDK 进行二次开发
 */

public class LzmaCompressor extends ICompressor {
    public static final int MIN_LEVEL = 0;
    public static final int MAX_LEVEL = 9;
    public static final int DEFAULT_LEVEL = 3;
    public static final String COMPRESSOR_NAME = "LZMA";

    final ILzmaCompressCtx compressor;

    public LzmaCompressor() throws UnsupportedOptionsException {
        super();
        this.compressor = new LzmaCompressCtx(DEFAULT_LEVEL);
    }

    public LzmaCompressor(int compressionLevel) throws UnsupportedOptionsException {
        super(compressionLevel);
        this.compressor = new LzmaCompressCtx(compressionLevel);
    }

    public LzmaCompressor(int compressionLevel, int cacheSize) throws UnsupportedOptionsException {
        super(compressionLevel, cacheSize);
        this.compressor = new LzmaCompressCtx(compressionLevel);
    }

    public LzmaCompressor(int compressionLevel, final VolumeByteStream cache) {
        super(compressionLevel, cache);
        this.compressor = new LzmaCompressCtx(compressionLevel);
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
        return 0;
    }

    @Override
    public int getMaxCompressionLevel() {
        return MAX_LEVEL;
    }

    @Override
    public int compress(byte[] src, int srcOffset, int srcLength, byte[] dst, int dstOffset, int dstLength) throws IOException {
        return this.compressor.compress(src, srcOffset, srcLength, dst, dstOffset, -1);
    }

    @Override
    public void close() {
        this.compressor.close();
    }

    public static int compressBound(int length) {
        Assert.valueRange(length, 0, 2112209743);
        return (int) Math.max(7680, 1.0167 * length);
    }

    public static VolumeByteStream compress(int compressionLevel, byte[] src, int offset, int length) throws IOException {
        LzmaCompressor compressor = new LzmaCompressor(compressionLevel);
        compressor.compress(src, offset, length);
        compressor.close();
        return compressor.getCache();
    }
}