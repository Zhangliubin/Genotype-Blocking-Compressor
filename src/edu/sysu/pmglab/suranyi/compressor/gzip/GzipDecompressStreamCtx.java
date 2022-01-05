package edu.sysu.pmglab.suranyi.compressor.gzip;

import edu.sysu.pmglab.suranyi.container.VolumeByteInputStream;

import java.io.IOException;
import java.util.zip.GZIPInputStream;

/**
 * @author suranyi
 * @description 转换接口 Gzip 解压缩上下文
 */

class GzipDecompressStreamCtx {
    final VolumeByteInputStream wrapper;

    GzipDecompressStreamCtx() {
        this.wrapper = new VolumeByteInputStream();
    }

    /**
     * 解压缩方法
     * @param src 原数据
     * @param srcOffset 源数据偏移量
     * @param srcLength 源数据有效长度
     * @param dst 目标数据容器
     * @param dstOffset 目标数据容器偏移量
     * @param dstLength 目标容器可用长度
     * @return 实际写入长度
     * @throws IOException IO 异常
     */
    int decompress(byte[] src, int srcOffset, int srcLength, byte[] dst, int dstOffset, int dstLength) throws IOException {
        // 将 dst 包装为 outputStream 对象
        this.wrapper.wrap(src, srcOffset);
        int totalLength = dstLength;

        try (GZIPInputStream inputStream = new GZIPInputStream(this.wrapper)) {
            int length = inputStream.read(dst, dstOffset, dstLength);
            while ((length != 0) && (length != -1)) {
                dstOffset += length;
                dstLength -= length;
                length = inputStream.read(dst, dstOffset, dstLength);
            }
            return totalLength;
        }
    }
}
