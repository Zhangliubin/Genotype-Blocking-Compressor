package edu.sysu.pmglab.suranyi.compressor.zstd;

import com.github.luben.zstd.Zstd;
import com.github.luben.zstd.ZstdCompressCtx;
import edu.sysu.pmglab.suranyi.compressor.ICompressor;
import edu.sysu.pmglab.suranyi.container.VolumeByteStream;
import edu.sysu.pmglab.suranyi.check.Assert;

import java.io.IOException;

/**
 * @author suranyi
 * @description ZSTD 压缩器，使用 https://github.com/luben/zstd-jni 下载的包作为前置
 */

public final class ZstdCompressor extends ICompressor {
    public static final int MIN_LEVEL = 0;
    public static final int MAX_LEVEL = 22;
    public static final int DEFAULT_LEVEL = 16;
    public static final String COMPRESSOR_NAME = "ZSTD";

    /**
     * 下载 zstd-jni，将 src/main/java 设置为 resource，并构建 jar 包
     * 本地链接库在 https://repo1.maven.org/maven2/com/github/luben/zstd-jni/ 下载
     * zstd api 库 https://facebook.github.io/zstd/zstd_manual.html
     */
    final ZstdCompressCtx compressor = new ZstdCompressCtx();

    public ZstdCompressor() {
        super();
        this.compressor.setLevel(DEFAULT_LEVEL);
    }


    public ZstdCompressor(int compressionLevel) {
        super(compressionLevel);
        this.compressor.setLevel(compressionLevel);
    }

    public ZstdCompressor(int compressionLevel, int cacheSize) {
        super(compressionLevel, cacheSize);
        this.compressor.setLevel(compressionLevel);
    }


    public ZstdCompressor(int compressionLevel, final VolumeByteStream cache) {
        super(compressionLevel, cache);
        this.compressor.setLevel(compressionLevel);
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
    public int compress(byte[] src, int srcOffset, int srcLength, byte[] dst, int dstOffset, int dstLength) {
        return this.compressor.compressByteArray(dst, dstOffset, dstLength, src, srcOffset, srcLength);
    }

    @Override
    public final void close() {
        this.compressor.close();
    }

    public static int compressBound(int length) {
        Assert.valueRange(length, 0, 2139127678);
        return (int) Zstd.compressBound(length);
    }

    public static VolumeByteStream compress(int compressionLevel, byte[] src, int offset, int length) throws IOException {
        ZstdCompressor compressor = new ZstdCompressor(compressionLevel);
        compressor.compress(src, offset, length);
        compressor.close();
        return compressor.getCache();
    }
}