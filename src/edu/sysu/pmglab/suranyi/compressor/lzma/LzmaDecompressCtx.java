package edu.sysu.pmglab.suranyi.compressor.lzma;

import edu.sysu.pmglab.suranyi.container.VolumeByteInputStream;
import org.tukaani.xz.ArrayCache;
import org.tukaani.xz.CorruptedInputException;
import org.tukaani.xz.UnsupportedOptionsException;
import org.tukaani.xz.lz.LZDecoder;
import org.tukaani.xz.lzma.LZMADecoder;
import org.tukaani.xz.rangecoder.RangeDecoderFromStream;

import java.io.IOException;

/**
 * @author suranyi
 * @description LZMA 解压缩上下文，复用数据区
 */

class LzmaDecompressCtx extends ILzmaDecompressCtx {
    public static final int DICT_SIZE_MAX = Integer.MAX_VALUE & ~15;
    final VolumeByteInputStream wrapper;
    RangeDecoderFromStream rc;
    LZDecoder lz;
    LZMADecoder lzma;

    /**
     * 解压器信息，非必要不重制
     */
    int props = -1;
    int dictSize = -1;
    int pb;
    int lp;
    int lc;

    /**
     * 构造器方法，LZMA 仅支持压缩级别在 0-9 之间
     */
    LzmaDecompressCtx() {
        this.wrapper = new VolumeByteInputStream();
    }

    @Override
    int decompress(byte[] src, int srcOffset, int srcLength, byte[] dst, int dstOffset, int dstLength) throws IOException {
        boolean lzmarebuild = false;
        boolean lzrebuild = false;

        // 将 src 包装为 outputStream 对象
        this.wrapper.wrap(src, srcOffset);

        // 读取字节
        int currentProps = this.wrapper.read();
        if (this.props != currentProps) {
            this.props = currentProps;

            // 解码属性字节
            if (currentProps > (4 * 5 + 4) * 9 + 8) {
                throw new CorruptedInputException("Invalid LZMA properties byte");
            }
            this.pb = currentProps / (9 * 5);
            currentProps -= this.pb * 9 * 5;
            this.lp = currentProps / 9;
            this.lc = currentProps - this.lp * 9;

            // 验证信息
            if (lc < 0 || lc > 8 || lp < 0 || lp > 4) {
                throw new IllegalArgumentException();
            }

            lzmarebuild = true;
        }

        // 获取字典大小
        int currentDictSize = 0;
        for (int i = 0; i < 4; ++i) {
            currentDictSize |= this.wrapper.read() << (8 * i);
        }

        if (currentDictSize < 0 || currentDictSize > DICT_SIZE_MAX) {
            throw new UnsupportedOptionsException("LZMA dictionary is too big for this implementation");
        }

        // 获取原数据大小，实际上 uncompSize 最大是 2GB 大小
        long uncompSize = 0;
        for (int i = 0; i < 8; ++i) {
            uncompSize |= (long) this.wrapper.read() << (8 * i);
        }

        // 未解压数据的大小为 0，直接返回
        if (uncompSize == 0) {
            return 0;
        }

        if (uncompSize > Integer.MAX_VALUE - 2) {
            throw new UnsupportedOptionsException("Src is too big (over 2GB)");
        }

        currentDictSize = getDictSize((int) Math.min(currentDictSize, uncompSize));
        // 校验字典大小，并决定是否需要重置解码器
        if ((this.dictSize != currentDictSize)) {
            this.dictSize = currentDictSize;

            lzrebuild = true;
        }

        // 校验数据容器
        dstLength = dst.length - dstOffset;
        if ((uncompSize < -1) || (dstLength < uncompSize)) {
            throw new UnsupportedOptionsException("Uncompressed size is too big");
        }

        // rc 重包装
        if (this.rc == null) {
            // 初始化阶段
            this.rc = new RangeDecoderFromStream(this.wrapper);
            this.lz = new LZDecoder(getDictSize(currentDictSize), null, ArrayCache.getDefaultCache());
            this.lzma = new LZMADecoder(lz, rc, lc, lp, pb);
        } else {
            if (lzrebuild) {
                this.lz.reset(getDictSize(currentDictSize));
            } else {
                this.lz.reset();
            }

            this.rc.reWrap();

            if (lzmarebuild) {
                this.lzma = new LZMADecoder(lz, rc, lc, lp, pb);
            } else {
                this.lzma.reset();
            }
        }

        // 解压数据长度
        int size = 0;

        while (dstLength > 0) {
            int copySizeMax = (int) Math.min(dstLength, uncompSize);
            lz.setLimit(copySizeMax);

            // Decode into the dictionary buffer.
            try {
                lzma.decode();
            } catch (CorruptedInputException e) {
                if (uncompSize != -1 || !lzma.endMarkerDetected()) {
                    throw e;
                }

                rc.normalize();
            }

            // Copy from the dictionary to buf.
            int copiedSize = lz.flush(dst, dstOffset);
            dstOffset += copiedSize;
            dstLength -= copiedSize;
            size += copiedSize;

            if (uncompSize >= 0) {
                // Update the number of bytes left to be decompressed.
                uncompSize -= copiedSize;

                if (uncompSize == 0) {
                    if (lz.hasPending() || !rc.isFinished()) {
                        throw new CorruptedInputException();
                    }
                    return size == 0 ? -1 : size;
                }
            }
        }

        return size;
    }

    private static int getDictSize(int dictSize) {
        if (dictSize < 0 || dictSize > DICT_SIZE_MAX) {
            throw new IllegalArgumentException(
                    "LZMA dictionary is too big for this implementation");
        }

        return (Math.max(dictSize, 4096) + 15) & ~15;
    }

    @Override
    void close() {
        rc = null;
        lz = null;
        lzma = null;
    }
}