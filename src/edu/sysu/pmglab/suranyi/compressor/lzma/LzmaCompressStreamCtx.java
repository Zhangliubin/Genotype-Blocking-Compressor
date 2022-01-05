package edu.sysu.pmglab.suranyi.compressor.lzma;

import edu.sysu.pmglab.suranyi.container.VolumeByteOutputStream;
import org.tukaani.xz.LZMA2Options;
import org.tukaani.xz.LZMAOutputStream;
import org.tukaani.xz.UnsupportedOptionsException;

import java.io.IOException;

/**
 * @author suranyi
 * @description 转换接口 LZMA 解压缩上下文
 */

class LzmaCompressStreamCtx extends ILzmaCompressCtx{
    final VolumeByteOutputStream wrapper;
    final LZMA2Options options;

    /**
     * 构造器方法，LZMA 仅支持压缩级别在 0-9 之间
     * @param compressionLevel 压缩级别
     */
    LzmaCompressStreamCtx(int compressionLevel) throws UnsupportedOptionsException {
        options = new LZMA2Options(compressionLevel);
        this.wrapper = new VolumeByteOutputStream();
    }

    @Override
    int compress(byte[] src, int srcOffset, int srcLength, byte[] dst, int dstOffset, int dstLength) throws IOException {
        // 将 dst 包装为 outputStream 对象
        this.wrapper.wrap(dst, dstOffset);
        LZMAOutputStream outputStream = new LZMAOutputStream(this.wrapper, this.options, srcLength);
        outputStream.write(src, srcOffset, srcLength);
        outputStream.finish();
        try {
            return this.wrapper.size() - dstOffset;
        } finally {
            outputStream.close();
        }
    }

    @Override
    void close() {

    }
}
