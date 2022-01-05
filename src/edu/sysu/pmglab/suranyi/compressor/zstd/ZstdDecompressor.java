package edu.sysu.pmglab.suranyi.compressor.zstd;

import com.github.luben.zstd.Zstd;
import com.github.luben.zstd.ZstdDecompressCtx;
import edu.sysu.pmglab.suranyi.compressor.IDecompressor;
import edu.sysu.pmglab.suranyi.container.VolumeByteStream;
import edu.sysu.pmglab.suranyi.check.Assert;

import java.io.IOException;

/**
 * @author suranyi
 * @description ZSTD 解压器，使用 https://github.com/luben/zstd-jni 下载的包作为前置
 */

public final class ZstdDecompressor extends IDecompressor {
    public static final String COMPRESSOR_NAME = "ZSTD";

    /**
     * 下载 zstd-jni，将 src/main/java 设置为 resource，并构建 jar 包
     * 本地链接库在 https://repo1.maven.org/maven2/com/github/luben/zstd-jni/ 下载
     * zstd api 库 https://facebook.github.io/zstd/zstd_manual.html
     */
    final ZstdDecompressCtx decompressor = new ZstdDecompressCtx();

    public ZstdDecompressor() {
        super();
    }

    public ZstdDecompressor(int cacheSize) {
        super(cacheSize);
    }

    public ZstdDecompressor(VolumeByteStream cache) {
        super(cache);
    }

    @Override
    public int getDecompressBound(byte[] src, int offset, int length) {
        return decompressBound(src, offset, length);
    }

    @Override
    public final int decompress(byte[] src, int srcOffset, int srcLength, byte[] dst, int dstOffset, int dstLength) {
        return this.decompressor.decompressByteArray(dst, dstOffset, dstLength, src, srcOffset, srcLength);
    }

    @Override
    public void close() {
        this.decompressor.close();
    }

    public static int decompressBound(byte[] src, int offset, int length) {
        long size = Zstd.decompressedSize(src, offset, length);

        Assert.valueRange(size, 0, Integer.MAX_VALUE - 2);
        return (int) size;
    }

    public static VolumeByteStream decompress(byte[] src, int offset, int length) throws IOException {
        ZstdDecompressor decompressor = new ZstdDecompressor();
        decompressor.decompress(src, offset, length, decompressor.cache);
        decompressor.close();
        return decompressor.getCache();
    }
}
