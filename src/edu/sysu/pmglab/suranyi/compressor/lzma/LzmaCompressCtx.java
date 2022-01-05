package edu.sysu.pmglab.suranyi.compressor.lzma;

import edu.sysu.pmglab.suranyi.container.VolumeByteOutputStream;
import org.tukaani.xz.ArrayCache;
import org.tukaani.xz.LZMA2Options;
import org.tukaani.xz.UnsupportedOptionsException;
import org.tukaani.xz.lz.LZEncoder;
import org.tukaani.xz.lzma.LZMAEncoder;
import org.tukaani.xz.rangecoder.RangeEncoderToStream;

import java.io.IOException;

/**
 * @author suranyi
 * @description LZMA 压缩上下文，复用数据区
 */

class LzmaCompressCtx extends ILzmaCompressCtx {
    final VolumeByteOutputStream wrapper;
    final RangeEncoderToStream rc;
    final int props;
    final byte[] dictSizeByteArray;
    final LZEncoder lz;
    final LZMAEncoder lzma;

    /**
     * 构造器方法，LZMA 仅支持压缩级别在 0-9 之间
     * @param compressionLevel 压缩级别
     */
    LzmaCompressCtx(int compressionLevel) {
        LZMA2Options options;
        try {
            options = new LZMA2Options(compressionLevel);
        } catch (UnsupportedOptionsException e) {
            throw new IllegalArgumentException(e.getMessage());
        }

        this.wrapper = new VolumeByteOutputStream();
        this.rc = new RangeEncoderToStream(this.wrapper);
        this.props = (options.getPb() * 5 + options.getLp()) * 9 + options.getLc();
        this.dictSizeByteArray = new byte[4];
        int dictSize = options.getDictSize();

        for (int i = 0; i < 4; ++i) {
            this.dictSizeByteArray[i] = (byte) (dictSize & 0xFF);
            dictSize >>>= 8;
        }

        this.lzma = LZMAEncoder.getInstance(rc, options, ArrayCache.getDefaultCache());
        this.lz = this.lzma.getLZEncoder();
    }

    @Override
    int compress(byte[] src, int srcOffset, int srcLength, byte[] dst, int dstOffset, int dstLength) throws IOException {
        // 将 dst 包装为 outputStream 对象
        this.wrapper.wrap(dst, dstOffset);
        this.wrapper.write(this.props);
        this.wrapper.write(this.dictSizeByteArray);

        // 64 bit
        this.wrapper.write(srcLength & 0xFF);
        this.wrapper.write((srcLength >> 8) & 0xFF);
        this.wrapper.write((srcLength >> 16) & 0xFF);
        this.wrapper.write((srcLength >> 24) & 0xFF);
        this.wrapper.write(0);
        this.wrapper.write(0);
        this.wrapper.write(0);
        this.wrapper.write(0);

        // 压缩数据
        while (srcLength > 0) {
            int used = this.lz.fillWindow(src, srcOffset, srcLength);
            srcOffset += used;
            srcLength -= used;
            lzma.encodeForLZMA1();
        }

        // 关闭流
        this.lz.setFinishing();
        this.lzma.encodeForLZMA1();
        this.rc.finish();
        this.rc.reset();
        this.lzma.reset();

        return this.wrapper.size() - dstOffset;
    }

    @Override
    void close() {

    }
}
