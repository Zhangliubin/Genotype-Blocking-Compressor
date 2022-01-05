package edu.sysu.pmglab.suranyi.compressor.lzma;

import edu.sysu.pmglab.suranyi.container.VolumeByteInputStream;
import org.tukaani.xz.LZMAInputStream;

import java.io.IOException;

/**
 * @author suranyi
 * @description 转换接口 LZMA 解压缩上下文
 */

public class LzmaDecompressStreamCtx extends ILzmaDecompressCtx {
    final VolumeByteInputStream wrapper;

    LzmaDecompressStreamCtx() {
        this.wrapper = new VolumeByteInputStream();
    }

    @Override
    int decompress(byte[] src, int srcOffset, int srcLength, byte[] dst, int dstOffset, int dstLength) throws IOException {
        // 将 dst 包装为 outputStream 对象
        this.wrapper.wrap(src, srcOffset);

        try (LZMAInputStream inputStream = new LZMAInputStream(this.wrapper)) {
            return inputStream.read(dst, dstOffset, dst.length);
        }
    }

    @Override
    void close() {

    }
}
