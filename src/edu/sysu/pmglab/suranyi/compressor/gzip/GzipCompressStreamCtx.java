package edu.sysu.pmglab.suranyi.compressor.gzip;

import edu.sysu.pmglab.suranyi.container.VolumeByteOutputStream;

import java.io.IOException;

/**
 * @author suranyi
 * @description 转换接口 Gzip 解压缩上下文
 */

class GzipCompressStreamCtx {
    final VolumeByteOutputStream wrapper;
    final int compressionLevel;

    /**
     * 构造器方法，Gzip 仅支持压缩级别在 0-9 之间
     * @param compressionLevel 压缩级别
     */
    GzipCompressStreamCtx(int compressionLevel) {
        this.compressionLevel = compressionLevel;
        this.wrapper = new VolumeByteOutputStream();
    }

    /**
     * 压缩方法
     * @param src 原数据
     * @param srcOffset 源数据偏移量
     * @param srcLength 源数据有效长度
     * @param dst 目标数据容器
     * @param dstOffset 目标数据容器偏移量
     * @param dstLength 目标容器可用长度
     * @return 实际写入长度
     * @throws IOException IO 异常
     */
    int compress(byte[] src, int srcOffset, int srcLength, byte[] dst, int dstOffset, int dstLength) throws IOException {
        // 将 dst 包装为 outputStream 对象
        this.wrapper.wrap(dst, dstOffset);
        GzipOutputStream outputStream = new GzipOutputStream(this.wrapper, this.compressionLevel);
        outputStream.write(src, srcOffset, srcLength);
        outputStream.finish();
        try {
            return this.wrapper.size() - dstOffset;
        } finally {
            outputStream.close();
        }
    }
}
